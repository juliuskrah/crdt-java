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

        SoftAssertions softly = new SoftAssertions();
        replica1.addVertex("julius");
        replica1.addVertex("james");
        replica1.addVertex("deleteme");
        // replica2.addVertex("zumar");
        // replica2.addVertex("alice");
        // replica2.addVertex("freda");
        // five vertices were added accross 2 replicas
        softly.assertThat(replica1.vertexSize()).isEqualTo(3);

        replica1.removeVertex("deleteme");
        softly.assertThat(replica1.vertexSize()).isEqualTo(2);

        // add edge to non existing vertices
        var result = replica1.addEdge("fake1", "fake2");
        softly.assertThat(result).isFalse();

        // add edge incident on julius and james
        result = replica1.addEdge("julius", "james");
        softly.assertThat(result).isTrue();
        softly.assertThat(replica1.findAdjacentVertices("julius")).isNotEmpty()
            .extracting(Vertex::getValue).containsOnly("james");
        softly.assertThat(replica1.findAdjacentVertices("james")).isNotEmpty()
            .extracting(Vertex::getValue).containsOnly("julius");

        replica1.removeEdge("julius", "james");
        softly.assertThat(replica1.findAdjacentVertices("julius")).isEmpty();
        softly.assertThat(replica1.findAdjacentVertices("james")).isEmpty();

        softly.assertAll();
    }   
}
