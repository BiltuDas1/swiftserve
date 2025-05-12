package com.github.biltudas1.swiftserve.blockchain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

public class BlockTest {
  Key generateKey() throws NoSuchAlgorithmException {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
    KeyPair keypair = kpg.generateKeyPair();
    Key key = new Key(keypair);
    return key;
  }

  @Test
  public void testCreationOfNormalBlock() throws NoSuchAlgorithmException, InvalidKeyException,
      SignatureException, JsonProcessingException, IOException {
    Key key = this.generateKey();
    long timeStart = System.currentTimeMillis() / 1000L;
    Block blk = new Block(0, "12f", "add_node", new Node("127.0.0.2"), "127.0.0.1", key.getPrivateKeyRaw());
    long timeEnd = System.currentTimeMillis() / 1000L;

    assertTrue(blk.toRecord().blockNumber() == 0);
    assertTrue(blk.toRecord().previousBlockHash() == "12f");
    assertTrue(blk.toRecord().actionType().equals("add_node"));
    assertTrue(((Node) blk.toRecord().actionData()).nodeIP().equals("127.0.0.2"));
    assertTrue(blk.toRecord().creatorIP().equals("127.0.0.1"));
    assertTrue(blk.toRecord().creationTime() >= timeStart && blk.toRecord().creationTime() <= timeEnd);
  }

  @Test
  public void testBytes() throws NoSuchAlgorithmException, InvalidKeyException,
      SignatureException, JsonProcessingException, IOException, ClassNotFoundException {
    Key key = this.generateKey();
    Block blk = new Block(0, "1", "add_node", new Node("127.0.0.2"), "127.0.0.1", key.getPrivateKeyRaw());
    int endBlock = 0;
    int start = 0;
    int end = 0;
    for (byte data : blk.toBytes()) {
      if (data == 2) {
        start += 1;
      } else if (data == 3) {
        end += 1;
      } else if (data == 23) {
        endBlock += 1;
      }
    }

    assertTrue(start == 3, "start should be 3; but it is " + start);
    assertTrue(end == 3, "end should be 3; but it is " + end);
    assertTrue(endBlock == 1, "endBlock should be 3; but it is " + endBlock);
  }

  @Test
  public void testSaveRestore() throws NoSuchAlgorithmException, InvalidKeyException,
      SignatureException, JsonProcessingException, IOException, ClassNotFoundException {
    Key key = this.generateKey();
    Block blk = new Block(0, "1", "add_node", new Node("127.0.0.2"), "127.0.0.1", key.getPrivateKeyRaw());

    byte[] backup = blk.toBytes();

    Block blkNew = new Block(backup);

    assertTrue(blk.getHash().equals(blkNew.getHash()));
    assertTrue(blk.getSignature().equals(blkNew.getSignature()));
    assertTrue(blk.getSignatureBytes().equals(blkNew.getSignatureBytes()));
    assertTrue(blk.toBytes().equals(blkNew.toBytes()));
    assertTrue(blk.toString().equals(blkNew.toString()));
    assertTrue(blk.toRecord().blockNumber() == blkNew.toRecord().blockNumber());
    assertTrue(blk.toRecord().creationTime() == blkNew.toRecord().creationTime());
    assertTrue(blk.toRecord().previousBlockHash().equals(blkNew.toRecord().previousBlockHash()));
    assertTrue(blk.toRecord().actionType().equals(blkNew.toRecord().actionType()));
    assertTrue(((Node) blk.toRecord().actionData()).nodeIP().equals(((Node) blkNew.toRecord().actionData()).nodeIP()));
    assertTrue(blk.toRecord().creatorIP().equals(blkNew.toRecord().creatorIP()));
    assertTrue(blk.toRecord().toString().equals(blkNew.toRecord().toString()));
  }
}
