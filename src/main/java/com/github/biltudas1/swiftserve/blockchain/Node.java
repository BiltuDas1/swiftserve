package com.github.biltudas1.swiftserve.blockchain;

/**
 * Node record refers to the actionData which only applicable on the actionTypes
 * 'add_node', 'remove_node'
 */
public final record Node(
    String nodeIP) implements ActionData {
}