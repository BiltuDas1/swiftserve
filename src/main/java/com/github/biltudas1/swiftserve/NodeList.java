package com.github.biltudas1.swiftserve;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import com.github.biltudas1.swiftserve.blockchain.Backup;

/**
 * NodeList keeps the IP Addresses of all the nodes into the blockchain
 */
public class NodeList extends Backup {
  private final ArrayList<String> list = new ArrayList<>();
  private final HashSet<String> set = new HashSet<>();
  private final Random rand = new Random();
  private static final HttpClient client = HttpClient.newHttpClient();

  public NodeList() {
  }

  /**
   * This method adds Node IP Address to the list, it only keeps unique IP
   * Addresses
   * 
   * @param ipAddress The IP Address to add
   * @return true if successfully added, otherwise false
   */
  public final boolean add(String ipAddress) {
    if (set.contains(ipAddress)) {
      return false;
    }
    set.add(ipAddress);
    list.add(ipAddress);
    return true;
  }

  /**
   * This method removes the IP Address from the List
   * 
   * @param ipAddress IP Address which will be removed
   * @return true if address removed, otherwise false
   */
  public final boolean remove(String ipAddress) {
    if (!set.contains(ipAddress)) {
      return false;
    }
    set.remove(ipAddress);
    list.remove(ipAddress); // O(n) time
    return true;
  }

  /**
   * Method that picks random k IP Addresses from the list
   * 
   * @param k The sample size
   * @return String array containing randomly picked IP Addresses
   * @throws IllegalArgumentException
   */
  public final String[] randomPicks(int k) throws IllegalArgumentException {
    if (k > list.size()) {
      throw new IllegalArgumentException("Sample size exceeds list size");
    }

    ArrayList<String> shuffled = new ArrayList<String>(list);
    for (int i = 0; i < k; i++) {
      int j = i + rand.nextInt(shuffled.size() - i);
      Collections.swap(shuffled, i, j);
    }

    return shuffled.subList(0, k).toArray(new String[0]);
  }

  /**
   * Returns the size of the list
   * 
   * @return size of the list
   */
  public final int size() {
    return list.size();
  }

  /**
   * Get the Hash of the specific block on the remote machine of the blockchain
   * 
   * @param ipAddress   The node where to look at
   * @param port        Port Number of the Application
   * @param blockNumber The block number of the blockchain
   * @return The hash value of the block into the remote machine
   * @throws IOException
   * @throws InterruptedException
   */
  public final static String getHash(String ipAddress, int port, long blockNumber)
      throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://" + ipAddress + ":" + port + "/getHash?num=" + blockNumber))
        .build();
    HttpResponse<String> response = NodeList.client.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  /**
   * Get the Top block number on the remote machine of the blockchain
   * 
   * @param ipAddress The node where to look at
   * @param port      Port Number of the Application
   * @return Returns the last block number in the remote blockchain
   * @throws IOException
   * @throws InterruptedException
   * @throws NumberFormatException
   */
  public final static long getLastBlockNumber(String ipAddress, int port)
      throws IOException, InterruptedException, NumberFormatException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://" + ipAddress + ":" + port + "/topBlockNumber"))
        .build();
    HttpResponse<String> response = NodeList.client.send(request, HttpResponse.BodyHandlers.ofString());
    return Long.parseLong(response.body());
  }

  /**
   * Get the total block count into the blockchain
   * 
   * @param ipAddress The node where to look at
   * @param port      Port Number of the Application
   * @return Returns the total block count in the remote blockchain
   * @throws IOException
   * @throws InterruptedException
   * @throws NumberFormatException
   */
  public final static long getTotalBlockCount(String ipAddress, int port)
      throws IOException, InterruptedException, NumberFormatException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://" + ipAddress + ":" + port + "/totalBlocks"))
        .build();
    HttpResponse<String> response = NodeList.client.send(request, HttpResponse.BodyHandlers.ofString());
    return Long.parseLong(response.body());
  }

  /**
   * Method that retieves the block data from the mentioned block to the end of
   * the blockchain in the remote machine
   * 
   * @param ipAddress     The node where to look at
   * @param port          Port Number of the Application
   * @param startBlockNum Starting block to select
   * @return The byte array consist of the selected starting block to the end of
   *         the blockchain
   * @throws IOException
   * @throws InterruptedException
   * @throws NumberFormatException
   */
  public final static byte[] getBlocksData(String ipAddress, int port, long startBlockNum)
      throws IOException, InterruptedException, NumberFormatException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://" + ipAddress + ":" + port + "/getBlockDatas"))
        .header("Content-Type", "text/plain")
        .POST(BodyPublishers.ofString(Long.toString(startBlockNum)))
        .build();
    HttpResponse<byte[]> response = NodeList.client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    return response.body();
  }

  /**
   * This method checks the node list and ping them all for the mentioned block
   * hash, and then group the block hashes and return the nodes list which have
   * the most common hash for the specific block. If a node returns no hash then
   * it will be skipped.
   * 
   * @param nodes    The list of the nodes IP Addresses which needs to check
   * @param port     The port number of the node
   * @param blockNum The block number which is going to verify in each node
   * @return Empty ArrayList if most nodes don't return any hash, if two hash
   *         group have same amount of nodes then this method will follow first
   *         come first serve basis to detect most common hash. Otherwise it will
   *         return the list containing the nodes which have the most common hash.
   * @throws IOException
   * @throws InterruptedException
   */
  public final static ArrayList<String> mostMatchedHashNodes(String[] nodes, int port, long blockNum)
      throws IOException, InterruptedException {
    HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
    int totalNodes = 0;
    String mostCommonHash = "";

    for (String ipAddress : nodes) {
      String hash = NodeList.getHash(ipAddress, port, blockNum);
      if (map.containsKey(hash)) {
        map.get(hash).add(ipAddress);
      } else {
        ArrayList<String> lst = new ArrayList<String>();
        lst.add(ipAddress);
        map.put(hash, lst);
      }

      if (totalNodes < map.get(hash).size()) {
        totalNodes = map.get(hash).size();
        mostCommonHash = hash;
      }
    }

    if (mostCommonHash.equals("")) {
      return new ArrayList<String>();
    } else {
      return map.get(mostCommonHash);
    }
  }
}
