package com.github.biltudas1.swiftserve.blockchain.exceptions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class InconsistentBlockchainExceptionTest {
  @Test
  public void testException() {
    InconsistentBlockchainException except = new InconsistentBlockchainException("This is test Exception");
    assertEquals("This is test Exception", except.getMessage());
  }
}
