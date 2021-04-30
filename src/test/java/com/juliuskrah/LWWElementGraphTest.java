package com.juliuskrah;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Julius Krah
 * @see {@link LWWElementGraph LWW Element Graph CRDT}
 */
public class LWWElementGraphTest {
    private final CRDTStoreFactory factory = CRDTStoreFactory.getInstance();

    @Test
    @DisplayName("LWW Element-Graph tests to add and merge")
    void testLWWElementGraphOperations() {
        // create two CRDT Stores and connect them
        final CRDTStore crdtStore1 = factory.crdtStore("ND-41");
        final CRDTStore crdtStore2 = factory.crdtStore("ND-42");
        crdtStore1.connect(crdtStore2);

        // create an LWW-Element-Graph and find a second replica from store
        final var replica1 = crdtStore1.<String>createLWWElementGraph("15-AD");
        final var replica2 = crdtStore2.<String>findLWWElementGraph("15-AD").get();

        SoftAssertions softly = new SoftAssertions();
        // 6 vertices [julius, james, zumar, alice, freda]
        replica1.addVertex("julius");
        replica1.addVertex("james");
        replica1.addVertex("deleteme");
        replica2.addVertex("zumar");
        replica2.addVertex("alice");
        replica2.addVertex("freda");
        // 6 vertices were added accross 2 replicas
        softly.assertThat(replica1.vertexSize()).isEqualTo(6);
        softly.assertThat(replica2.vertexSize()).isEqualTo(6);

        replica1.removeVertex("deleteme");
        softly.assertThat(replica1.vertexSize()).isEqualTo(5);
        softly.assertThat(replica2.vertexSize()).isEqualTo(5);

        // add edge to non existing vertices
        var result = replica1.addEdge("fake1", "fake2");
        softly.assertThat(result).isFalse();

        // add edge incident on julius and james
        result = replica1.addEdge("julius", "james");
        softly.assertThat(result).isTrue();
        // add edges [[alice,zumar], [james,zumar], [julius,alice], [alice,freda], [james,freda], [julius,james]]
        replica1.addEdge("alice", "zumar");
        replica1.addEdge("james", "zumar");
        replica2.addEdge("julius", "alice");
        replica2.addEdge("alice", "freda");
        replica2.addEdge("james", "freda");
        // both replicas should see the update
        softly.assertThat(replica1.findAdjacentVertices("julius")).isNotEmpty()
            .extracting(Vertex::getValue).containsOnly("james", "alice");
        softly.assertThat(replica2.findAdjacentVertices("james")).isNotEmpty()
            .extracting(Vertex::getValue).containsOnly("julius", "freda", "zumar");

        // check existence of vertex
        softly.assertThat(replica1.containsVertex("julius")).isTrue();
        softly.assertThat(replica2.containsVertex("notfound")).isFalse();
        
        // remove edge E(julius, james)
        replica1.removeEdge("julius", "james");
        softly.assertThat(replica1.findAdjacentVertices("julius")).isNotEmpty() //
            .extracting(Vertex::getValue).containsOnly("alice");
        softly.assertThat(replica2.findAdjacentVertices("james")).isNotEmpty() //
            .extracting(Vertex::getValue).containsOnly("julius", "freda", "zumar");

        softly.assertThat(replica2.findPath("julius", "zumar")).contains("values");
        
        // disconnect the stores simulating a network issue, brain split
        crdtStore1.disconnect(crdtStore2);

        // replica2 is updated while in a disconnected state
        replica2.addEdge("julius", "james");
        softly.assertThat(replica2.findAdjacentVertices("julius")).isNotEmpty()
            .extracting(Vertex::getValue).containsOnly("james", "alice");
        // replica1 hasn't seen the update yet
        softly.assertThat(replica1.findAdjacentVertices("julius")).isNotEmpty() //	
            .extracting(Vertex::getValue).containsOnly("alice");
        
        // reconnect the stores
        crdtStore1.connect(crdtStore2);
        softly.assertThat(replica1.findAdjacentVertices("julius")).isNotEmpty() //	
            .extracting(Vertex::getValue).containsOnly("alice", "james");

        softly.assertAll();
    }   
}
