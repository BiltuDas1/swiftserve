package com.github.biltudas1.swiftserve.blockchain;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

/**
 * Block is the smallest element of blockchain which holds the data
 * 
 * @param blockNumber       The block number, it tells what is the position of
 *                          the block in the blockchain, and it should always be
 *                          unique
 * @param previousBlockHash The hash of the previous block
 * @param actionType        The operation data the block kept
 * @param actionData        The data which require for the operation to perform
 * @param creatorIP         The IP address of the node which created the block
 * @param key               The key is the private key of EdDSA (aka
 *                          Ed25519), which will be used for digital sign
 *                          the block of the blockchain
 * @throws IllegalArgumentException
 * @throws NoSuchAlgorithmException
 * @throws InvalidKeyException
 * @throws SignatureException
 */
public final class Block {
  private final BlockData data;
  private final String hash;
  private String json;
  private byte[] signature;
  private static ObjectMapper mapper = new ObjectMapper();

  public Block(long blockNumber, String previousBlockHash, String actionType, ActionData actionData, String creatorIP,
      PrivateKey key)
      throws IllegalArgumentException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    this.data = new BlockData(
        blockNumber,
        previousBlockHash,
        System.currentTimeMillis() / 1000L,
        actionType,
        actionData,
        creatorIP);

    // If the instance is actionType is another type rather than we need as input
    if (!(Node.class.isInstance(actionData) || File.class.isInstance(actionData))) {
      throw new IllegalArgumentException("invalid type of actionData: " + actionData.getClass().getName());
    }

    this.signature = this.Sign(key);
    this.hash = generateHash(); // This should be at last because it store the hash of current block
  }

  /**
   * Generates the SHA-256 hash of the block data
   * 
   * @return String object containing the hash of the block
   * @throws NoSuchAlgorithmException
   */
  private final String generateHash() throws NoSuchAlgorithmException {
    String json = this.toString();

    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));

    // Convert bytes to hex
    StringBuilder hexString = new StringBuilder();
    for (byte b : hashBytes) {
      hexString.append(String.format("%02x", b));
    }
    return hexString.toString();
  }

  /**
   * Returns JSON Style String, If not possible then it returns empty string
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
  public final BlockData toRecord() {
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

  /**
   * The Sign method allows to sign the block with the creator's private key
   *
   * @param key key refers to the private key
   * @return Returns the base64 encoded string containing the signature
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws SignatureException
   */
  private final byte[] Sign(PrivateKey key) throws NoSuchAlgorithmException,
      InvalidKeyException, SignatureException {
    Signature sig = Signature.getInstance("Ed25519");
    sig.initSign(key);
    sig.update(this.toString().getBytes(StandardCharsets.UTF_8));
    return sig.sign();
  }

  /**
   * Returns the current block object signature in base64 format
   * 
   * @return String object containing the signature
   */
  public final String getSignature() {
    return Base64.getEncoder().encodeToString(this.signature);
  }

  /**
   * Returns the current block object signature in byte array format
   * 
   * @return Byte array containing the signature
   */
  public final byte[] getSignatureBytes() {
    return this.signature;
  }

  /**
   * Verifies if the block is really signed by the owner of the public key and the
   * block is not tampared in any way
   * 
   * @param pubKey The public key which will be used for verification
   * @return true if the Signature matched, otherwise false
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws SignatureException
   */
  public final boolean verifySignature(PublicKey pubKey)
      throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    Signature verifier = Signature.getInstance("Ed25519");
    verifier.initVerify(pubKey);
    verifier.update(this.toString().getBytes(StandardCharsets.UTF_8));
    return verifier.verify(this.signature);
  }

}
