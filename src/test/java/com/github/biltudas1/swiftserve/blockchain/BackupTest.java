package com.github.biltudas1.swiftserve.blockchain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.Test;

/**
 * Test Class for Testing Backup
 */
class TestClass extends Backup {
  public final int number = 10;
  public final String str = "TestString";
  public final double flt = 3.141;

  public double multiply() {
    return this.number * this.flt;
  }
}

public class BackupTest {
  @Test
  public void testBackup() {
    TestClass test = new TestClass();
    test.save("testbackup.bak");

    File ff = new File("testbackup.bak");
    assertTrue(ff.exists() && ff.isFile());
    ff.delete(); // Deleting the file
  }

  @Test
  public void testRestore() {
    TestClass test = new TestClass();
    test.save("testbackup.bak");

    TestClass testLoaded = (TestClass) TestClass.load("testbackup.bak");
    assertTrue(testLoaded.number == 10);
    assertTrue(testLoaded.str.equals("TestString"));
    assertTrue(testLoaded.flt == 3.141);
    assertTrue(testLoaded.multiply() == 31.41);

    // Cleaning
    File ff = new File("testbackup.bak");
    ff.delete();
  }
}
