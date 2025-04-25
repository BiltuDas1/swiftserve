package com.github.biltudas1.swiftserve.blockchain;

/**
 * File record refers to the actionData which only applicable on the actionTypes
 * 'add_file', 'remove_file'
 */
public final record File(
    String filename,
    String filehash) implements ActionData {
}