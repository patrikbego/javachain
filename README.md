# JavaChain - introduction to blockchain in Java 

JavaChain is a simple blockchain implementation based on Java and Spring Boot Shell, made mainly to give you a basic idea of 
how the blockchain works.  

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


To build the project, run:
`mvn clean install`     
The project is executable from the command line (work in progress) as an executable main class:
`JcApplication`  (`java -jar target/jc-0.0.1-SNAPSHOT.jar`)  
which will open a shell where we can run specific commands. For now, there is just one executable command: `miner <data> <difficulty>`.


## BASIC CONCEPTS BEHIND BLOCKCHAIN  

Blockchain is heavily dependent on cryptography and cryptographic concepts that provide a basis for a reliable and secure decentralized system.  
I will try to quickly and as simple as possible explain the basic concepts behind cryptography used in blockchains.
&nbsp;&nbsp;&nbsp;&nbsp;  
&nbsp;&nbsp;&nbsp;&nbsp;  

**ENCODING** 

Encoding is the process of transforming one format of data into another, so a system or a user can process it.  
&nbsp;&nbsp;&nbsp;&nbsp;E.g. of encoding types: UTF-8 to ASCII, Base64 ...  
&nbsp;&nbsp;&nbsp;&nbsp;Practical e.g.: if we encode the string "this is not really a secret" to base64 format, it will generate a encoded output "dGhpcyBpcyBub3QgcmVhbGx5IGEgc2VjcmV0".  
        This encoded output can then be used to transport data over different protocols and later decoded back into a human-readable format.
        
In the JavaChain project, the encoding method is located in the `EncodingUtility` class. In the blockchain, it is used mainly to convert bytes to hexadecimal representations during the mining process. 

```
public String bytesToHex(byte[] bytes) {
    StringBuilder hexString = new StringBuilder();
    for (byte byt : bytes) {
        hexString.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
    }
    return hexString.toString();
}
```

More examples of encoding can be found in `src/test/java/com/javachain/util/EncodingUtilityTest.java`
&nbsp;&nbsp;&nbsp;&nbsp;  
&nbsp;&nbsp;&nbsp;&nbsp;  
        
**HASHING**    

Hashing is the process of converting given data into usually a shorter hashed value. A hash value is not reversible.  
&nbsp;&nbsp;&nbsp;&nbsp;E.g. of hash algorithms: sha-1, md5 , sha-256 (used in BitCoin), SHA-3/Keccak, etc.  
&nbsp;&nbsp;&nbsp;&nbsp;Practical e.g.: if we run sha-256 on a string "this is secret" it will produce output "37dca33640e56a775653bb63381c114c9790898858036674a25bdf4a34c98ca4"
If somebody messes up with that data, then the hash value will be different (for example, the hash is used when we download new software distributions. We run hash on that downloaded package and check it against the given hash. If hashes are the same, then we know that nobody did meddle with that package).
             
In the JavaChain project, the hashing method is located in HashingUtility class, and it is triggered when we generate a new block. 

```
public byte[] sha256(String message) {
    MessageDigest messageDigest = null;
    try {
        messageDigest = MessageDigest.getInstance(CRYPTO_HASH_ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
        LOGGER.error("Can't digest the message. {}", e.getMessage(), e);
    }
    return Objects.requireNonNull(messageDigest).digest(message.getBytes(StandardCharsets.UTF_8));
}
```   

More examples of hashing can be found in `src/test/java/com/javachain/util/HashingUtilityTest.java`  
&nbsp;&nbsp;&nbsp;&nbsp;  
&nbsp;&nbsp;&nbsp;&nbsp;
        
**ENCRYPTION**   

