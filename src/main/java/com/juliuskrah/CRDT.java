package com.juliuskrah;

import org.reactivestreams.Publisher;

/**
 * Base interface for all CRDT data types
 * 
 * @author Julius Krah
 * @param <COMMAND> CRDT Command
 */
public interface CRDT<COMMAND extends CRDTCommand> extends Publisher<COMMAND> {
    /**
     * Returns the ID of the datatype. If no ID is available, then one is generated
     * 
     * @return unique ID of the data type
     */
    String getCrdtId();

    /**
     * Copies over the state changes to replicas if connected
     * 
     * @param publisher
     */
    void subscribeTo(Publisher<? extends COMMAND> publisher);
}
