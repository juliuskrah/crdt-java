package com.juliuskrah;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Julius Krah
 * @see {@link RGA RGA CRDT}
 */
public class RGATest {
    private final CRDTStoreFactory factory = CRDTStoreFactory.getInstance();

    @Test
    @DisplayName("RGA tests to add and merge")
    void testRGAOperations() {
        // create two CRDT Stores and connect them
        final CRDTStore crdtStore1 = factory.crdtStore();
        final CRDTStore crdtStore2 = factory.crdtStore();
        crdtStore1.connect(crdtStore2);

        // create RGA replica and find a second replica from store
        final var replica1 = crdtStore1.<String>createRGA("12-AD");
        final var replica2 = crdtStore2.<String>findRGA("12-AD").get();

        // add an entry to each replica
        replica1.add("STROKE_UP");
        replica2.add("STROKE_DOWN");

        SoftAssertions softly = new SoftAssertions();

        // the stores are connected, thus the RGA is automatically synchronized
        softly.assertThat(replica1).containsExactly("STROKE_UP", "STROKE_DOWN");
        softly.assertThat(replica2).containsExactly("STROKE_UP", "STROKE_DOWN");

        // disconnect the stores simulating a network issue, brain split
        crdtStore1.disconnect(crdtStore2);

        // remove an entry from replica1 and add an entry to replica2
        replica1.remove("STROKE_DOWN");
        replica2.add(1, "STROKE_LEFT");

        // the stores are not connected, thus the changes have only local effects
        softly.assertThat(replica1).containsExactly("STROKE_UP");
        softly.assertThat(replica2).containsExactly("STROKE_UP", "STROKE_LEFT", "STROKE_DOWN");

        // reconnect the stores, network is healed
        crdtStore1.connect(crdtStore2);

        // the RGA is synchronized automatically
        softly.assertThat(replica1).containsExactly("STROKE_UP", "STROKE_LEFT");
        softly.assertThat(replica2).containsExactly("STROKE_UP", "STROKE_LEFT");

        // disconnect the stores
        crdtStore1.disconnect(crdtStore2);

        replica1.remove(0);
        replica1.add("STROKE_RIGHT");
        replica2.remove(0);
        replica2.add("STROKE_CENTRE");

        // the first entry has been replaced
        softly.assertThat(replica1).containsExactly("STROKE_LEFT", "STROKE_RIGHT");
        softly.assertThat(replica2).containsExactly("STROKE_LEFT", "STROKE_CENTRE");

        // reconnect the stores
        crdtStore1.connect(crdtStore2);

        // we have actually added two elements, the RGA keeps both
        softly.assertThat(replica1).containsExactlyInAnyOrder("STROKE_LEFT", "STROKE_RIGHT", "STROKE_CENTRE");
        softly.assertThat(replica2).containsExactlyInAnyOrder("STROKE_LEFT", "STROKE_RIGHT", "STROKE_CENTRE");

        softly.assertAll();
    }
}
