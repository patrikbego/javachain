# JavaChain - the introduction to blockchain in Java 

JavaChain is a simple project which demonstrates the basic blockchain implementation written in Java. 

The purpose of Javachain is to give you basic idea of how the blockchain works and its ???internals???. 

Project structure: (tree)
```
├── README.md
├── mvnw
├── mvnw.cmd
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── javachain
    │   │           ├── JcApplication.java
    │   │           ├── ShellExecutor.java
    │   │           ├── aspect
    │   │           │   └── JcAspect.java
    │   │           ├── dto
    │   │           │   ├── Block.java
    │   │           │   ├── InTransaction.java
    │   │           │   ├── OutTransaction.java
    │   │           │   ├── Transaction.java
    │   │           │   └── Wallet.java
    │   │           ├── service
    │   │           │   ├── BlockService.java
    │   │           │   ├── MiningService.java
    │   │           │   ├── TransactionService.java
    │   │           │   └── WalletService.java
    │   │           └── util
    │   │               ├── EncodingUtility.java
    │   │               ├── EncryptionUtility.java
    │   │               └── HashingUtility.java
    │   └── resources
    │       └── application.properties
    └── test
        └── java
            └── com
                └── javachain
                    ├── BlockServiceIT.java
                    ├── JcApplicationIT.java
                    ├── TestApplicationRunner.java
                    ├── WeaknessesIT.java
                    ├── service
                    │   ├── BlockServiceTest.java
                    │   ├── MiningServiceTest.java
                    │   ├── TransactionServiceTest.java
                    │   └── WalletServiceTest.java
                    └── util
                        ├── EncodingUtilityTest.java
                        ├── EncryptionUtilityTest.java
                        └── HashingUtilityTest.java
```                       


To build the project simply run:
`mvn clean install`     
Project is meant to be executed from command line (work in progress) as executable main class:
`JcApplication` 
and run specific commands (for now there is just executable command: `miner text difficulty`)
Or run tests in BlockChainIT to get the idea how blockchain technology really works.

**Blockchain Technical concepts/terms:**    

WALLET:
- Is a private store secured with public/private keys. 
- Each wallet communicates with another wallet trough special protocol. 
- Each wallet contains whole blockcain.
- wallet doesn’t store the "coins" themselves. 
- Information about "coins" and balances are stored in the blockchain.

BLOCKCHAIN (in project represented as Block):
- Is a list of transactions.
- All transactions are grouped in files called blocks. 
- Bitcoin for example adds a new block of transactions every 10 minutes. 
- Once a new block is added to the blockchain, it becomes immutable and can't be deleted or modified. 
- Miner mines blocks (mining happens in a block): each block is constructed from:???
    - miners address
    - transaction list
    - previous/parent block (first block is always initial block and that gives the miner reward + fee)
    - The first miner to submit a valid block gets his block added to the blockchain and receives the reward in bitcoins
    - if there is a conflict (simultaneous mine) on the blockchain, then the the longest chain wins

MINING (in project represented as MiningService):
- Mining is a process of validating transactions and adding it to the blockchain.
- Senders adds a fee to a transaction to incentivize the miners to add their transactions 
  to the blocks.
- If a mined block is accepted by the blockchain, the miner receives a reward, which is an additional 
  incentive to transaction fees.
- For a block to be accepted by the blockchain, it needs to be "mined". 
  To mine a block, miners need to find a nonce (solve cryptographic puzzle). 

TRANSACTION (in project represented as Transaction/InTransaction/OutTransaction):
- wallet (UUID or public key of the transaction owner) //t2.setWallet(patriksWallet);
- List of In transactions (that is constructed on In transaction and OutputIndex of previous Out transaction list 
(they have to match - this could be improved)) // t5.setInTransactions(Arrays.asList(new InTransaction(t2, 2), 
new InTransaction(t4, 0))); 
- List of Out transactions (money sent out of ur wallet (do we need to sent money to our selves?))

PRIVATE KEY:    
- A secret string of randomly generated alphanumerical characters, known only to the person that generated it. 
- In Bitcoin, someone with the private key that corresponds to funds on the public ledger can spend the funds. 
- In Bitcoin, a private key is a single unsigned 256 bit integer (32 bytes).

PUBLIC KEY:     
- A number that corresponds to a private key, but does not need to be kept secret. 
- A public key can be calculated from a private key, but not vice versa. 
- A public key can be used to determine if a signature is genuine (in other words, produced with the proper key) 
without requiring the private key to be divulged. In Bitcoin, public keys are either compressed or uncompressed. 
Compressed public keys are 33 bytes, consisting of a prefix either 0x02 or 0x03, and a 256-bit integer called x. 
The older uncompressed keys are 65 bytes, consisting of constant prefix (0x04), 
followed by two 256-bit integers called x and y (2 * 32 bytes). 
The prefix of a compressed key allows for the y value to be derived from the x value.

SIGNATURE:      
A number that proves that a signing operation took place. 
A signature is mathematically generated from a hash of something to be signed, plus a private key. 
The signature itself is two numbers known as r and s. 
With the public key, a mathematical algorithm can be used on the signature to determine that it was originally 
produced from the hash and the private key, without needing to know the private key. 
Signatures are either 73, 72, or 71 bytes long, with probabilities approximately 25%, 50% and 25% respectively, 
although sizes even smaller than that are possible with exponentially decreasing probability.


**Links and useful resources**
https://en.wikipedia.org/wiki/Elliptic_Curve_Digital_Signature_Algorithm
https://danielmiessler.com/study/encoding-encryption-hashing-obfuscation/
https://bitcoin.org/bitcoin.pdf
http://www.michaelnielsen.org/ddi/how-the-bitcoin-protocol-actually-works/

Conclusion: 
    
Hope this gives you a better overview on how the blockhains work and principles
behind them. 
While Bitcoin could be a future alternative to current transaction baking systems, there are certain issues which could be improved 
(like transactions speed and issues with scalability)
Another vulnerability is it's binding to a certain hashing algorithm. While that is not 
an issue at the current times it could become in 10, 20 ... years. 
And here I see a chance for improvement. 

Powered By: 

[![N|Solid](http://mubigo.com/mubigo-logo.png)](https://mubigo.com)