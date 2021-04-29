package com.juliuskrah;

import java.util.Objects;

import io.vavr.control.Option;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

/**
 * Implements a 'Last Writer Wins Register' CRDT, also called a 'LWW-Register'.
 * 
 * It is described in the paper <a href=
 * "https://hal.inria.fr/file/index/docid/555588/filename/techreport.pdf"> A
 * comprehensive study of Convergent and Commutative Replicated Data Types</a>.
 *
 * Merge takes the register with highest timestamp. LWWRegister should only be
 * used when the choice of value is not important for concurrent updates
 * occurring within the clock skew.
 * 
 * @author Julius Krah
 * @implNote We start from less complex implementations to test our
 *           synchronization
 */
public class LWWRegister<T> extends AbstractCRDT<LWWRegister.SetCommand<T>> {
    private T value;
    private VectorClock vectorClock;

    private void doSet(T value) {
        this.value = value;
        vectorClock = vectorClock.increment();
    }

    @Override
    protected Option<? extends SetCommand<T>> processCommand(SetCommand<T> command) {
        if (vectorClock.compareTo(command.getVectorClock()) < 0) {
            vectorClock = vectorClock.merge(command.getVectorClock());
            doSet(command.getValue());
            return Option.of(command);
        }
        return Option.none();
    }

    public LWWRegister(String nodeId, String crdtId) {
        super(nodeId, crdtId, Sinks.many().replay().all());
        this.vectorClock = new VectorClock(nodeId);
    }

    /**
     * Sets the new value
     * @param newValue
     */
    public void set(T newValue) {
        if (!Objects.equals(value, newValue)) {
            doSet(newValue);
            commands.emitNext(new SetCommand<>(crdtId, value, vectorClock), EmitFailureHandler.FAIL_FAST);
        }
    }

    /**
     * Gets the current value
     * @return
     */
    public T get() {
        return value;
    }

    public static final class SetCommand<T> extends CRDTCommand {

        private final T value;
        private final VectorClock vectorClock;

        SetCommand(String crdtId, T value, VectorClock vectorClock) {
            super(crdtId);
            this.value = value;
            this.vectorClock = Objects.requireNonNull(vectorClock, "vectorClock must not be null");
        }

        T getValue() {
            return value;
        }

        VectorClock getVectorClock() {
            return this.vectorClock;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || getClass() != o.getClass())
                return false;

            SetCommand<?> that = (SetCommand<?>) o;

            return Objects.equals(value, that.value) //
                    && Objects.equals(vectorClock, that.vectorClock);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, vectorClock);
        }
    }
}
