package com.github.biltudas1.swiftserve;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

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

@SpringBootApplication
@RestController
public class SwiftserveApplication {
	private static Key key;
	private static Blockchain chain;

	/**
	 * Generates/Loads Private and Public key of this machine
	 * 
	 * @return Key object containing the Private and Public key
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	private static Key getKey() throws NoSuchAlgorithmException, IOException {
		File kp = new File("localKey.pem");
		Key key;

		if (kp.exists() && kp.isFile()) {
			key = new Key();
			key.loadKey("localKey.pem");
		} else {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
			KeyPair keypair = kpg.generateKeyPair();
			key = new Key(keypair);
			key.saveKey("localKey.pem");
		}

		return key;
	}

	public static void main(String[] args) throws NoSuchAlgorithmException, IOException, InvalidKeyException,
			SignatureException {
		SwiftserveApplication.key = SwiftserveApplication.getKey();
		Block genesis = new Block(0, "0", "add_node", new Node(""), "127.0.0.1",
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
		SwiftserveApplication.chain.add(newBlock);
		return true;
	}

	@GetMapping(value = "/key.pem", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getPublicKeyOfNode()
			throws NoSuchAlgorithmException, IOException {
		return "-----BEGIN PUBLIC KEY-----\n" + SwiftserveApplication.key.getPublicKey() + "\n-----END PUBLIC KEY-----";
	}

}