Encryption is the process of transforming data into a secure unreadable format.   
Symmetric encryption uses a secret key (all parties/users who are supposed to see this data should know this key).  
&nbsp;&nbsp;&nbsp;&nbsp;e.g of encryption algorithms. Blowfish, AES ...  
Asymmetric Encryption uses public / private keys to encrypt and decrypt data
&nbsp;&nbsp;&nbsp;&nbsp;E.g. of encryption algorithms: RSA, DSA ...  
&nbsp;&nbsp;&nbsp;&nbsp;Practical e.g. of RSA encryption:  
         1. U1 initiates a transaction  
         2. U1 encrypts it using U1's PRIVATE key  
         3. U1 encrypts it again using U2's PUBLIC key  
         4. U1 sends the transaction to U2.    
         5. U2 receives a transaction from U1   
         6. U2 decrypts it using U2's PRIVATE key  
         7. U2 decrypts it again using U1's PUBLIC key  
         8. U2 can see the transaction details.    
         
In the JavaChain project, encryption happens when we do a transaction between wallets and is done using EncryptionUtility class methods. 

```
public String sign(String message, PrivateKey privateKey) {
    Signature privateSignature;//hash the data (SHA256) and encrypt it (RSA)
    byte[] signature = new byte[0];
    try {
        privateSignature = Signature.getInstance(SHA_256_WITH_RSA);
        privateSignature.initSign(privateKey);
        privateSignature.update(message.getBytes(StandardCharsets.UTF_8));

        signature = privateSignature.sign();
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
        LOGGER.error("Can't sign the message {}", e.getMessage(), e);
    }
    return Base64.getEncoder().encodeToString(signature);
}
```  

During the process of mining, we need to validate the transaction and verify the digital signature. 
```
public boolean verifySignature(String signer, String signature, PublicKey publicKey) throws SignatureException {
    Signature publicSignature = null;//hash the data (SHA256) and encrypt it (RSA)
    byte[] signatureBytes = new byte[0];
    try {
        publicSignature = Signature.getInstance(SHA_256_WITH_RSA);
        publicSignature.initVerify(publicKey);
        publicSignature.update(signer.getBytes(StandardCharsets.UTF_8));
        signatureBytes = Base64.getDecoder().decode(signature);
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
        LOGGER.error("Can't verify signature: {}", e.getMessage(), e);
    }
    return Objects.requireNonNull(publicSignature).verify(signatureBytes);
}
```  

More examples of encryption can be found in `src/test/java/com/javachain/util/EncryptionUtilityTest.java`
&nbsp;&nbsp;&nbsp;&nbsp;  
&nbsp;&nbsp;&nbsp;&nbsp;

**PRIVATE KEY**      
Used in asymmetric encryption, is a secret string of randomly generated alphanumerical characters, known only to the person that generated it. 
&nbsp;&nbsp;&nbsp;&nbsp;  
&nbsp;&nbsp;&nbsp;&nbsp;

**PUBLIC KEY**       
A string that corresponds to a private key, but does not need to be kept secret. 
&nbsp;&nbsp;&nbsp;&nbsp;  
&nbsp;&nbsp;&nbsp;&nbsp;

**DIGITAL SIGNATURE**       
A number that proves that a signing operation took place. 
A signature is mathematically generated from a hash of something to be signed, plus a private key.  

E.g.:  
    1. U1 signs a message with its private key.   
    2. U1 appends to the original message a version encrypted message  
    3. U2 receives both the message and signature    
    4. U2 uses U1's public key to verify the authenticity of the message  
    
## CRYPTOGRAPHY IN BLOCKCHAIN
Symmetric encryption is used to converted data to a form that cannot be understood by anyone who does not possess the secret key to decrypt it.  
E.g., a bitcoin wallet is encrypted with symmetric AES-256-CBC encryption.

Each new blockchain transaction is recorded and verified onto a new block of data in the blockchain. 
Each block in the chain includes a digital signature linking it to the previous block.  

E.g. (b = block; prk = PRIVATE KEY; puk = PUBLIC KEY; s = SIGNATURE)
1. 1st transaction ( b0 + prk1 => HASHED => s0 )
2. mining => transaction gets approved /  (puk1 + s0)
3. block gets synced between the wallets
4. 2nd transaction. b1 + pk2 => hashed => s1 
...


## BUILDING BLOCKS OF BLOCKCHAIN

