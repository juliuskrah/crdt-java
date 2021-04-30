package com.juliuskrah;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

/**
 * In memory implementation of a CRDT store. Mimics independent replicas
 * 
 * @author Julius Krah
 */
@SuppressWarnings("rawtypes")
public class InMemoryCRDTStore implements CRDTStore {
    private final String nodeId;
    private final Sinks.Many<CRDTDefinition> definitions = Sinks.many().replay().all();
    private Map<CRDTStore, CRDTStoreSubscriber> subscribers = HashMap.empty();

    private Map<String, CRDT<? extends CRDTCommand>> crdts = HashMap.empty();
    private Map<Class<? extends CRDT>, BiFunction<String, String, ? extends CRDT>> factories = HashMap.empty();

    /**
     * Loads all default implementations
     */
    @SuppressWarnings("unchecked")
    private void registerDefaultFactories() {
        registerFactory(LWWRegister.class, LWWRegister::new);
        registerFactory(LWWElementGraph.class, LWWElementGraph::new);
        registerFactory(LWWElementSet.class, LWWElementSet::new);
        // registerFactory(LWWElementSet.class, (nodeId, crdtId) -> new LWWElementSet(nodeId, crdtId, LWWBias.REMOVE));
        registerFactory(RGA.class, RGA::new);
    }

    /**
     * Registers the CRDT implementation InMemory
     * 
     * @param crdt CRDT implementation
     */
    private void register(CRDT<? extends CRDTCommand> crdt) {
        crdts = crdts.put(crdt.getCrdtId(), crdt);
        definitions.emitNext(new CRDTDefinition(crdt.getCrdtId(), crdt.getClass(), crdt), EmitFailureHandler.FAIL_FAST);
    }

    public InMemoryCRDTStore() {
        this(UUID.randomUUID().toString());
    }

    public InMemoryCRDTStore(String nodeId) {
        this.nodeId = nodeId;
        registerDefaultFactories();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe(Subscriber<? super CRDTDefinition> s) {
        definitions.asFlux().subscribe(s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends CRDT<? extends CRDTCommand>> void registerFactory(Class<T> crdtClass, //
            BiFunction<String, String, T> mapper) {
        factories = factories.put(crdtClass, mapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Option<? extends CRDT<? extends CRDTCommand>> findCrdt(String crdtId) {
        return crdts.get(crdtId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends CRDT<? extends CRDTCommand>> T createCrdt(Class<T> crdtClass, String crdtId) {
        Objects.requireNonNull(crdtClass, "crdtClass must not be null");
        Objects.requireNonNull(crdtId, "crdtId must not be null");
        final Option<BiFunction<String, String, ? extends CRDT>> factory = factories.get(crdtClass);
        if (factory.isEmpty()) {
            throw new IllegalArgumentException("Factory for class " + crdtClass + " not defined");
        }
        @SuppressWarnings("unchecked")
        final T result = (T) factory.get().apply(nodeId, crdtId);
        register(result);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribeTo(Publisher<? extends CRDTDefinition> publisher) {
        Flux.from(publisher).onTerminateDetach().subscribe(new CRDTStoreSubscriber());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect(CRDTStore other) {
        if (!subscribers.containsKey(other)) {
            final CRDTStoreSubscriber subscriber = new CRDTStoreSubscriber();
            other.subscribe(subscriber);
            subscribers = subscribers.put(other, subscriber);
            other.connect(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect(CRDTStore other) {
        subscribers.get(other).peek(subscriber -> {
            subscriber.dispose();
            subscribers = subscribers.remove(other);
            other.disconnect(this);
        });
    }

    /**
     * Subscriber implementation
     */
    protected class CRDTStoreSubscriber extends BaseSubscriber<CRDTDefinition> {
        private final Sinks.Many<Boolean> cancelProcessor = Sinks.many().replay().latest();

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public void hookOnNext(CRDTDefinition definition) {
            final String crdtId = definition.getCrdtId();
            final Flux<? extends CRDTCommand> publisher = Flux.from(definition.getPublisher())
                    .takeUntilOther(cancelProcessor.asFlux());
            final Option<? extends CRDT> existingCrdt = findCrdt(crdtId);
            if (existingCrdt.isDefined()) {
                existingCrdt.get().subscribeTo(publisher);
            } else {
                final Class<? extends CRDT> crdtClass = definition.getCrdtClass();
                factories.get(crdtClass).map(factory -> factory.apply(nodeId, crdtId))
                        .peek(InMemoryCRDTStore.this::register).peek(crdt -> crdt.subscribeTo(publisher));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void hookOnError(Throwable throwable) {
            cancel();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void hookOnComplete() {
            cancel();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose() {
            cancelProcessor.emitNext(true, EmitFailureHandler.FAIL_FAST);
            cancelProcessor.emitComplete(EmitFailureHandler.FAIL_FAST);
        }
    }
}
