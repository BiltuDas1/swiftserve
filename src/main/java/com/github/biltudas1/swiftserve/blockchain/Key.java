package com.github.biltudas1.swiftserve.blockchain;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystemException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Key {
  private PrivateKey prvkey;
  private PublicKey pubkey;
  private final Pattern keyPattern = Pattern.compile("^-{5}(END|BEGIN) (PUBLIC|PRIVATE) KEY-{5}$");
  private static final HttpClient client = HttpClient.newHttpClient();

  public Key() {
  }

  public Key(KeyPair keypair) {
    this.prvkey = keypair.getPrivate();
    this.pubkey = keypair.getPublic();
  }

  /**
   * This method loads the key from the path, it can load
   * Public or Private Keys from the local system. If a pem file contains more
   * than one smae type of key (Public or Private) then only the first one get
   * loaded
   * 
   * @param filepath The path where the key file located
   * @return true if the key loaded successfully, otherwise false
   * @throws NoSuchAlgorithmException
   */
  public final boolean loadKey(String filepath) throws NoSuchAlgorithmException {
    File ff = new File(filepath);
    if (!(ff.exists() && !ff.isDirectory())) {
      return false;
    }
    try {
      // Key found, loading the key
      Scanner reader = new Scanner(ff);

      while (reader.hasNextLine()) {
        // Reading Header (Public or Private Key)
        Matcher header = this.keyPattern.matcher(reader.nextLine());
        // If not matched then it's invalid pem file
        if (!header.find()) {
          reader.close();
          return false;
        }

        // Reading Body and storing it into data
        String keyType = header.group(2);
        byte[] key = Base64.getDecoder().decode(reader.nextLine());
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        try {
          if (keyType.equals("PUBLIC")) {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(key);
            this.pubkey = kf.generatePublic(spec);
          } else if (keyType.equals("PRIVATE")) {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(key);
            this.prvkey = kf.generatePrivate(spec);
          } else {
            reader.close();
            return false;
          }
        } catch (InvalidKeySpecException err) {
          reader.close();
          return false;
        }

        // Reading footer (Public or Private Key)
        Matcher footer = this.keyPattern.matcher(reader.nextLine());
        // If not matched then it's invalid pem file
        if (!footer.find()) {
          reader.close();
          return false;
        }
      }
      reader.close();

    } catch (FileNotFoundException err) {
      return false;
    }
    return true;
  }

  /**
   * This method allows to save the Private and Public key to the specific
   * location as a pem format
   * 
   * @param filepath The location where the file will be stored
   * @return Returns true if saving successful, otherwise false
   * @throws IOException
   */
  public final boolean saveKey(String filepath) throws IOException {
    File ff = new File(filepath);
    if (!ff.createNewFile()) {
      return false;
    }

    FileWriter writer = new FileWriter(filepath);
    StringBuffer pem = new StringBuffer();
    if (this.prvkey != null) {
      pem.append("-----BEGIN PRIVATE KEY-----\n");
      pem.append(Base64.getEncoder().encodeToString(this.prvkey.getEncoded()) + "\n");
      pem.append("-----END PRIVATE KEY-----\n");
    }

    if (this.pubkey != null) {
      pem.append("-----BEGIN PUBLIC KEY-----\n");
      pem.append(Base64.getEncoder().encodeToString(this.pubkey.getEncoded()) + "\n");
      pem.append("-----END PUBLIC KEY-----\n");
    }

    writer.write(pem.toString());
    writer.close();

    return true;
  }

  /**
   * The getKey is a function that loads the public Key from the local machine or
   * from the remote ipAddress, for local machine it looks into keys directory
   * (into the current directory) and then it checks for the file with the
   * specific ip address, if it is doesn't exist then it try to download the key
   * from the /key.pem endpoint on the given ip address
   * 
   * @param ipAddress The ip address which the key belongs to
   */
  public final void getKey(String ipAddress, int port)
      throws NoSuchAlgorithmException, FileSystemException, IOException, InterruptedException {
    File dir = new File("keys/");
    if (!(dir.exists() && dir.isDirectory())) {
      if (!dir.mkdirs()) {
        throw new FileSystemException("unable to create directory keys/");
      }
    }

    // If loading key is not possible then download the key from remote
    if (!this.loadKey("keys/" + ipAddress + ".pem")) {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://" + ipAddress + ":" + port + "/key.pem"))
          .build();
      HttpResponse<byte[]> response = Key.client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      FileOutputStream fos = new FileOutputStream("keys/" + ipAddress + ".pem");
      fos.write(response.body());
      fos.close();

      // Again try to load the Key
      this.loadKey("keys/" + ipAddress + ".pem");
    }
  }

  /**
   * Get the public key in Base64 format
   * 
   * @return Return string containing the public key
   */
  public final String getPublicKey() {
    return Base64.getEncoder().encodeToString(this.pubkey.getEncoded());
  }

  /**
   * Get the public key in PublicKey type format, if not exist then it returns
   * null value
   * 
   * @return Returns PublicKey object containing the public Key
   */
  public final PublicKey getPublicKeyRaw() {
    if (this.pubkey != null) {
      return this.pubkey;
    } else {
      return null;
    }
  }

  /**
   * Get the private key in PrivateKey type format, if not exist then it returns
   * null value
   * 
   * @return Returns PrivateKey object containing the private Key
   */
  public final PrivateKey getPrivateKeyRaw() {
    if (this.prvkey != null) {
      return this.prvkey;
    } else {
      return null;
    }
  }

}
