package com.github.biltudas1.swiftserve.blockchain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.biltudas1.swiftserve.NodeList;
import com.github.biltudas1.swiftserve.blockchain.exceptions.InconsistentBlockchainException;

/**
 * The blockchain Class refers to the blockchain, and it can perform
 * opeartions like adding block, syncronizing blocks etc.
 * 
 * @param genesisBlock The genesis block of the blockchain
 */
public class Blockchain extends Backup {
  private ArrayList<Block> blocks;

  public Blockchain(Block genesisBlock) {
    this.blocks = new ArrayList<Block>();
    blocks.add(genesisBlock);
  }

  /**
   * Method to add a new block to the blockchain, also verifies if the block data
   * seems correct, if not then it throws error
   * 
   * @param block
   * @throws InvalidParameterException
   */
  public final void add(Block block) throws InvalidParameterException, NoSuchAlgorithmException, FileSystemException,
      IOException, InterruptedException, InvalidKeyException, SignatureException {
    BlockData blockData = block.toRecord();
    // If the added block number is lastblocknumber + 1
    if (!(blockData.blockNumber() == (this.lastBlockNumber() + 1))) {
      throw new InvalidParameterException("blockNumber can only be " + (this.lastBlockNumber() + 1));
    }

    // If the creation time of new block is less-equal than top block of the
    // blockchain
    if (!(this.blocks.getLast().toRecord().creationTime() < blockData.creationTime())) {
      throw new InvalidParameterException("new block can't be created before the top of the block");
    }

    // If the previous block hash is not equal to the top of the blockchain
    if (!(this.blocks.getLast().getHash() == blockData.previousBlockHash())) {
      throw new InvalidParameterException("new block previousBlockHash is different from the top of the block hash");
    }

    // Verifying the signature
    Key key = new Key();
    key.getKey(blockData.creatorIP(), 80);
    if (!block.verifySignature(key.getPublicKeyRaw())) {
      throw new InvalidParameterException("block signature verification failed: signature not matched");
    }

    this.blocks.add(block);
  }

  /**
   * Method for Accessing the last block number in constant time
   * 
   * @return The last block number of the blockchain
   */
  public final long lastBlockNumber() {
    return this.blocks.getLast().toRecord().blockNumber();
  }

  /**
   * Method whith tells how many blocks are into the blockchain
   * 
   * @return The total block count into the blockchain
   */
  public final long size() {
    return this.blocks.size();
  }

  /**
   * Method for Accessing the last block hash in constant time
   * 
   * @return Returns the hash of the last block of the blockchain
   */
  public final String lastBlockHash() {
    return this.blocks.getLast().getHash();
  }

  /**
   * Gets the top block
   * 
   * @return Returns the block object which is the last block of the blockchain
   */
  public final Block topBlock() {
    return this.blocks.getLast();
  }

  /**
   * This method returns the byte version of the blockchain containing all the
   * blocks between the starting block and the end of the blockchain
   * 
   * @param startBlockNum The block number which will be used as starting block
   * @return byte array containing all the blocks information
   * @throws IOException
   */
  public final byte[] getBlocksData(long startBlockNum) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    for (long current = startBlockNum; current <= this.lastBlockNumber(); current++) {
      Block blk = this.blocks.get((int) startBlockNum);
      baos.write(blk.toBytes());
    }

    return baos.toByteArray();
  }

  /**
   * This method loads the blocks data into the local blockchain
   * 
   * @param data          byte array containing one or multiple block information
   * @param startBlockNum The position where the blocks are going to replace
   * @throws IllegalArgumentException
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws SignatureException
   * @throws JsonProcessingException
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws InterruptedException
   */
  public final void loadBlocksData(byte[] data, long startBlockNum)
      throws IllegalArgumentException, NoSuchAlgorithmException, InvalidKeyException, SignatureException,
      JsonProcessingException, IOException, ClassNotFoundException, InterruptedException {
    // Removing blocks until startBlockNum (Inclusive)
    for (long current = this.lastBlockNumber(); current >= startBlockNum; current--) {
      this.blocks.removeLast();
    }

    // Reading the data
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (byte b : data) {
      baos.write(b);

      // End of the block
      if (b == 23) {
        Block blk = new Block(baos.toByteArray());
        this.add(blk); // Adding the new block to the blockchain
        baos.reset();
      }
    }
  }

  /**
   * Gets the block hash of specific position block
   * 
   * @param position Position of the block of the blockchain
   * @return The hash of the selected block
   * @throws IndexOutOfBoundsException
   */
  public final String getBlockHash(long position) throws IndexOutOfBoundsException {
    return this.blocks.get((int) position).getHash();
  }

  /**
   * Method that looks into another node blockchain and then tell what is the
   * starting block that have mismatch between two blockchain
   * 
   * @param ipAddress The node where to look at
   * @param port      Port Number of the Application
   * @return Returns -1 if the whole blockchain is the same, otherwise return the
   *         starting unmathced block number
   * @throws IOException
   * @throws InterruptedException
   * @throws NumberFormatException
   * @throws InconsistentBlockchainException
   * @throws IndexOutOfBoundsException
   */
  public final long collidedBlock(String ipAddress, int port)
      throws IOException, InterruptedException, NumberFormatException, InconsistentBlockchainException,
      IndexOutOfBoundsException {
    long endBlockNumber = NodeList.getLastBlockNumber(ipAddress, port);
    long totalBlocks = NodeList.getTotalBlockCount(ipAddress, port);

    if (!((endBlockNumber + 1) >= totalBlocks)) {
      throw new InconsistentBlockchainException("error: remote computer have inconsistent blocks");
    }

    long startBlockNumber = endBlockNumber - totalBlocks;

    // Binary Search
    long middleBlockNumber = (totalBlocks / 2) + startBlockNumber;
    while (startBlockNumber < endBlockNumber) {
      String hash = NodeList.getHash(ipAddress, port, middleBlockNumber);
      String localHash = this.getBlockHash(middleBlockNumber);
      if (hash.equals(localHash)) {
        startBlockNumber = middleBlockNumber + 1;
      } else {
        endBlockNumber = middleBlockNumber;
      }
      middleBlockNumber = ((endBlockNumber - startBlockNumber) / 2) + startBlockNumber;
    }

    String hash = NodeList.getHash(ipAddress, port, middleBlockNumber);
    String localHash = this.getBlockHash(middleBlockNumber);

    if (hash.equals(localHash)) {
      return -1;
    }

    return middleBlockNumber;
  }
}
