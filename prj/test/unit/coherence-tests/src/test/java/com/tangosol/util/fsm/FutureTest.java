/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

import com.oracle.coherence.common.base.Blocking;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;
import com.tangosol.util.fsm.annotations.OnEnterState;
import com.tangosol.util.fsm.annotations.Transition;
import com.tangosol.util.fsm.annotations.Transitions;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * This class contains unit tests for future finite state machine events.
 *
 * @author pfm  2013.04.10
 */
public class FutureTest
    {
    /**
     * Test that an ignored event does not affect future events.
     */
    @Test
    public void testFutureEvent()
        {
        // build the finite state machine model
        AnnotationDrivenModel<State> model =
                new AnnotationDrivenModel<State>(State.class,
                        new Model());

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setName("fsm-unit:FutureTest:testFutureEvent");

        // build the finite state machine from the model
        // note that the initial state is running if the local participant is already known
        FiniteStateMachine<State> machine = new NonBlockingFiniteStateMachine<State>("Test",
                model,
                State.FIRST,
                deps,
                false,
                null);

        machine.start();

        // attempt to for the machine to transition to the unreachable state
        // (which should be impossible) until the last state is reached
        // (try at most 50 times)
        int retries = 50;
        while (machine.getState() != State.LAST && retries-- > 0)
            {
            machine.process(new Instruction.TransitionTo(State.UNREACHABLE));

            try
                {
                Blocking.sleep(100);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
                }
            }

        assertThat(machine.getState(), is(State.LAST));

        assertThat(machine.stop(), is(true));
        }

    // ----- Finite State Machine Model -------------------------------------

    /**
     * The Finite State Machine Model.
     */
    @Transitions({
            @Transition(
                    fromStates = {FIRST},
                    toState    = SECOND),
            @Transition(
                    fromStates = {SECOND},
                    toState    = LAST)
    })
    public class Model
        {
        @OnEnterState(FIRST)
        public Instruction onEnterFirst(State previousState,
                                        State newState,
                                        Event<State> event,
                                        ExecutionContext context)
            {
            return new NonBlockingFiniteStateMachine.ProcessEventLater<State>(
                    new NonBlockingFiniteStateMachine.SubsequentEvent(new Instruction.TransitionTo(State.SECOND)),
                    1, TimeUnit.SECONDS);
            }

        @OnEnterState(SECOND)
        public Instruction onEnterSecond(State previousState,
                                         State newState,
                                         Event<State> event,
                                         ExecutionContext context)
            {
            return new NonBlockingFiniteStateMachine.ProcessEventLater<State>((new Instruction.TransitionTo(State.LAST)),
                    1, TimeUnit.SECONDS);
            }

        @OnEnterState(LAST)
        public Instruction onEnterLast(State previousState,
                                       State newState,
                                       Event<State> event,
                                       ExecutionContext context)
            {
            return Instruction.NOTHING;
            }

        @OnEnterState(UNREACHABLE)
        public Instruction onEnterUnreachable(State previousState,
                                              State newState,
                                              Event<State> event,
                                              ExecutionContext context)
            {
            throw new IllegalStateException("UNREACHABLE was reached.  This should be impossible");
            }
        }

    // ----- inner class State ----------------------------------------------

    /**
     * The state of the Finite State Machine.
     */
    public enum State
        {
          FIRST,
          SECOND,
          LAST,
          UNREACHABLE
        }

    // NOTE: Strings are needed for annotation

    private static final String FIRST = "FIRST";

    private static final String SECOND = "SECOND";

    private static final String LAST = "LAST";

    private static final String UNREACHABLE = "UNREACHABLE";
    }
