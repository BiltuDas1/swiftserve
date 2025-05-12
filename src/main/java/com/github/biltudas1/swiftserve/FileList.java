package com.github.biltudas1.swiftserve;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import com.github.biltudas1.swiftserve.blockchain.Backup;

/**
 * FileInfo stores the file information like the filename, creator, filehash,
 * the chunk hashes etc.
 */
class FileInfo {
  private final String filename;
  private final String creator;
  private final long filesize;

  private HashSet<String> chunkHashes;

  public FileInfo(String filename, String creator, long size) {
    this.filename = filename;
    this.creator = creator;
    this.filesize = size;
  }

  /**
   * @return Actual filename of the file
   */
  public final String getFilename() {
    return this.filename;
  }

  /**
   * @return Get the file creator IP Address
   */
  public final String getCreator() {
    return this.creator;
  }

  /**
   * @return The size of the total file (In Bytes)
   */
  public final long getFileSize() {
    return this.filesize;
  }

  /**
   * Add a new chunk Hash into the list
   * 
   * @param sha1Hash The SHA1 hash of the chunk
   */
  public final void addChunkHash(String sha1Hash) {
    this.chunkHashes.add(sha1Hash);
  }

  /**
   * Remove a chunk hash from the list
   * 
   * @param sha1Hash The SHA1 hash of the chunk
   */
  public final void removeChunkHash(String sha1Hash) {
    this.chunkHashes.remove(sha1Hash);
  }

  /**
   * Checks if the SHA1 hash exist into the Chunk List
   * 
   * @param sha1Hash The SHA1 hash of the chunk
   * @return
   */
  public final boolean isChunkHashExists(String sha1Hash) {
    return this.chunkHashes.contains(sha1Hash);
  }
}

/**
 * FileList keeps all the File details (Filename and Hash) of the file
 */
public class FileList extends Backup {
  private HashMap<String, FileInfo> map = new HashMap<>();
  private static final HttpClient client = HttpClient.newHttpClient();

  public FileList() {
  }

  /**
   * Adds the filename and it's hash to the list
   * 
   * @param filehash The hash of the file
   * @param filename The name of the file (Should be unique)
   * @param creator  The node IP Which actually have the file
   */
  public final void add(String filehash, String filename, String creator, long size) {
    map.put(filehash, new FileInfo(filename, creator, size));
  }

  /**
   * Gets the filehash of a file
   * 
   * @param filehash The hash of the file
   * @return Hash of the file
   */
  public final String getFileName(String filehash) {
    return map.get(filehash).getFilename();
  }

  /**
   * Checks if the hash exist into the File List
   * 
   * @param filehash The hash of the file
   * @return true if the file exist, otherwise false
   */
  public final boolean isFileExist(String filehash) {
    return this.map.containsKey(filehash);
  }

  /**
   * Removes the file from the List
   * 
   * @param filehash The hash of the file
   * @return true if the file data removed, false otherwise
   */
  public final boolean remove(String filehash) {
    return map.remove(filehash) != null;
  }

  /**
   * This method returns the file info of a file
   * 
   * @param filehash The SHA256 hash of the original file
   * @return FileInfo object containing all the file information
   */
  public final FileInfo getFileInfo(String filehash) {
    return map.get(filehash);
  }

  /**
   * Downloads specific chunk from the remote computer and save it to path
   * 
   * @param ipAddress   The remote computer IP Address
   * @param port        The port number of remote computer
   * @param filehash    The hash of the file
   * @param chunkNumber The part number of the file which will be downloaded
   * @param path        The path where the chunk will be downloaded
   * @throws IOException
   * @throws InterruptedException
   */
  public final static void downloadChunk(String ipAddress, int port, String filehash, long chunkNumber, String path)
      throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI
            .create("http://" + ipAddress + ":" + port + "/getChunk?filehash=" + filehash + "&number=" + chunkNumber))
        .build();
    HttpResponse<byte[]> response = FileList.client.send(request, HttpResponse.BodyHandlers.ofByteArray());

    if (response.statusCode() == 200) {
      Path chunkDir = Paths.get(path + "/chunks/" + filehash);
      Files.createDirectories(chunkDir);

      Path filePath = chunkDir.resolve(chunkNumber + ".part");

      try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
        fos.write(response.body());
      }
    } else {
      throw new InterruptedException("chunk not found");
    }
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  /**
   * This method verifies the chunk whether it's matched with the sha1 hash
   * 
   * @param path        The path of download directory
   * @param filehash    The hash of the whole file
   * @param chunkNumber The number of the chunk
   * @param sha1Hash    The sha1 hash of the chunk
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   * @throws NoSuchAlgorithmException
   */
  public final static boolean verifyChunk(String path, String filehash, long chunkNumber, String sha1Hash)
      throws FileNotFoundException, IOException, NoSuchAlgorithmException {
    File chunkFile = new File(path + "/chunks/" + filehash + "/" + chunkNumber + ".part");

    if (!chunkFile.exists()) {
      throw new FileNotFoundException("chunk not found");
    }

    try (FileInputStream fis = new FileInputStream(chunkFile)) {
      MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = fis.read(buffer)) != -1) {
        sha1.update(buffer, 0, bytesRead);
      }

      String computedHash = bytesToHex(sha1.digest());

      if (computedHash.equalsIgnoreCase(sha1Hash)) {
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * Method tells how many chunks are downloaded of a specific file
   * 
   * @param filehash The SHA256 hash of the actual file
   * @param path     The download path
   * @return Count of total chunks
   */
  public final static long totalDownloadedChunks(String filehash, String path) {
    return new File(path + "/chunks/" + filehash).list().length;
  }

  /**
   * The splitFile methods takes one large file and then split it into specific
   * size files, and store it into <save_path>/chunks/<hash_of_the_file>
   * directory, If the parent directory doesn't exist then it will be create
   * automatically
   * 
   * @param sourceFile      The source file which is going to split
   * @param partSizeInBytes The each parted file size
   * @param filehash        The hash of the actual file
   * @param savePath        The path where the file will be saved
   * @throws IOException
   */
  public final static void splitFile(String sourceFile, int partSizeInBytes, String filehash,
      String savePath)
      throws IOException {
    File inputFile = new File(sourceFile);
    FileInputStream fis = new FileInputStream(inputFile);

    byte[] buffer = new byte[partSizeInBytes];
    int bytesRead;
    int partCounter = 1;

    Path chunkDir = Paths.get(savePath + "/chunks/" + filehash);
    Files.createDirectories(chunkDir);

    while ((bytesRead = fis.read(buffer)) > 0) {
      Path filePath = chunkDir.resolve((partCounter++) + ".part");

      try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
        fos.write(buffer, 0, bytesRead);
      }
    }

    fis.close();

  }

  /**
   * The combineFiles method combines the part files into one file
   * 
   * @param outputFile       The filepath with filename where you want to save the
   *                         file
   * @param partFileLocation The directory which contains all the part files
   * @throws IOException
   */
  public final static void combineFiles(String outputFile, String partFileLocation) throws IOException {
    File dir = new File(partFileLocation);
    File[] partFiles = dir.listFiles((d, name) -> name.endsWith(".part"));

    Arrays.sort(partFiles, Comparator.comparing(File::getName));

    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      for (File part : partFiles) {
        try (FileInputStream fis = new FileInputStream(part)) {
          byte[] buffer = new byte[1024];
          int bytesRead;
          while ((bytesRead = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, bytesRead);
          }
        }
      }
    }
  }
}
