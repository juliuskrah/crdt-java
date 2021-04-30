package com.juliuskrah;

import java.util.Objects;
import java.util.Stack;

import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
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
        vectorClock = vectorClock.increment();
        var result = doAddEdge(element1, element2);
        commands.emitNext(new AddEdgeCommand<T>(crdtId, element1, element2, vectorClock), 
            EmitFailureHandler.FAIL_FAST);
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
        vectorClock = vectorClock.increment();
        doRemoveVertex(element);
        commands.emitNext(new RemoveVertexCommand<>(crdtId, element, vectorClock), //
            EmitFailureHandler.FAIL_FAST);
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
        vectorClock = vectorClock.increment();
        doRemoveEdge(element1, element2);
        commands.emitNext(new RemoveEdgeCommand<>(crdtId, element1, element2, vectorClock), //
            EmitFailureHandler.FAIL_FAST);
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected Option<? extends GraphCommand<T>> processCommand(GraphCommand<T> command) {
        if (vectorClock.compareTo(command.vectorClock) < 0 ) {
            if(command instanceof AddVertexCommand) {
                var addVertex = (AddVertexCommand<T>) command;
                vectorClock = vectorClock.merge(command.vectorClock);
                doAddVertex(addVertex.element, command.vectorClock);
                return Option.of(command);
            } else if(command instanceof RemoveVertexCommand) {
                var removeVertex = (RemoveVertexCommand<T>) command;
                doRemoveVertex(removeVertex.element);
                return Option.of(command);
            } else if(command instanceof AddEdgeCommand) {
                var addEdge = (AddEdgeCommand<T>) command;
                doAddEdge(addEdge.element1, addEdge.element2);
                return Option.of(command);
            } else if(command instanceof RemoveEdgeCommand) {
                var removeEdge = (RemoveEdgeCommand<T>) command;
                doRemoveEdge(removeEdge.element1, removeEdge.element2);
                return Option.of(command);
            }
        }
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

    public Set<T> findPath(T src, T dest) {
        // to keep track of whether a vertex is visited or not
        Set<T> visited = HashSet.empty();
        Stack<T> path = new Stack<>();
        // include the current node in the path
        path.push(src);
        if (src == dest) {
            return visited;
        }
        
        while (!path.isEmpty()) {
            T element = path.pop();
            if (!visited.contains(element)) {
                visited = visited.add(element);
                for (Vertex<T> vertex : findAdjacentVertices(element)) {              
                    path.push(vertex.getValue());
                }
            }
        }
        return visited;
    }

    public int vertexSize() {
        return this.vertices.size();
    }

    public boolean containsVertex(T element) {
        if(elements.get(element).isEmpty()) {
            return false;
        }
        var vertex = new Vertex<>(element, elements.get(element).get());
        return vertices.containsKey(vertex);
    }

    public static class GraphCommand<T> extends CRDTCommand {
        private final VectorClock vectorClock;
        public GraphCommand(String crdtId, VectorClock vectorClock) {
            super(crdtId);
            this.vectorClock = vectorClock;
        }
    }

    public static class AddVertexCommand<T> extends GraphCommand<T> {
        private final T element;

        public AddVertexCommand(String crdtId, T element, VectorClock vectorClock) {
            super(crdtId, vectorClock);
            this.element = element;
        }
    }

    public static class RemoveVertexCommand<T> extends GraphCommand<T> {
        private final T element;

        public RemoveVertexCommand(String crdtId, T element, VectorClock vectorClock) {
            super(crdtId, vectorClock);
            this.element = element;
        }
    }

    public static class AddEdgeCommand<T> extends GraphCommand<T> {
        private final T element1;
        private final T element2;
        
        public AddEdgeCommand(String crdtId, T element1, T element2, VectorClock vectorClock) {
            super(crdtId, vectorClock);
            this.element1 = element1;
            this.element2 = element2;
        }
    }

    public static class RemoveEdgeCommand<T> extends GraphCommand<T> {
        private final T element1;
        private final T element2;
        public RemoveEdgeCommand(String crdtId, T element1, T element2, VectorClock vectorClock) {
            super(crdtId, vectorClock);
            this.element1 = element1;
            this.element2 = element2;
        }
    }

}
