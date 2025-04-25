package com.github.biltudas1.swiftserve;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final record BlockData(
    long blockNumber,
    String previousBlockHash,
    long creationTime,
    String actionType,
    String actionData,
    String creatorIP,
    String signature) {
}

/**
 * Block is the tiny element of blockchain which holds the data
 * 
 * @param blockNumber       The block number, it tells what is the position of
 *                          the block in the blockchain, and it should always be
 *                          unique
 * @param previousBlockHash The hash of the previous block
 * @param actionType        The operation data the block kept
 * @param actionData        The data which require for the operation to perform
 * @param creatorIP         The IP address of the node which created the block
 * @param signature         The digital signarue of the node, which created the
 *                          block for authentication
 */
public final class Block {
  private final BlockData data;
  private final String hash;
  private String json;
  private static ObjectMapper mapper = new ObjectMapper();

  Block(long blockNumber, String previousBlockHash, String actionType, String actionData, String creatorIP,
      String signature) {
    this.data = new BlockData(
        blockNumber,
        previousBlockHash,
        System.currentTimeMillis() / 1000L,
        actionType,
        actionData,
        creatorIP,
        signature);
    this.hash = generateHash(); // This should be at last because it store the hash of current block
  }

  /**
   * Generates the SHA-256 hash of the block data
   * 
   * @return String object containing the hash of the block
   */
  private final String generateHash() {
    String json = this.toString();
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));

      // Convert bytes to hex
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        hexString.append(String.format("%02x", b));
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      return new String();
    }
  }

  /**
   * Returns JSON Style String
   * 
   * @return String object containing the data of block
   */
  public final String toString() {
    if (this.json != null && !this.json.isBlank()) {
      return this.json;
    }

    // If json converstion not possible then return empty string
    try {
      this.json = Block.mapper.writeValueAsString(this.data);
    } catch (JsonProcessingException e) {
      this.json = new String();
    }
    return this.json;
  }

  /**
   * Returns record Class
   * 
   * @return Record object containing the data of block
   */
  public final Record toRecord() {
    return this.data;
  }

  /**
   * Returns the SHA-256 hash of the current block
   * 
   * @return String object containing the hash of the block
   */
  public final String getHash() {
    return this.hash;
  }
}
