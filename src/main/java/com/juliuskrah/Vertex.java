package com.juliuskrah;

import java.util.Objects;

/**
 * @author Julius Krah
 */
public final class Vertex<E> {
    private final E value;
    private final VectorClock vectorClock;

    private boolean removed;

    public Vertex(E value, VectorClock clock) {
        this.value = value;
        this.vectorClock = clock;
    }

    public E getValue() {
        return this.value;
    }

    public VectorClock getVectorClock() {
        return this.vectorClock;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public boolean isRemoved() {
        return this.removed;
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

        var vertex = (Vertex<?>) o;

        return Objects.equals(vectorClock, vertex.vectorClock);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return vectorClock.hashCode();
    }

}
