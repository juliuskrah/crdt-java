package com.juliuskrah;

import java.util.UUID;
import java.util.function.BiFunction;

import org.reactivestreams.Publisher;

import io.vavr.control.Option;

/**
 * An implementation of this interface is provided {@link InMemoryCRDTStore}
 * that simulates nodes running on different JVMs accross different machines
 * 
 * Each node of a distributed application contains a CrdtStore that manages the
 * CRDTs. It offers functionality to add CRDTs and find CRDTs that were added by
 * other nodes. To be able to identify CRDTs, they have to be created with a
 * unique ID. If no ID is provided, a random UUID is used.
 * 
 * @author Julius Krah
 */
public interface CRDTStore extends Publisher<CRDTDefinition> {
    /**
     * Registers a CRDT data type in the factory
     * 
     * @param <T>       type of CRDT
     * @param crdtClass CRDT class
     * @param mapper    function to compute CRDT from crdt ID and node ID
     */
    <T extends CRDT<? extends CRDTCommand>> void registerFactory(Class<T> crdtClass,
            BiFunction<String, String, T> mapper);

    /**
     * Fetches CRDT by ID
     * 
     * @param crdtId the ID
     * @return CRDT if available
     */
    Option<? extends CRDT<? extends CRDTCommand>> findCrdt(String crdtId);

    /**
     * Retrieves an RSA from the replica
     * 
     * @param <E>    element
     * @param crtdId the ID
     * @return RGA if available
     */
    @SuppressWarnings("unchecked")
    default <E> Option<RGA<E>> findRGA(String crtdId) {
        final Option<? extends CRDT<? extends CRDTCommand>> option = findCrdt(crtdId);
        return option.flatMap(crtd -> crtd instanceof RGA ? Option.of((RGA<E>) crtd) : Option.none());
    }

    /**
     * Retrieves an LWWRegister from the replica
     * 
     * @param <T>    element
     * @param crtdId the ID
     * @return LWWRegister if available
     */
    @SuppressWarnings("unchecked")
    default <T> Option<LWWRegister<T>> findLWWRegister(String crtdId) {
        final Option<? extends CRDT<? extends CRDTCommand>> option = findCrdt(crtdId);
        return option.flatMap(crtd -> crtd instanceof LWWRegister ? Option.of((LWWRegister<T>) crtd) : Option.none());
    }

    /**
     * Retrieves an LWWElementSet from the replica
     * 
     * @param <E>    element
     * @param crtdId the ID
     * @return LWWElementSet if available
     */
    @SuppressWarnings("unchecked")
    default <E> Option<LWWElementSet<E>> findLWWElementSet(String crtdId) {
        final Option<? extends CRDT<? extends CRDTCommand>> option = findCrdt(crtdId);
        return option
                .flatMap(crtd -> crtd instanceof LWWElementSet ? Option.of((LWWElementSet<E>) crtd) : Option.none());
    }

    /**
     * Retrieves an LWWElementGraph from the replica
     * 
     * @param <T>    element
     * @param crtdId the ID
     * @return LWWElementGraph if available
     */
    @SuppressWarnings("unchecked")
    default <T> Option<LWWElementGraph<T>> findLWWElementGraph(String crtdId) {
        final Option<? extends CRDT<? extends CRDTCommand>> option = findCrdt(crtdId);
        return option.flatMap(
                crtd -> crtd instanceof LWWElementGraph ? Option.of((LWWElementGraph<T>) crtd) : Option.none());
    }

    /**
     * Creates a new CRDT. An identifier is computed
     * 
     * @param <T>       type of CRDT
     * @param crdtClass CRDT class
     * @return CRDT
     */
    default <T extends CRDT<? extends CRDTCommand>> T createCrdt(Class<T> crdtClass) {
        return createCrdt(crdtClass, UUID.randomUUID().toString());
    }

    /**
     * Create a new CRDT using the provided identifier
     * 
     * @param <T>
     * @param crdtClass CRDT class
     * @param crdtId    the identifier
     * @return CRDT
     */
    <T extends CRDT<? extends CRDTCommand>> T createCrdt(Class<T> crdtClass, String crdtId);

    /**
     * Creates a new RGA. An identifier is computed
     * 
     * @param <E> element type
     * @return RGA
     */
    default <E> RGA<E> createRGA() {
        return createRGA(UUID.randomUUID().toString());
    }

    /**
     * Creates a new RGA using the provided identifier
     * 
     * @param <E>    element type
     * @param crdtId the identifier
     * @return RGA
     */
    @SuppressWarnings("unchecked")
    default <E> RGA<E> createRGA(String crdtId) {
        return createCrdt(RGA.class, crdtId);
    }

    /**
     * Creates a new LWWRegister. An identifier is computed
     * 
     * @param <T> element type
     * @return LWWRegister
     */
    default <T> LWWRegister<T> createLWWRegister() {
        return createLWWRegister(UUID.randomUUID().toString());
    }

    /**
     * Creates a new LWWRegister using the provided identifier
     * 
     * @param <T>    element type
     * @param crdtId the identifier
     * @return LWWRegister
     */
    @SuppressWarnings("unchecked")
    default <T> LWWRegister<T> createLWWRegister(String crdtId) {
        return createCrdt(LWWRegister.class, crdtId);
    }

    /**
     * Creates a new LWW-Element-Set. An identifier is computed
     * 
     * @param <E> element type
     * @return LWWElementSet
     */
    default <E> LWWElementSet<E> createLWWElementSet() {
        return createLWWElementSet(UUID.randomUUID().toString());
    }

    /**
     * Creates a new LWW-Element-Set using the provided identifier
     * 
     * @param <E>    element type
     * @param crdtId the identifier
     * @return LWWElementSet
     */
    @SuppressWarnings("unchecked")
    default <E> LWWElementSet<E> createLWWElementSet(String crdtId) {
        return createCrdt(LWWElementSet.class, crdtId);
    }

    /**
     * Creates a new LWW-Element-Set. An identifier is computed
     * 
     * @param <T> element type
     * @return LWWElementSet
     */
    default <T> LWWElementGraph<T> createLWWElementGraph() {
        return createLWWElementGraph(UUID.randomUUID().toString());
    }

    /**
     * Creates a new LWW-Element-Graph using the provided identifier
     * 
     * @param <T>    element type
     * @param crdtId the identifier
     * @return LWWElementGraph
     */
    @SuppressWarnings("unchecked")
    default <T> LWWElementGraph<T> createLWWElementGraph(String crdtId) {
        return createCrdt(LWWElementGraph.class, crdtId);
    }

    /**
     * Copies over the state changes to replicas if connected
     * 
     * @param publisher
     */
    void subscribeTo(Publisher<? extends CRDTDefinition> publisher);

    /**
     * Establish a network connection to a replica
     * 
     * @param other store
     */
    void connect(CRDTStore other);

    /**
     * Disconnect from replica. Called when network connection is lost or split
     * brain
     * 
     * @param other store
     */
    void disconnect(CRDTStore other);
}
