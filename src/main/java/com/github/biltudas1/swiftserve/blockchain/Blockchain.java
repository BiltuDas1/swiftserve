package com.github.biltudas1.swiftserve.blockchain;

import java.security.InvalidParameterException;
import java.util.ArrayList;

/**
 * The blockchain Class refers to the blockchain, and it can perform
 * opeartions like adding block, syncronizing blocks etc.
 * 
 * @param genesisBlock The genesis block of the blockchain
 */
public class Blockchain {
  private ArrayList<Block> blocks;
  private BlockData topBlockData;
  private String topBlockHash;

  public Blockchain(Block genesisBlock) {
    this.blocks = new ArrayList<Block>();
    this.topBlockData = genesisBlock.toRecord();
    this.topBlockHash = genesisBlock.getHash();
    blocks.add(genesisBlock);
  }

  /**
   * Method to add a new block to the blockchain, also verifies if the block data
   * seems correct, if not then it throws error
   * 
   * @param block
   * @throws InvalidParameterException
   */
  public final void add(Block block) throws InvalidParameterException {
    BlockData blockData = block.toRecord();
    // If the added block number is lastblocknumber + 1
    if (!(blockData.blockNumber() == (this.lastBlockNumber() + 1))) {
      throw new InvalidParameterException("blockNumber can only be " + (this.lastBlockNumber() + 1));
    }

    // If the creation time of new block is less-equal than top block of the
    // blockchain
    if (!(this.topBlockData.creationTime() < blockData.creationTime())) {
      throw new InvalidParameterException("new block can't be created before the top of the block");
    }

    // If the previous block hash is not equal to the top of the blockchain
    if (!(this.topBlockHash == blockData.previousBlockHash())) {
      throw new InvalidParameterException("new block previousBlockHash is different from the top of the block hash");
    }

    // Verifying the signature

    this.topBlockData = block.toRecord();
    this.blocks.add(block);
  }

  /**
   * Method for Accessing the last block number in constant time
   * 
   * @return The last block number of the blockchain
   */
  public final long lastBlockNumber() {
    return this.topBlockData.blockNumber();
  }

  /**
   * Method for Accessing the last block hash in constant time
   * 
   * @return Returns the hash of the last block of the blockchain
   */
  public final String lastBlockHash() {
    return this.topBlockHash;
  }

  /**
   * Gets the top block
   * 
   * @return Returns the block object which is the last block of the blockchain
   */
  public final Block topBlock() {
    return this.blocks.getLast();
  }

}
