package com.github.biltudas1.swiftserve.blockchain;

import java.io.Serializable;

/**
 * BlockData record refers to the common block data, which always exist into
 * all blocks into the blockchain
 */
final public record BlockData(
    long blockNumber,
    String previousBlockHash,
    long creationTime,
    String actionType,
    ActionData actionData,
    String creatorIP) implements Serializable {

  public BlockData {
    if (!(previousBlockHash.matches("^[0-9a-fA-F]+$"))) {
      throw new IllegalArgumentException("invalid previousBlockHash: not a valid hex string");
    }
  }
}