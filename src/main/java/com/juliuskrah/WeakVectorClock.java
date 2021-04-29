package com.juliuskrah;

import java.util.Objects;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.Set;

/**
 * A vector clock is a data structure used for determining the partial ordering
 * of events in a distributed system and detecting causality violations.
 * 
 * @see {@link https://en.wikipedia.org/wiki/Vector_clock}
 * 
 * @author Julius Krah
 */
public final class WeakVectorClock implements Comparable<WeakVectorClock> {
    private final Map<String, Long> entries;

    /**
     * Computes the difference between this {@linkplain clock} and {@linkplain other} clock
     * @param other Vector Clock
     * @return all differences
     */
    private Set<Long> calculateDiffs(WeakVectorClock other) {
        final Set<String> allKeys = entries.keySet().addAll(other.entries.keySet());
        return allKeys.map(key -> entries.get(key).getOrElse(0L) - other.entries.get(key).getOrElse(0L));
    }

    public WeakVectorClock() {
        this(HashMap.empty());
    }

    public WeakVectorClock(Map<String, Long> entries) {
        this.entries = entries;
    }

    /**
     * increments the logical clock in the vector by one
     * 
     * @param key the key
     * @return incremented VectorClock
     */
    public WeakVectorClock increment(String key) {
        final long counter = entries.get(key).map(value -> value + 1L).getOrElse(1L);
        return new WeakVectorClock(entries.put(key, counter));
    }

    /**
     * Merges two {@code VectorClock}s
     * @param other
     * @return merged VectorClock
     */
    public WeakVectorClock merge(WeakVectorClock other) {
        return new WeakVectorClock(entries.merge(other.entries, Math::max));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(WeakVectorClock other) {
        final Set<Long> diffs = calculateDiffs(other);
        final boolean isGreater = diffs.find(diff -> diff > 0).isDefined();
        final boolean isLess = diffs.find(diff -> diff < 0).isDefined();

        return (isGreater && isLess) ? 0 : isLess ? -1 : isGreater ? 1 : 0;
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
    public boolean equals(Object other) {
        if (this == other) return true;

        if (other == null || getClass() != other.getClass()) return false;

        WeakVectorClock that = (WeakVectorClock) other;

        return calculateDiffs(that).forAll(diff -> diff == 0);
    }
}
