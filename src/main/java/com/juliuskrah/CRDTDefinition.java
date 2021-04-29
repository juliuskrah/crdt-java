package com.juliuskrah;

import org.reactivestreams.Publisher;

/**
 * Definitions required to send messages asynchronously between replicas
 * 
 * @author Julius Krah
 */
public final class CRDTDefinition {
    private final String crdtId;
    @SuppressWarnings("rawtypes")
    private final Class<? extends CRDT> crdtClass;
    /**
     * A publisher sends events to replicas and holds a buffer of unconsumed events
     */
    private final Publisher<? extends CRDTCommand> publisher;

    @SuppressWarnings("rawtypes")
    CRDTDefinition(String crdtId, Class<? extends CRDT> crdtClass,
            Publisher<? extends CRDTCommand> publisher) {
        this.crdtId = crdtId;
        this.crdtClass = crdtClass;
        this.publisher = publisher;
    }

    public String getCrdtId() {
        return crdtId;
    }

    @SuppressWarnings("rawtypes")
    public Class<? extends CRDT> getCrdtClass() {
        return crdtClass;
    }

    public Publisher<? extends CRDTCommand> getPublisher() {
        return publisher;
    }
}
