package com.juliuskrah;

import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

/**
 * A simple LWW element set. The underlying set is a HashSet
 * 
 * @author Julius Krah
 * @param <E>
 * @implNote We start from less complex implementations to test our
 *           synchronization
 */
public class LWWElementSet<E> extends AbstractCRDT<LWWElementSet.SetCommand<E>> {
    private Map<E, VectorClock> addSet = HashMap.empty();
    private Map<E, VectorClock> removeSet = HashMap.empty();
    private Set<E> elements = HashSet.empty();
    private VectorClock vectorClock;
    private final LWWBias bias;

    private void doAdd(E element) {
        vectorClock = vectorClock.increment();
        addSet = addSet.put(element, vectorClock);
        updateElements(element);
    }

    /**
     * Publishes an event to replicas after successfully adding an element
     * @see #add(Object)
     */
    private void prepareAdd(E element) {
        doAdd(element);
        commands.emitNext(new SetCommand<E>(crdtId, element, vectorClock, 1), EmitFailureHandler.FAIL_FAST);
    }

    /**
     * Updates the set by checking the elements in addSet against the elements in the removeSet
     * and keep any elements that appear in both add and remove sets but have a higher
     * vectorClock in addSet
     * @param element
     * @see #doAdd(Object)
     * @see #doRemove(Object)
     */
    private void updateElements(E element) {
        VectorClock removeTime = removeSet.get(element).isDefined() ? //
                removeSet.get(element).get() : null;
        VectorClock addTime = addSet.get(element).isDefined() ? //
                addSet.get(element).get() : null;
        // element is in both addSet and removeSet
        if (removeTime != null && addTime != null) {
            if ((removeTime.compareTo(addTime) < 0 //
                    || (removeTime.compareTo(addTime) == 0 && bias == LWWBias.ADD))) {
                elements = elements.add(element);
            } else {
                elements = elements.remove(element);
            }
        } else if (addTime != null) {
            elements = elements.add(element);
        } else {
            elements = elements.remove(element);
        }
    }

    private void doRemove(E element) {
        vectorClock = vectorClock.increment();
        removeSet = removeSet.put(element, vectorClock);
        updateElements(element);
    }

    /**
     * Publishes an event to replicas after successfully removing an element
     * @param element
     * @see #remove(Object)
     */
    private void prepareRemove(E element) {
        doRemove(element);
        commands.emitNext(new SetCommand<E>(crdtId, element, vectorClock, 0), EmitFailureHandler.FAIL_FAST);
    }

    /**
     * Processes all received events (commands)
     */
    @Override
    protected Option<? extends SetCommand<E>> processCommand(SetCommand<E> command) {
        if (vectorClock.compareTo(command.vectorClock) < 0 
            || vectorClock.compareTo(command.vectorClock) == 0) {
            E element = command.element;
            switch (command.type) {
            case 0:
                doRemove(element);
                return Option.of(command);
            case 1:
                doAdd(element);
                return Option.of(command);
            default:
                // do nothing
                break;
            }
        }
        return Option.none();
    }

    public LWWElementSet(String nodeId, String crdtId) {
        this(nodeId, crdtId, LWWBias.ADD);
    }

    public LWWElementSet(String nodeId, String crdtId, LWWBias bias) {
        super(nodeId, crdtId, Sinks.many().replay().all());
        this.bias = bias;
        this.vectorClock = new VectorClock(nodeId);
    }

    /**
     * Adds an element to the addSet
     * @param element
     */
    public void add(E element) {
        prepareAdd(element);
    }

    /**
     * Removes an element from the removeSet
     * @param element
     */
    public void remove(E element) {
        prepareRemove(element);
    }

    /**
     * Retrieves current items in set
     * @return
     */
    public Set<E> get() {
        return this.elements;
    }

    /**
     * A BIAS value that determines whether to keep add or remove elements
     * when they share the same vectorClock
     */
    public static enum LWWBias {
        ADD, REMOVE
    }

    public static final class SetCommand<E> extends CRDTCommand {
        final E element;
        final VectorClock vectorClock;
        final int type; // 0 = remove; 1 = add

        public SetCommand(String crdtId, E element, VectorClock vectorClock, int type) {
            super(crdtId);
            this.element = element;
            this.vectorClock = vectorClock;
            this.type = type;
        }

    }

}
