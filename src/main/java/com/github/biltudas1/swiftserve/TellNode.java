package com.github.biltudas1.swiftserve;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;

/**
 * Implementation of Runnable which tells other nodes which chunk the current
 * node has been downloaded
 * 
 * @param ipAddress       the ip address of the target node where the chunk
 *                        details will be shared
 * @param port            the port of the remote computer
 * @param chunkNumber     the chunk number which the remote node is going to
 *                        download
 * @param sha1Hash        the sha1 hash for verifying the chunk integrity
 * @param currentNodeIP   the IP Address of the current node
 * @param currentNodePort the port of the current node
 * @param filehash        the SHA256 hash of the whole file
 */
public record TellNode(String ipAddress, int port, long chunkNumber, long totalChunks, String sha1Hash,
    String currentNodeIP,
    int currentNodePort, String filehash) implements Runnable {

  private static final HttpClient client = HttpClient.newHttpClient();

  @Override
  public void run() {
    try {
      ChunkInfo chunk = new ChunkInfo(chunkNumber, totalChunks, sha1Hash, filehash, currentNodeIP, currentNodePort);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create("http://" + this.ipAddress + ":" + this.port + "/tellAboutChunk"))
          .header("Content-Type", "application/octet-stream")
          .POST(BodyPublishers.ofByteArray(chunk.toBytes()))
          .build();
      HttpResponse<String> response = TellNode.client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        if (response.body().contains("true")) {
          SwiftserveApplication.remainingPeersToKnowAboutChunk -= 1;
        }
      }
    } catch (Exception e) {
      e.getStackTrace();
      return;
    }
  }
}
