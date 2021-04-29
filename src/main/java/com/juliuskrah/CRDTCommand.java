package com.juliuskrah;

import java.util.Objects;

/**
 * A command represents the data that is sent when replicas are in-sync. After a
 * network partition, the same command sends replay events when the node was disconnected
 * from the cluster
 * 
 * @author Julius Krah
 */
public abstract class CRDTCommand {
    private final String crdtId;

    public CRDTCommand(String crdtId) {
        this.crdtId = Objects.requireNonNull(crdtId, "crdtId must not be null");
    }

    public String getCrdtId() {
        return crdtId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        CRDTCommand that = (CRDTCommand) o;

        return Objects.equals(crdtId, that.crdtId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(crdtId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toString(crdtId);
    }
}
