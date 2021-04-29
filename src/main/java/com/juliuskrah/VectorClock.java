package com.juliuskrah;

import java.util.Objects;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.Set;

/**
 * This implementation of a {@code VectorClock} provides the {@linkplain key} at
 * object creation
 * 
 * {@link https://en.wikipedia.org/wiki/Vector_clock}
 * 
 * @author Julius Krah
 */
public final class VectorClock implements Comparable<VectorClock> {
    private final String key;
    private final Map<String, Long> entries;

    /**
     * Computes the difference between this {@linkplain clock} and
     * {@linkplain other} clock
     * 
     * @param other Vector Clock
     * @return all differences
     */
    private Set<Long> calculateDiffs(VectorClock other) {
        final Set<String> allKeys = entries.keySet().addAll(other.entries.keySet());
        return allKeys.map(key -> entries.get(key).getOrElse(0L) - other.entries.get(key).getOrElse(0L));
    }

    public VectorClock(String key) {
        this(key, HashMap.empty());
    }

    public VectorClock(String key, Map<String, Long> entries) {
        this.key = key;
        this.entries = entries;
    }

    /**
     * increments the logical clock in the vector by one
     * 
     * @return incremented VectorClock
     */
    public VectorClock increment() {
        final long counter = entries.get(key).map(value -> value + 1L).getOrElse(1L);
        return new VectorClock(key, entries.put(key, counter));
    }

    /**
     * Merges two {@code VectorClock}s
     * 
     * @param other another VectorClock
     * @return merged VectorClock
     */
    public VectorClock merge(VectorClock other) {
        return new VectorClock(key, entries.merge(other.entries, Math::max));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(VectorClock other) {
        final Set<Long> diffs = calculateDiffs(other);
        final boolean isGreater = diffs.find(diff -> diff > 0).isDefined();
        final boolean isLess = diffs.find(diff -> diff < 0).isDefined();

        return (isGreater && isLess) ? key.compareTo(other.key)
                : isLess ? -1
                : isGreater ? 1
                : key.compareTo(other.key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toString(entries, "entries");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        VectorClock that = (VectorClock) o;

        return calculateDiffs(that).forAll(diff -> diff == 0);
    }

}
