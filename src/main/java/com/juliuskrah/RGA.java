package com.juliuskrah;

import java.util.AbstractList;
import java.util.Objects;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

/**
 * Replicated Growable Array
 * 
 * @see {@link https://replicated.cc/rdts/rga/}
 * @see {@link https://bartoszsypytkowski.com/operation-based-crdts-arrays-1/}
 * @author Julius Krah
 * @implNote We start from less complex implementations to test our synchronization
 */
public class RGA<E> extends AbstractList<E> implements CRDT<RGA.RGACommand<E>> {
    private final String crdtId;
    private final Sinks.Many<RGACommand<E>> commands = Sinks.many().replay().all();
    private final Vertex<E> start;

    private Map<VectorClock, Vertex<E>> vertices;
    private Map<Vertex<E>, Vertex<E>> edges = HashMap.empty();
    private VectorClock clock;
    private int size;

    /**
     * Add items to the right
     * @param left
     * @param value
     * @param clock
     */
    private void doAddRight(Vertex<E> left, E value, VectorClock clock) {
        Option<Vertex<E>> right = edges.get(left);
        while (right.isDefined() && (clock.compareTo(right.get().getVectorClock()) < 0)) {
            left = right.get();
            right = edges.get(left);
        }
        final Vertex<E> w = new Vertex<>(value, clock);
        vertices = vertices.put(clock, w);
        size++;
        edges = edges.put(left, w);
        if (right.isDefined()) {
            edges = edges.put(w, right.get());
        }
    }

    private Option<RGACommand<E>> processCommand(RGACommand<E> command) {
        if (command instanceof AddRightCommand) {
            final AddRightCommand<E> addRightCommand = (AddRightCommand<E>) command;
            if (findVertex(addRightCommand.newVertexClock).isEmpty()) {
                final Option<Vertex<E>> anchor = findVertex(addRightCommand.anchorClock);
                clock = clock.merge(addRightCommand.newVertexClock);
                anchor.peek(
                        vertex -> doAddRight(vertex, addRightCommand.newVertexValue, //
                            addRightCommand.newVertexClock)
                );
                return Option.of(command);
            }
        } else if (command instanceof RemoveCommand) {
            final VectorClock removedClock = ((RemoveCommand<E>) command).vectorClock;
            final Option<Vertex<E>> vertex = findVertex(removedClock);
            return vertex.map(this::doRemove).flatMap(result -> result? Option.of(command) : Option.none());
        }

        return Option.none();
    }

    /**
     * Find vertex by index
     * 
     * @param index
     * @return
     */
    private Vertex<E> findVertex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        Vertex<E> vertex = start;
        for (int i = 0; i <= index; i++) {
            do { // TODO check and fix loop
                vertex = edges.get(vertex).get();
            } while (vertex.isRemoved());
        }
        return vertex;
    }

    /**
     * Find vertex by vectorClock
     * 
     * @param vectorClock
     * @return
     */
    private Option<Vertex<E>> findVertex(VectorClock vectorClock) {
        return vertices.get(vectorClock);
    }

    private void prepareRemove(Vertex<E> vertex) {
        commands.emitNext(new RemoveCommand<>(crdtId, vertex.getVectorClock()), EmitFailureHandler.FAIL_FAST);
        doRemove(vertex);
    }

    private boolean doRemove(Vertex<E> vertex) {
        if (! vertex.isRemoved()) {
            vertex.setRemoved(true);
            size--;
            return true;
        }
        return false;
    }

    private void prepareAddRight(Vertex<E> anchor, E value) {
        clock = clock.increment();
        doAddRight(anchor, value, clock);
        commands.emitNext(new AddRightCommand<>(crdtId, anchor.getVectorClock(), value, clock), //
            EmitFailureHandler.FAIL_FAST);
    }

    public RGA(String nodeId, String crdtId) {
        this.crdtId = Objects.requireNonNull(crdtId, "crtdId must not be null");

        Objects.requireNonNull(nodeId, "nodeId must not be null");
        this.clock = new VectorClock(nodeId);
        this.start = new Vertex<>(null, clock);
        this.vertices = HashMap.of(clock, start);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe(Subscriber<? super RGACommand<E>> s) {
        commands.asFlux().subscribe(s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCrdtId() {
        return this.crdtId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribeTo(Publisher<? extends RGACommand<E>> publisher) {
        Flux.from(publisher).onTerminateDetach().subscribe(command -> {
            final Option<RGACommand<E>> newCommand = processCommand(command);
            newCommand.peek(commands::tryEmitNext);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public E get(int index) {
        return findVertex(index).getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(int index, E element) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException();
        }
        final Vertex<E> anchor = index == 0 ? start : findVertex(index - 1);
        prepareAddRight(anchor, element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public E remove(int index) {
        final Vertex<E> vertex = findVertex(index);
        prepareRemove(vertex);
        return vertex.getValue();
    }

    public abstract static class RGACommand<E> extends CRDTCommand {
        protected RGACommand(String crdtId) {
            super(crdtId);
        }
    }

    public static final class RemoveCommand<E> extends RGACommand<E> {

        private final VectorClock vectorClock;

        private RemoveCommand(String crdtId, VectorClock vectorClock) {
            super(crdtId);
            this.vectorClock = vectorClock;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            @SuppressWarnings("unchecked")
            RemoveCommand<E> that = (RemoveCommand<E>) o;

            return Objects.equals(vectorClock, that.vectorClock);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vectorClock);
        }
    }

    public static final class AddRightCommand<E> extends RGACommand<E> {

        private final VectorClock anchorClock;
        private final E newVertexValue;
        private final VectorClock newVertexClock;

        private AddRightCommand(String crdtId, VectorClock anchorClock, E newVertexValue, VectorClock newVertexClock) {
            super(crdtId);
            this.anchorClock = Objects.requireNonNull(anchorClock, "anchorClock must not be null");
            this.newVertexValue = Objects.requireNonNull(newVertexValue, "newVertexValue must not be null");
            this.newVertexClock = Objects.requireNonNull(newVertexClock, "newVertexClock must not be null");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            AddRightCommand<?> that = (AddRightCommand<?>) o;

            return Objects.equals(anchorClock, that.anchorClock) //
                && Objects.equals(newVertexValue, that.newVertexValue) //
                && Objects.equals(newVertexClock, that.newVertexClock);
        }

        @Override
        public int hashCode() {
            return Objects.hash(anchorClock, newVertexValue, newVertexClock);
        }
    }
}
