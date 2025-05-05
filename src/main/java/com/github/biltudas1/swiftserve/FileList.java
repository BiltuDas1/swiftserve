package com.github.biltudas1.swiftserve;

import java.util.HashMap;

import com.github.biltudas1.swiftserve.blockchain.Backup;

/**
 * FileInfo stores the file hash and the creator IP Address who have the file
 */
record FileInfo(String hash, String creator) {
}

/**
 * FileList keeps all the File details (Filename and Hash) of the file
 */
public class FileList extends Backup {
  private HashMap<String, FileInfo> map = new HashMap<>();

  public FileList() {
  }

  /**
   * Adds the filename and it's hash to the list
   * 
   * @param filename The name of the file (Should be unique)
   * @param filehash The hash of the file
   * @param creator  The node IP Which actually have the file
   */
  public final void add(String filename, String filehash, String creator) {
    map.put(filename, new FileInfo(filehash, creator));
  }

  /**
   * Gets the filehash of a file
   * 
   * @param filename The name of the file
   * @return Hash of the file
   */
  public final String getFileHash(String filename) {
    return map.get(filename).hash();
  }

  /**
   * Removes the file from the List
   * 
   * @param filename The name of the file
   * @return true if the file data removed, false otherwise
   */
  public final boolean remove(String filename) {
    return map.remove(filename) != null;
  }
}
