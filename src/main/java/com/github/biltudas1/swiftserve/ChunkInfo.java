package com.github.biltudas1.swiftserve;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

// Refers to the chunk details and it's location
public record ChunkInfo(long chunkNumber, long totalChunks, String sha1, String filehash, String nodeIP, int port) {
  /**
   * Convert the Record to bytes
   * 
   * @return object in the byte array format
   */
  public byte[] toBytes() {
    byte[] sha1Bytes = sha1.getBytes(StandardCharsets.UTF_8);
    byte[] filehashBytes = filehash.getBytes(StandardCharsets.UTF_8);
    byte[] ipBytes = nodeIP.getBytes(StandardCharsets.UTF_8);

    ByteBuffer buffer = ByteBuffer
        .allocate(Long.BYTES + Long.BYTES + Integer.BYTES + sha1Bytes.length + Integer.BYTES + filehashBytes.length
            + Integer.BYTES
            + ipBytes.length + Integer.BYTES);

    buffer.putLong(chunkNumber);
    buffer.putLong(totalChunks);
    buffer.putInt(sha1Bytes.length);
    buffer.put(sha1Bytes);
    buffer.putInt(filehashBytes.length);
    buffer.put(filehashBytes);
    buffer.putInt(ipBytes.length);
    buffer.put(ipBytes);
    buffer.putInt(port);

    return buffer.array();
  }

  /**
   * Take the byte array and convert it to the Record
   * 
   * @param data the byte array containing all the data
   * @return The ChunkInfo object contains all the data copied from the byte array
   */
  public static ChunkInfo fromBytes(byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);

    long chunkNumber = buffer.getLong();
    long totalChunks = buffer.getLong();

    int sha1Len = buffer.getInt();
    byte[] sha1Bytes = new byte[sha1Len];
    buffer.get(sha1Bytes);
    String sha1 = new String(sha1Bytes, StandardCharsets.UTF_8);

    int filehashLen = buffer.getInt();
    byte[] filehashBytes = new byte[filehashLen];
    buffer.get(filehashBytes);
    String filehash = new String(filehashBytes, StandardCharsets.UTF_8);

    int ipLen = buffer.getInt();
    byte[] ipBytes = new byte[ipLen];
    buffer.get(ipBytes);
    String nodeIP = new String(ipBytes, StandardCharsets.UTF_8);

    int port = buffer.getInt();

    return new ChunkInfo(chunkNumber, totalChunks, sha1, filehash, nodeIP, port);
  }
}
