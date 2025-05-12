package com.github.biltudas1.swiftserve;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Random;

import org.springframework.core.io.Resource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.biltudas1.swiftserve.blockchain.Block;
import com.github.biltudas1.swiftserve.blockchain.Blockchain;
import com.github.biltudas1.swiftserve.blockchain.Key;
import com.github.biltudas1.swiftserve.blockchain.Node;

@SpringBootApplication
@RestController
public class SwiftserveApplication {
	private static Key key;
	private static Blockchain chain;
	private static NodeList nodes = new NodeList();
	private static FileList files = new FileList();
	private static String currentNodeIP;
	private static String savePath;

	public static int remainingPeersToKnowAboutChunk = 4;

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
		SwiftserveApplication.savePath = System.getProperty("user.dir") + "/downloads";
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
			long filesize = ((com.github.biltudas1.swiftserve.blockchain.File) newBlock.toRecord().actionData()).filesize();
			SwiftserveApplication.files.add(filehash, filename, newBlock.toRecord().creatorIP(), filesize);
		} else if (actionType.equals("remove_file")) {
			String filehash = ((com.github.biltudas1.swiftserve.blockchain.File) newBlock.toRecord().actionData()).filehash();
			SwiftserveApplication.files.remove(filehash);
		}

		// Telling nearest random 4 nodes about the new block (max)
		String[] nodes;
		if (SwiftserveApplication.nodes.size() > 4) {
			nodes = SwiftserveApplication.nodes.randomPicks(4);
		} else {
			nodes = SwiftserveApplication.nodes.randomPicks(SwiftserveApplication.nodes.size());
		}

		// Sending the block to other nodes
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

	@PostMapping(value = "/tellAboutChunk", produces = MediaType.TEXT_PLAIN_VALUE)
	public boolean startDownloadChunk(@RequestBody byte[] chunkData)
			throws IOException, InterruptedException, NoSuchAlgorithmException {
		ChunkInfo chunk = ChunkInfo.fromBytes(chunkData);

		// Check if the chunk is already downloaded, if yes then skip
		if (SwiftserveApplication.files.isFileExist(chunk.filehash())
				&& SwiftserveApplication.files.getFileInfo(chunk.filehash()).isChunkHashExists(chunk.sha1())) {
			return true;
		}

		// Trying to download the file, if failed then try max 3 times
		int retry = 0;
		for (; retry < 3; retry++) {
			FileList.downloadChunk(chunk.nodeIP(), chunk.port(), chunk.filehash(), chunk.chunkNumber(),
					SwiftserveApplication.savePath); // Downloading and saving the chunk

			// Verifying the Chunk
			if (FileList.verifyChunk(SwiftserveApplication.savePath, chunk.filehash(), chunk.chunkNumber(), chunk.sha1())) {
				break;
			}
		}

		// Try max limit excedded
		if (retry == 3) {
			File ff = new File(
					SwiftserveApplication.savePath + "/chunk/" + chunk.filehash() + "/" + chunk.chunkNumber() + ".part");
			if (ff.exists() && ff.isFile()) {
				ff.delete();
			}
			return false;
		}

		// File downloaded successfully
		if (!SwiftserveApplication.files.isFileExist(chunk.filehash())) {
			return false;
		}
		SwiftserveApplication.files.getFileInfo(chunk.filehash()).addChunkHash(chunk.sha1());

		// If all chunks are downloaded, if yes then start combining them
		long totalFiles = FileList.totalDownloadedChunks(chunk.filehash(), SwiftserveApplication.savePath);
		if (totalFiles == chunk.totalChunks()) {
			// Combining the chunks
			FileList.combineFiles(SwiftserveApplication.files.getFileName(chunk.filehash()),
					SwiftserveApplication.savePath + "/chunks/" + chunk.filehash());
		}

		// Tell 4 random peers (max) that a new chunk has been downloaded
		int totalPeers = SwiftserveApplication.remainingPeersToKnowAboutChunk;
		while (SwiftserveApplication.remainingPeersToKnowAboutChunk != 0) {
			if (totalPeers == SwiftserveApplication.remainingPeersToKnowAboutChunk) {
				if (SwiftserveApplication.remainingPeersToKnowAboutChunk > SwiftserveApplication.nodes.size()) {
					SwiftserveApplication.remainingPeersToKnowAboutChunk = SwiftserveApplication.nodes.size();
				}
			}
			String[] nodes = SwiftserveApplication.nodes.randomPicks(SwiftserveApplication.remainingPeersToKnowAboutChunk);

			// Sending the block to other nodes
			for (String nodeIP : nodes) {
				TellNode cnk = new TellNode(nodeIP, 8080, chunk.chunkNumber(), chunk.totalChunks(), chunk.sha1(),
						SwiftserveApplication.currentNodeIP, 8080, chunk.filehash());
				Thread.startVirtualThread(cnk);
			}

			// Waiting for 1 Minute if some tasks are remaining, and again retry
			if (SwiftserveApplication.remainingPeersToKnowAboutChunk > 0) {
				Thread.sleep(60 * 1000);
			}
		}
		SwiftserveApplication.remainingPeersToKnowAboutChunk = 4; // Reset back to 4 peers

		return true;
	}

	@GetMapping(value = "/getChunk")
	public ResponseEntity<Resource> downloadChunk(@RequestParam String filehash, long number) {
		File file = new File(SwiftserveApplication.savePath + "/chunks/" + filehash + "/" + number);
		if (!file.exists() || !file.isFile()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		try {
			InputStream inputStream = new FileInputStream(file);
			InputStreamResource resource = new InputStreamResource(inputStream);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentDisposition(
					ContentDisposition.attachment().filename(filehash + "-" + file.getName() + ".part").build());

			return ResponseEntity.ok()
					.headers(headers)
					.contentLength(file.length())
					.contentType(MediaType.APPLICATION_OCTET_STREAM)
					.body(resource);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping(value = "/key.pem", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getPublicKeyOfNode()
			throws NoSuchAlgorithmException, IOException {
		return "-----BEGIN PUBLIC KEY-----\n" + SwiftserveApplication.key.getPublicKey() + "\n-----END PUBLIC KEY-----";
	}

}
