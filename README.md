# SwiftServe

Swiftserve is a distributed file service which keeps file into different types of nodes, and allows multiple users to download the files from these nodes.

### How it works

1. The system is using a blockchain which keeps the note of all the nodes and their ip address, also the file metadata information (which the user is going to be download)
2. Each node will create a local copy which contains all the nodes list and files list
3. The gossip (Spreading the block) works like this: The creator (node who signed the block) pick random peers (max 4) and send the block and again the peers will do the same. If any node found redundant block then it stop spreading.
4. Each node have EdDSA (Ed25519) public key of all nodes into the blockchain for verification of the data.
5. If any node found that it have outdated blocks, or the previous hash is not matching with the latest block. Then it checks which top block hash is the most common in the blockchain, and picks one random node which have the top block hash and then it start copying the blockchain data from that node.
6.

### The folder structure of downloaded files

```
downloads/
├── file1.zip
├── file2.zip
└── chunks/
    ├── df3b49a3cf0ada038d474bc0e7063aefdd322dcbba86e92f6c84e3b19d2e379f/
    │   ├── 1.part
    │   ├── 2.part
    │   └── 3.part
    └── a849858266e87a54e64b7f5c2207aed67d9daed3fa1d26644c28fe150b78674f/
        ├── 1.part
        └── 2.part
```

### References

- [EdDSA in Java (Example)](https://howtodoinjava.com/java15/java-eddsa-example/)
