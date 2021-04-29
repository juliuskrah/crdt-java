package com.juliuskrah;

import java.util.Objects;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.vavr.control.Option;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Implementations are encouraged to extend from this abstract class
 * 
 * @author Julius Krah
 * @param <COMMAND>
 */
public abstract class AbstractCRDT<COMMAND extends CRDTCommand> implements CRDT<COMMAND> {
    /**
     * The node ID of the current replica
     */
    protected final String nodeId;
    protected final String crdtId;
    protected final Sinks.Many<COMMAND> commands;

    public AbstractCRDT(String nodeId, String crdtId, Sinks.Many<COMMAND> commands) {
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null");
        this.crdtId = Objects.requireNonNull(crdtId, "crdtId must not be null");
        this.commands = Objects.requireNonNull(commands, "commands must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCrdtId() {
        return crdtId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe(Subscriber<? super COMMAND> subscriber) {
        commands.asFlux().subscribe(subscriber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribeTo(Publisher<? extends COMMAND> publisher) {
        Flux.from(publisher).onTerminateDetach().subscribe(command -> {
            final Option<? extends COMMAND> newCommand = processCommand(command);
            newCommand.peek(commands::tryEmitNext);
        });
    }

    protected abstract Option<? extends COMMAND> processCommand(COMMAND command);
}