**CANONICAL SERIALIZATION**  
Everything that gets hashed or signed is now produced by `CanonicalSerializer` instead of
`Object::toString()`. The canonical form is deterministic and JVM-independent: keys are the
hex of their X.509 encoding, amounts are normalized decimals, transaction inputs reference
the parent transaction by its id (no nested object graphs), and blocks commit to their
ancestor's hash, their timestamp, miner address and the list of transaction ids.

This fixes a fundamental flaw of the first version: hashing/signing used to include JVM
identity hash codes (via `Wallet`, which had no `toString()`), so hashes and signatures
could not be reproduced after a restart or on another machine - making persistence and
network verification impossible by design. Blocks now also carry an explicit
`previousHash` value (the real chain link) and a committed timestamp; both were missing
from the mined message before.

The fee is deliberately excluded from the signed payload until fees are actually
implemented (`computeTotalFee` currently mutates the field as a side effect).

## VERIFICATION AND CONSENSUS

`BlockService.verifyBlock()` walks a chain from its tip to genesis and enforces, per
block:

1. **Real proof-of-work**: the stored hash must equal `sha256(canonical payload +
   "|nonce=" + nonce)`. Earlier versions only checked the leading-digits prefix, so any
   fabricated hash string passed as valid work.
2. **Minting rules (coinbase)**: exactly one coinbase, at index 0, with no inputs,
   paying exactly the `Consensus.BLOCK_INCENTIVE` to the miner, signed by the miner.
   The old blanket "initial transaction -> valid" bypass allowed unlimited minting.
