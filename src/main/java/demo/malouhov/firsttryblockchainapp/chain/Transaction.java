package demo.malouhov.firsttryblockchainapp.chain;

import demo.malouhov.firsttryblockchainapp.MyFirstBlockchain;
import demo.malouhov.firsttryblockchainapp.util.BlockUtil;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class Transaction {

    private String transactionId; //Contains a hash of transaction*
    private PublicKey sender; //Senders address/public key.
    private PublicKey reciepient; //Recipients address/public key.
    private float value; //Contains the amount we wish to send to the recipient.
    private byte[] signature; //This is to prevent anybody else from spending funds in our wallet.
    private ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
    private ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

    private static int sequence = 0; //A rough count of how many transactions have been generated

    // Constructor:
    public Transaction(PublicKey from, PublicKey to, float value,  ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.reciepient = to;
        this.value = value;
        this.inputs = inputs;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public PublicKey getSender() {
        return sender;
    }

    public PublicKey getReciepient() {
        return reciepient;
    }

    public float getValue() {
        return value;
    }

    public ArrayList<TransactionInput> getInputs() {
        return inputs;
    }

    public ArrayList<TransactionOutput> getOutputs() {
        return outputs;
    }

    public boolean processTransaction() {

        if(!verifySignature()) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }

        //Gathers transaction inputs (Making sure they are unspent):
        for(TransactionInput i : inputs) {
            i.setUTXO(MyFirstBlockchain.UTXOs.get(i.getTransactionOutputId()));
        }

        //Checks if transaction is valid:
        if(getInputsValue() < MyFirstBlockchain.minimumTransaction) {
            System.out.println("Transaction Inputs too small: " + getInputsValue());
            System.out.println("Please enter the amount greater than " + MyFirstBlockchain.minimumTransaction);
            return false;
        }

        //Generate transaction outputs:
        float leftOver = getInputsValue() - value; //get value of inputs then the left over change:
        transactionId = calulateHash();
        outputs.add(new TransactionOutput( this.reciepient, value,transactionId)); //send value to recipient
        outputs.add(new TransactionOutput( this.sender, leftOver,transactionId)); //send the left over 'change' back to sender

        //Add outputs to Unspent list
        for(TransactionOutput o : outputs) {
            MyFirstBlockchain.UTXOs.put(o.getId() , o);
        }

        //Remove transaction inputs from UTXO lists as spent:
        for(TransactionInput i : inputs) {
            if(i.getUTXO() == null) continue; //if Transaction can't be found skip it
            MyFirstBlockchain.UTXOs.remove(i.getUTXO().getId());
        }

        return true;
    }

    public float getInputsValue() {
        float total = 0;
        for(TransactionInput i : inputs) {
            if(i.getUTXO() == null) continue; //if Transaction can't be found skip it, This behavior may not be optimal.
            total += i.getUTXO().getValue();
        }
        return total;
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = BlockUtil.getStringFromKey(sender) + BlockUtil.getStringFromKey(reciepient) + Float.toString(value)	;
        signature = BlockUtil.applyECDSASig(privateKey,data);
    }

    public boolean verifySignature() {
        String data = BlockUtil.getStringFromKey(sender) + BlockUtil.getStringFromKey(reciepient) + Float.toString(value)	;
        return BlockUtil.verifyECDSASig(sender, data, signature);
    }

    public float getOutputsValue() {
        float total = 0;
        for(TransactionOutput o : outputs) {
            total += o.getValue();
        }
        return total;
    }

    private String calulateHash() {
        sequence++; //increase the sequence to avoid 2 identical transactions having the same hash
        return BlockUtil.applySha256(
                BlockUtil.getStringFromKey(sender) +
                        BlockUtil.getStringFromKey(reciepient) +
                        Float.toString(value) + sequence
        );
    }
}
