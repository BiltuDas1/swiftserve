package com.github.biltudas1.swiftserve;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;

import com.github.biltudas1.swiftserve.blockchain.Block;

import java.net.http.HttpResponse;

public record SendBlock(String ipAddress, int port, Block block) implements Runnable {

  private static final HttpClient client = HttpClient.newHttpClient();

  @Override
  public void run() {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create("http://" + this.ipAddress + ":" + this.port + "/addBlock"))
          .header("Content-Type", "application/octet-stream")
          .POST(BodyPublishers.ofByteArray(this.block.toBytes()))
          .build();
      SendBlock.client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    } catch (Exception e) {
      e.getStackTrace();
      return;
    }
  }
}
