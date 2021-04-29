package com.juliuskrah;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Julius Krah
 * @see {@link LWWRegister LWW Register CRDT}
 */
public class LWWRegisterTest {
    private final CRDTStoreFactory factory = CRDTStoreFactory.getInstance();

    @Test
    @DisplayName("LWW Register tests to set and merge")
    void testLWWRegisterOperations() {
        // create two CRDT Stores and connect them
        final CRDTStore crdtStore1 = factory.crdtStore("ND-21");
        final CRDTStore crdtStore2 = factory.crdtStore("ND-22");
        crdtStore1.connect(crdtStore2);

        // create an LWW-Register and find a second replica from store
        final var replica1 = crdtStore1.<String>createLWWRegister("13-AD");
        final var replica2 = crdtStore2.<String>findLWWRegister("13-AD").get();

        // set values in both replicas
        replica1.set("STROKE_CENTRE");
        replica2.set("STROKE_LEFT");

        SoftAssertions softly = new SoftAssertions();

        // the stores are connected, thus the last write wins
        softly.assertThat(replica1.get()).isEqualTo("STROKE_LEFT");
        softly.assertThat(replica2.get()).isEqualTo("STROKE_LEFT");

        // disconnect the stores simulating a network issue, brain split
        crdtStore1.disconnect(crdtStore2);

        // add one entry to each replica
        replica1.set("STROKE_DOWN");
        replica2.set("STROKE_UP");

        // the stores are not connected, thus the changes have only local effects
        softly.assertThat(replica1.get()).isEqualTo("STROKE_DOWN");
        softly.assertThat(replica2.get()).isEqualTo("STROKE_UP");

        // reconnect the stores
        crdtStore1.connect(crdtStore2);

        // the LWW-Register is synchronized automatically.
        // as the update happened concurrently, the update from the node with the larger
        // ID wins
        softly.assertThat(replica1.get()).isEqualTo("STROKE_UP");
        softly.assertThat(replica2.get()).isEqualTo("STROKE_UP");

        softly.assertAll();
    }

}
