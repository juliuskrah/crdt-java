package com.juliuskrah;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Julius Krah
 * @see {@link LWWElementSet LWW Element Set CRDT}
 */
public class LWWElementSetTest {
    private final CRDTStoreFactory factory = CRDTStoreFactory.getInstance();

    @Test
    @DisplayName("LWW Element-Set tests to add and merge")
    void testLWWElementSetOperations() {
        // create two CRDT Stores and connect them
        final CRDTStore crdtStore1 = factory.crdtStore("ND-31");
        final CRDTStore crdtStore2 = factory.crdtStore("ND-32");
        crdtStore1.connect(crdtStore2);

        // create an LWW-Element-Set and find a second replica from store
        final var replica1 = crdtStore1.<String>createLWWElementSet("14-AD");
        final var replica2 = crdtStore2.<String>findLWWElementSet("14-AD").get();

        replica1.add("STROKE_UP");
        replica2.add("STROKE_DOWN");

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(replica1.get()).containsExactly("STROKE_UP", "STROKE_DOWN");
        softly.assertThat(replica2.get()).containsExactly("STROKE_UP", "STROKE_DOWN");

        // disconnect the stores simulating a network issue, brain split
        crdtStore1.disconnect(crdtStore2);

        // replica1 removes an item and replica2 adds that item while in isolation
        replica1.remove("STROKE_RIGHT");
        replica2.add("STROKE_RIGHT");        
        softly.assertThat(replica1.get()).containsExactly("STROKE_UP", "STROKE_DOWN");
        softly.assertThat(replica2.get()).containsExactly("STROKE_UP", "STROKE_DOWN", "STROKE_RIGHT");

         // reconnect the stores
         crdtStore1.connect(crdtStore2);
       
         // Both replicas are synchronised now.
        softly.assertThat(replica1.get()).containsExactly("STROKE_UP", "STROKE_DOWN", "STROKE_RIGHT");
        softly.assertThat(replica2.get()).containsExactly("STROKE_UP", "STROKE_DOWN", "STROKE_RIGHT");

        replica2.remove("STROKE_UP");
        softly.assertThat(replica1.get()).containsExactly("STROKE_DOWN", "STROKE_RIGHT");
        softly.assertThat(replica2.get()).containsExactly("STROKE_DOWN", "STROKE_RIGHT");

        softly.assertAll();
    }
}
