package com.github.biltudas1.swiftserve.blockchain;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Class Refers to Backup/Restore Objects
 */
public class Backup implements Serializable {
  /**
   * Store the Object into file for backup
   * 
   * @param filename Filename where to store the backup
   */
  public final void save(String filename) {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
      oos.writeObject(this);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Loads the file and then restore the Object data
   * 
   * @param filename Filename where the backup is saved
   * @return New object
   */
  public final static Object load(String filename) {
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
      return ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }
}
