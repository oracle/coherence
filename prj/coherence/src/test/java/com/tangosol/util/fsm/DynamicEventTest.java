/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;
import com.tangosol.util.fsm.misc.Light;
import com.tangosol.util.fsm.misc.TraceAction;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.testsupport.deferred.Eventually.within;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the {@link NonBlockingFiniteStateMachine}.
 *
 * @author Brian Oliver
 */
public class DynamicEventTest
    {
    /**
     * A simple test of transitions for a {@link Light}.
     */
    @Test
    public void testSimpleLightSwitchMachine()
        {
        // build the finite state machine model
        SimpleModel<Brightness> model = new SimpleModel<Brightness>(Brightness.class);

        model.addTransition(new Transition<Brightness>("Dull",
                Brightness.OFF,
                Brightness.DULL,
                new TraceAction<Brightness>()));
        model.addTransition(new Transition<Brightness>("Medium",
                Brightness.DULL,
                Brightness.MEDIUM,
                new TraceAction<Brightness>()));
        model.addTransition(new Transition<Brightness>("Bright",
                Brightness.MEDIUM,
                Brightness.BRIGHT,
                new TraceAction<Brightness>()));
        model.addTransition(new Transition<Brightness>("Off",
                Brightness.BRIGHT,
                Brightness.OFF,
                new TraceAction<Brightness>()));

        model.addStateEntryAction(Brightness.BRIGHT, new StateEntryAction<Brightness>()
        {
        @Override
        public Instruction onEnterState(Brightness        exitingState,
                Brightness        enteringState,
                Event<Brightness> event,
                ExecutionContext  context)
            {
            return Instruction.NOTHING;
            }
        });

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setName("fsm-unit:DynamicEventTest:testSimpleLightSwitchMachine");

        // build the finite state machine from the model
        NonBlockingFiniteStateMachine<Brightness> machine =
                new NonBlockingFiniteStateMachine<Brightness>("Light Switch",
                        model,
                        Brightness.OFF,
                        deps,
                        true,
                        null);

        machine.start();

        Eventually.assertDeferred(machine::getState,
                              is(Brightness.OFF),
                              within(2, TimeUnit.SECONDS));

        machine.process(SwitchEvent.TURN);

        machine.process(SwitchEvent.TURN);

        machine.process(SwitchEvent.TURN);

        machine.process(SwitchEvent.TURN);

        machine.process(SwitchEvent.TURN);

        machine.process(SwitchEvent.TURN);

        machine.process(SwitchEvent.TURN);

        assertThat(machine.quiesceThenStop(), is(true));

        assertThat(machine.stop(), is(false));
        }

    /**
     * A {@link SwitchEvent} captures the {@link Event}s that can happen to
     * a {@link Light}.
     */
    private enum SwitchEvent implements Event<Brightness>
        {
        TURN;

        /**
         * {@inheritDoc}
         */
        @Override
        public Brightness getDesiredState(Brightness       currentState,
                ExecutionContext context)
            {
            // each 'turn' event increases the brightness.  when a turn
            // from 'bright' brightness occurs, the brightness becomes 'off'

            int cBrightness = Brightness.values().length;

            return Brightness.values()[(currentState.ordinal() + 1) % cBrightness];
            }
        }

    /**
     * A {@link Light} that provides several different distinct levels of
     * brightness.
     */
    private enum Brightness
        {
        OFF,
        DULL,
        MEDIUM,
        BRIGHT
        }
    }
