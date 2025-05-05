package com.github.biltudas1.swiftserve;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Random;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.biltudas1.swiftserve.blockchain.Block;
import com.github.biltudas1.swiftserve.blockchain.Blockchain;
import com.github.biltudas1.swiftserve.blockchain.Key;
import com.github.biltudas1.swiftserve.blockchain.Node;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@SpringBootApplication
@RestController
public class SwiftserveApplication {
	private static Key key;
	private static Blockchain chain;
	private static NodeList nodes = new NodeList();
	private static FileList files = new FileList();
	private static String currentNodeIP;

	/**
	 * Generates/Loads Private and Public key of this machine
	 * 
	 * @param filepath The path where the key file will be stored for future use
	 * @return Key object containing the Private and Public key
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	private final static Key getKey(String filepath) throws NoSuchAlgorithmException, IOException {
		File kp = new File(filepath);
		Key key;

		if (kp.exists() && kp.isFile()) {
			key = new Key();
			key.loadKey(filepath);
		} else {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
			KeyPair keypair = kpg.generateKeyPair();
			key = new Key(keypair);
			key.saveKey(filepath);
		}

		return key;
	}

	/**
	 * Picks a random Item from an array
	 * 
	 * @param <T>   Can Accept any kind of Object
	 * @param array The ArrayList containing the objects
	 * @return Object which is randomly picked
	 */
	public final static <T> T getRandom(ArrayList<T> array) {
		int rnd = new Random().nextInt(array.size());
		return array.get(rnd);
	}

	public static void main(String[] args) throws NoSuchAlgorithmException, IOException, InvalidKeyException,
			SignatureException {
		SwiftserveApplication.key = SwiftserveApplication.getKey("localkey.pem");
		SwiftserveApplication.currentNodeIP = "127.0.0.1";
		Block genesis = new Block(0, "0", "add_node", new Node(""), SwiftserveApplication.currentNodeIP,
				SwiftserveApplication.key.getPrivateKeyRaw());
		SwiftserveApplication.chain = new Blockchain(genesis); // Added genesis block to the blockchain
		SpringApplication.run(SwiftserveApplication.class, args);
	}

	@GetMapping("/")
	public void root() {
	}

	@PostMapping("/addBlock")
	public boolean addBlock(@RequestBody byte[] block)
			throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, JsonProcessingException, IOException,
			ClassNotFoundException, InterruptedException {
		Block newBlock = new Block(block);
		try {
			SwiftserveApplication.chain.add(newBlock);

		} catch (InvalidParameterException e) {
			if (e.getMessage().equals("new block previousBlockHash is different from the top of the block hash") || e
					.getMessage().contains("blockNumber can only be")
					|| e.getMessage().equals("new block can't be created before the top of the block")) {
				ArrayList<String> mostCommonHashNodes = NodeList.mostMatchedHashNodes(
						SwiftserveApplication.nodes.randomPicks((int) Math.sqrt(nodes.size())), 8080,
						SwiftserveApplication.chain.lastBlockNumber());

				// If the current node have the most common hash
				if (mostCommonHashNodes.contains(SwiftserveApplication.currentNodeIP)) {
					return false;
				}

				// Picking random node and copy the blockchain data
				int i = 0;
				for (; i < 5; i++) {
					String pickedNode = SwiftserveApplication.getRandom(mostCommonHashNodes);
					try {
						long collidedBlockNumber = SwiftserveApplication.chain.collidedBlock(pickedNode, 8080);
						byte[] data = NodeList.getBlocksData(pickedNode, 8080, collidedBlockNumber);
						SwiftserveApplication.chain.loadBlocksData(data, collidedBlockNumber);
					} catch (Exception e1) {
						e1.printStackTrace();
						continue;
					}
					break;
				}

				// If no nodes found to copy the blockchain, then avoid the transaction
				if (i >= 5) {
					return false;
				}
				// Now adding the new block to the chain
				SwiftserveApplication.chain.add(newBlock);
			} else if (e.getMessage().equals("block signature verification failed: signature not matched")) {
				// Reject the block from adding to the blockchain
				return false;
			}
		}

		String actionType = newBlock.toRecord().actionType();
		if (actionType.equals("add_node")) {
			SwiftserveApplication.nodes.add(((Node) newBlock.toRecord().actionData()).nodeIP());
		} else if (actionType.equals("remove_node")) {
			SwiftserveApplication.nodes.remove(((Node) newBlock.toRecord().actionData()).nodeIP());
		} else if (actionType.equals("add_file")) {
			String filename = ((com.github.biltudas1.swiftserve.blockchain.File) newBlock.toRecord().actionData()).filename();
			String filehash = ((com.github.biltudas1.swiftserve.blockchain.File) newBlock.toRecord().actionData()).filehash();
			SwiftserveApplication.files.add(filename, filehash, newBlock.toRecord().creatorIP());
		} else if (actionType.equals("remove_file")) {
			String filename = ((com.github.biltudas1.swiftserve.blockchain.File) newBlock.toRecord().actionData()).filename();
			SwiftserveApplication.files.remove(filename);
		}

		// Telling nearest random 4 nodes about the new block (max)
		String[] nodes;
		if (SwiftserveApplication.nodes.size() > 4) {
			nodes = SwiftserveApplication.nodes.randomPicks(4);
		} else {
			nodes = SwiftserveApplication.nodes.randomPicks(SwiftserveApplication.nodes.size());
		}

		// Sending the block to othe r nodes
		for (String nodeIP : nodes) {
			SendBlock send = new SendBlock(nodeIP, 8080, newBlock);
			Thread.startVirtualThread(send);
		}

		return true;
	}

	@GetMapping(value = "/getHash", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getBlockHash(@RequestParam long num) {
		try {
			return chain.getBlockHash(num);
		} catch (IndexOutOfBoundsException e) {
			return "";
		}
	}

	@GetMapping(value = "/topBlockNumber", produces = MediaType.TEXT_PLAIN_VALUE)
	public long getTopBlockNumber(@RequestParam(defaultValue = "") String param) {
		return SwiftserveApplication.chain.lastBlockNumber();
	}

	@GetMapping(value = "/totalBlocks", produces = MediaType.TEXT_PLAIN_VALUE)
	public long getTotalBlocksCount(@RequestParam(defaultValue = "") String param) {
		return SwiftserveApplication.chain.size();
	}

	@PostMapping(value = "/getBlockDatas", produces = MediaType.TEXT_PLAIN_VALUE)
	public byte[] getBlockDatas(@RequestBody long blockNum) throws IOException {
		return SwiftserveApplication.chain.getBlocksData(blockNum);
	}

	@GetMapping(value = "/key.pem", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getPublicKeyOfNode()
			throws NoSuchAlgorithmException, IOException {
		return "-----BEGIN PUBLIC KEY-----\n" + SwiftserveApplication.key.getPublicKey() + "\n-----END PUBLIC KEY-----";
	}

}
