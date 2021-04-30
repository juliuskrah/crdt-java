package com.juliuskrah;

import java.util.Objects;

import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

/**
 * An Adjacency List Graph for CRDT
 * 
 * @author Julius Krah
 * @see https://www.khanacademy.org/computing/computer-science/algorithms/graph-representation/a/representing-graphs
 */
public class LWWElementGraph<T> extends AbstractCRDT<LWWElementGraph.GraphCommand<T>> {
    private Map<Vertex<T>, List<Vertex<T>>> vertices;
    /**
     * Temporary holder to store vector clocks
     */
    private Map<T, VectorClock> elements;
    private VectorClock vectorClock;

    private void prepareAddVertex(T element) {
        vectorClock = vectorClock.increment();
        doAddVertex(element, vectorClock);
        commands.emitNext(new AddVertexCommand<>(crdtId, element, vectorClock), //
                EmitFailureHandler.FAIL_FAST);
    }

    private void doAddVertex(T element, VectorClock vectorClock) {
        var vertex = new Vertex<>(element, vectorClock);
        elements = elements.put(element, vectorClock);
        vertices = vertices.computeIfAbsent(vertex, v -> List.empty())._2;
    }

    private boolean prepareAddEdge(T element1, T element2) {
        var result = doAddEdge(element1, element2);
        commands.emitNext(new AddEdgeCommand<T>(crdtId, element1, element2), EmitFailureHandler.FAIL_FAST);
        return result;
    }

    private boolean doAddEdge(T element1, T element2) {
        if(!elements.containsKey(element1) && !elements.containsKey(element2)) {
            // one or both vertices do not exist to create an edge
            return false;
        }
        var clock1 = elements.get(element1).get(); // remove element later to avoid OOM
        var clock2 = elements.get(element2).get();
        var vertex1 = new Vertex<T>(element1, clock1);
        var vertex2 = new Vertex<T>(element2, clock2);
        // add vertex2 to vertex1 adjacency list
        vertices = vertices.computeIfPresent(vertex1, (k, v) -> v.append(vertex2))._2;
        // add vertex1 to vertex2 adjacency list
        vertices = vertices.computeIfPresent(vertex2, (k, v) -> v.append(vertex1))._2;
        return true;
    }

    private void prepareRemoveVertex(T element) {
        doRemoveVertex(element);
        commands.emitNext(new RemoveVertexCommand<>(crdtId, element), EmitFailureHandler.FAIL_FAST);
    }

    @SuppressWarnings("deprecation")
    private void doRemoveVertex(T element) {
        // get original clock
        var clock = elements.get(element);
        if (clock.isDefined()) {
            var vertex = new Vertex<>(element, clock.get());
            // remove all adjacent vertices
            vertices.values().forEach(e -> e.remove(vertex));
            vertices = vertices.remove(vertex);
        }
    }

    private void prepareRemoveEdge(T element1, T element2) {
        doRemoveEdge(element1, element2);
        commands.emitNext(new RemoveEdgeCommand<>(crdtId, element1, element2), EmitFailureHandler.FAIL_FAST);
    }

    private void doRemoveEdge(T element1, T element2) {
        if(elements.containsKey(element1) && elements.containsKey(element2)) {
            var clock1 = elements.get(element1).get();
            var clock2 = elements.get(element2).get();
            var vertex1 = new Vertex<T>(element1, clock1);
            var vertex2 = new Vertex<T>(element2, clock2);

            // remove vertex2 from vertex1 adjacency list
            vertices = vertices.computeIfPresent(vertex1, (k, v) -> v.remove(vertex2))._2;
            // remove vertex1 from vertex2 adjacency list
            vertices = vertices.computeIfPresent(vertex2, (k, v) -> v.remove(vertex1))._2;
        }
    }

    @Override
    protected Option<? extends GraphCommand<T>> processCommand(GraphCommand<T> command) {
        return Option.none();
    }

    public LWWElementGraph(String nodeId, String crdtId) {
        super(nodeId, crdtId, Sinks.many().replay().all());
        this.vertices = HashMap.empty();
        this.elements = HashMap.empty();
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        this.vectorClock = new VectorClock(nodeId);
    }

    public void addVertex(T element) {
        prepareAddVertex(element);
    }

    public void removeVertex(T element) {
        prepareRemoveVertex(element);
    }

    public boolean addEdge(T element1, T element2) {
        return prepareAddEdge(element1, element2);
    }

    public void removeEdge(T element1, T element2) {
        prepareRemoveEdge(element1, element2);
    }

    /**
     * Query for all vertices adjacent to current vertex
     * @param element current vertex
     * @return
     */
    List<Vertex<T>> findAdjacentVertices(T element) {
        if(elements.get(element).isDefined()) {
            var vertex = new Vertex<>(element, elements.get(element).get());
            return vertices.get(vertex).get();
        }
        return List.empty();
    }

    public int vertexSize() {
        return this.vertices.size();
    }

    public static class GraphCommand<T> extends CRDTCommand {

        public GraphCommand(String crdtId) {
            super(crdtId);
        }
    }

    public static class AddVertexCommand<T> extends GraphCommand<T> {
        private final T element;
        private final VectorClock vectorClock;

        public AddVertexCommand(String crdtId, T element, VectorClock vectorClock) {
            super(crdtId);
            this.element = element;
            this.vectorClock = vectorClock;
        }
    }

    public static class RemoveVertexCommand<T> extends GraphCommand<T> {
        private final T element;

        public RemoveVertexCommand(String crdtId, T element) {
            super(crdtId);
            this.element = element;
        }
    }

    public static class AddEdgeCommand<T> extends GraphCommand<T> {
        private final T element1;
        private final T element2;
        
        public AddEdgeCommand(String crdtId, T element1, T element2) {
            super(crdtId);
            this.element1 = element1;
            this.element2 = element2;
        }
    }

    public static class RemoveEdgeCommand<T> extends GraphCommand<T> {
        private final T element1;
        private final T element2;
        public RemoveEdgeCommand(String crdtId, T element1, T element2) {
            super(crdtId);
            this.element1 = element1;
            this.element2 = element2;
        }
    }

}
