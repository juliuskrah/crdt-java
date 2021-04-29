package com.juliuskrah;

/**
 * A factory class used to create CRDT store instances
 * 
 * @author Julius Krah
 */
public class CRDTStoreFactory {
    private static CRDTStoreFactory INSTANCE;
    private CRDTStoreFactory() {}

    public static CRDTStoreFactory getInstance() {
        if(null == INSTANCE) {
            INSTANCE = new CRDTStoreFactory();
        }
        return INSTANCE;
    }

    /**
     * Creates CRDTStores
     * @return inMemory CRDT store
     */
    public CRDTStore crdtStore() {
        return new InMemoryCRDTStore();
    }

    /**
     * Creates CRDTStores with a node ID
     * @return inMemory CRDT store
     */
    public CRDTStore crdtStore(String nodeId) {
        return new InMemoryCRDTStore(nodeId);
    }
}