3. **Ownership**: a spending transaction must reference resolvable outputs that all
   belong to one wallet, and its signature must verify against exactly that owner's key.
   No silent skips remain: an unresolvable input invalidates the transaction. Inputs
   also record the exact output index they spend (hardcoded index 0 used to make
   wallets spend other people's outputs).
4. **Double spends**: spent outputs are tracked as `parentTxId:outputIndex` references
   across the whole chain walk; duplicates - including the same transaction included
   twice - are rejected.
5. **Fork choice**: a candidate chain replaces the local chain only if it is strictly
   longer (or is literally the same tip - idempotent re-sync). Equal-height forks are
   rejected instead of silently overwriting local state.

`WeaknessesIT` and `ReviewFindingsIT` pin each of these rules as regression tests: every
attack from the original review now fails validation.

## PERSISTENCE AND CROSS-PROCESS VERIFICATION

Because hashing and signing are now fully canonical (no JVM identity hash codes), a
chain can finally leave the process where it was born:

- `FileChainStore` saves a chain tip (the whole chain hangs off it) or an entire wallet
  to disk; all DTOs declare explicit `serialVersionUID`s.
- `com.javachain.tools.VerifyChainFile` is a standalone verifier with NO Spring
  context - services are wired by hand - that loads a chain file, re-validates every
  rule from scratch, prints `CHAIN_VALID=true/false` and exits accordingly.
- `PersistenceIT` proves both directions end to end: a chain built inside the test JVM
  is accepted (`CHAIN_VALID=true`, exit 0) by a freshly spawned `java` process, and a
  file whose genesis nonce was rewritten without redoing the work is rejected with
  "hash does not match recomputed proof-of-work".
- The interactive shell has a matching command: `verify-file <path>`.

This closes the loop on what made v1 a toy: before, nothing serialized could verify
again after a restart, so persistence and network sync were impossible BY CONSTRUCTION.

## NETWORK SYNC: TWO PEERS, ONE TRUTH

`com.javachain.tools.ChainNode` turns the one-shot verifier into a running peer. The
"network" is a shared directory; each peer owns `<name>.wallet`, publishes
`<name>.chain` atomically (write-temp-then-rename, so nobody ever reads a half-written
chain) and consumes a `<name>.mempool` file of transactions to include in its next
block.

Every round a node:

1. **pulls** every foreign `.chain` file, validates it completely and adopts it only
   if strictly longer - identical tips are idempotent no-ops, equal-height forks are
   ignored;
2. **mines** one block on its tip, including any mempool transaction that does not
   double-spend something already confirmed in its current view;
3. **publishes** the new tip.

`PeerSyncIT` runs this for real: two independent JVM processes start from the same
genesis, each handed a DIFFERENT leg of a double spend. They mine conflicting blocks,
fight the fork - and converge through the consensus rules alone. The test then reads
the published files and asserts: every published chain verifies from scratch, the
peers agree on history (the shorter chain is an exact prefix of the longer one),
exactly one leg of the double spend survived anywhere (the other was orphaned with its
fork), and the spender's coins were consumed exactly once network-wide.

## FEES AND AN HONEST SEND API

`TransactionService.send()` used to take the transfer amount from the RECEIVING
wallet's `amountToBeSent` field - and nothing checked that outputs stayed below
inputs, so a hand-built transaction could mint money by overspending. Both are fixed:

- `send(sender, isInitial, fee, OutgoingTransaction...)` - recipients and amounts are
  explicit; the sender's unspent outputs fund the transfer, and any surplus returns to
  the sender as a trailing change output. Value is conserved exactly:
  inputs = outputs + fee. Insufficient funds throw instead of silently destroying or
  creating coins.
- Validation DERIVES the fee (inputs minus outputs) on every check - it is
  deliberately not part of the canonical payload, so there is nothing mutable to
  sneak past a signature (the old `computeTotalFee` mutated fees after signing,
  which is why fees had to stay out of the payload).
- A spend transaction whose outputs exceed its owned inputs is rejected even when
  correctly signed - closing the value-creation hole.
- The coinbase pays exactly `BLOCK_INCENTIVE` plus the fees of its sibling
  transactions. Standalone checks can only reject underpayment (which would burn
  money); the exact payout is enforced by `verifyBlock()` once the sibling fees are
  known. A miner paying itself one extra coin fails verification.

`FeesIT` pins each rule: fee collection by the miner, insufficient-funds rejection,
overspend rejection, and the greedy-coinbase rejection.

**WALLET**  
A cryptocurrency wallet is the main "store" of blocks and credentials linked to users. 
The keys linked in the wallet are used to encrypt/decrypt and track ownership of transactions.
Each wallet communicates with another through its protocol  (this is mocked in the JavaChain project).

In JavaChain, each wallet contains: 
- public / private key and signature used for encryption,
- signer/owner used mainly for human readability
- and a blockchain where the transactional data is stored 

**BLOCK**  
Blocks are encapsulations of transactional data.
Miners are continually processing new transactions into new blocks added to the block chain's end.  
Each block is a growing chain of blocks that are linked using cryptography.
Once a block is mined/approved/"nonce solved" it becomes immutable and cannot be deleted or modified.  
In JavaChain, each block contains information about: 
 - list of transactions,
 - the nonce is the number that blockchain miners are trying to solve (proof-of-work)
 - the hash (fixed-length presentation of the block data),
 - address of a miner,
 - creation date.

**TRANSACTION**  
A transaction is a transfer value that is broadcasted to the network and collected into blocks.
In JavaChain transaction contains:
 - list of incoming and outgoing sub-transactions,
    - List of InTransactions (that is constructed on InTransaction and OutputIndex of previous OutTransaction list 
    (they have to match)) // t5.setInTransactions(Arrays.asList(new InTransaction(t2, 2), new InTransaction(t4, 0))); 
    - List of Out transactions (money sent out of your wallet)
 - fee (miner gets once it is approved),
 - a digital signature,
 - digital signature,
 - the amount we are sending (temporary storage),
 - wallet related fields - like keys, sender address ...
 

**MINING**  
Mining is a kind of a core process in the blockchain. It is a process of validating transactions and adding it to the blockchain and solving a "cryptographic puzzle."  
To mine a block, miners need to find a nonce.  

In Javachain mining, the method is located in BlockService. 
```
public Block mineBlock(Wallet wallet, List<Transaction> transactions, Block previousBlock)
            throws SignatureException {
...
```

Here we validate the transactions, previous blocks and mine the nonce.
A nonce is a dynamic number that is used in the blockchain "proof of work" algorithm. 
It is a number added to a hashed or encrypted block and is becoming more significant with each block 
(the computational power needed to mine the block is becoming more and more complicated).

The best example to see how the whole blockchain works it is in (`src/test/java/com/javachain/JcApplicationIT.java`)

```
LOGGER.info("Wallets initialization started");
initializeWallets();
```
Here we can imagine that at the begging, three users download the wallet. 
```
assertEquals(new BigDecimal(0), blockService.computeBalance(patriksWallet));
assertEquals(new BigDecimal(0), blockService.computeBalance(donnasWallet));
assertEquals(new BigDecimal(0), blockService.computeBalance(johnsWallet));
```
1st user creates/mines a genesis block and is awarded 25 tokens. 
```
LOGGER.info("Creating a new initial / genesis block");
Block genesisBlock = blockService.mineBlock(patriksWallet, new ArrayList<>(), null);
```
Wallets are synced (this is supposed to be a continuous background job inside of the wallet).
```
patriksWallet = walletService.syncBlockchain(patriksWallet, genesisBlock);
johnsWallet = walletService.syncBlockchain(johnsWallet, genesisBlock);
donnasWallet = walletService.syncBlockchain(donnasWallet, genesisBlock);

LOGGER.info("Block (in this case just initial block) is transferred to other wallets (each wallet does that)");
assertEquals(new BigDecimal(25), blockService.computeBalance(patriksWallet));
assertEquals(new BigDecimal(0), blockService.computeBalance(donnasWallet));
assertEquals(new BigDecimal(0), blockService.computeBalance(johnsWallet));
```
After the user has tokens, they can start doing transactions with each other. In this case, Patrik sent John and Donna 5 tokens.
```
LOGGER.info("5 tokens is being set to be sent to Johns and Donnas wallet (patriks wallet UI)");
donnasWallet.setAmountToBeSent(new BigDecimal(5));
johnsWallet.setAmountToBeSent(new BigDecimal(5));

LOGGER.info("Create first real transaction - send 5 tokens to Donna and 5 to John (patriks wallet UI)");
t2 = transactionService.send(patriksWallet, false, donnasWallet, johnsWallet);
```
Transactions need to be validated, and when a miner does that, the balances change in user wallets. Here John validates the transactions 
(is awarded 25 tokens) and receives the token from Patrik.
```
Block b1 = blockService.mineBlock(johnsWallet, Collections.singletonList(t2), genesisBlock);
LOGGER.info("Miner John approves the transaction (this could be any miner - first one wins the fee)");

patriksWallet = walletService.syncBlockchain(patriksWallet, b1);
johnsWallet = walletService.syncBlockchain(johnsWallet, b1);
donnasWallet = walletService.syncBlockchain(donnasWallet, b1);
assertEquals(new BigDecimal(15), blockService.computeBalance(patriksWallet));
assertEquals(new BigDecimal(5), blockService.computeBalance(donnasWallet));
assertEquals(new BigDecimal(30), blockService.computeBalance(johnsWallet));
LOGGER.info("Once the transaction is approved the wallets need to be synced again (syncing is ongoing/looping process)");

```

That is roughly the whole end to end "happy path" scenario (there are additional scenarios located in BlockServiceIT and WeaknessesIT). 


&nbsp;&nbsp;&nbsp;&nbsp;  
&nbsp;&nbsp;&nbsp;&nbsp;  

**Conclusion:** 
      
I hope this gives you a better overview of how the blockchain works and the concepts behind it.   
While blockchain could be a future alternative to current transaction banking systems, certain issues could be improved in current blockchains (i.e., transactional speed and scalability issues).
Another vulnerability is binding to a particular hashing algorithm. 
While that is not an issue at present, it could become an issue in the future. With more powerful computing 
(like quantum computing) also, modern algorithms could be at risk.   


**Links and useful resources**  
https://en.wikipedia.org/wiki/Cryptocurrency
https://bitcoin.org/bitcoin.pdf
http://www.michaelnielsen.org/ddi/how-the-bitcoin-protocol-actually-works/
https://en.bitcoin.it/wiki/How_bitcoin_works
https://quizlet.com/de/412572806/blockchain-bitcoin-transactions-flash-cards/

Powered By: 

[(http://mubigo.com/mubigo-logo.png)](http://mubigo.com)
