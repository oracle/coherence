/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;
import com.tangosol.util.fsm.NonBlockingFiniteStateMachine.CoalescedEvent;
import com.tangosol.util.fsm.NonBlockingFiniteStateMachine.CoalescedEvent.Process;
import com.tangosol.util.fsm.NonBlockingFiniteStateMachine.SubsequentEvent;
import com.tangosol.util.fsm.misc.Light;
import com.tangosol.util.fsm.misc.Motor;
import com.tangosol.util.fsm.misc.MotorEvent;
import com.tangosol.util.fsm.misc.SwitchEvent;
import com.tangosol.util.fsm.misc.TraceAction;
import com.tangosol.util.fsm.misc.TrackingEvent;
import org.junit.Test;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link NonBlockingFiniteStateMachine}
 *
 * @author Brian Oliver
 */
public class NonBlockingFiniteStateMachineTest
    {
    /**
     * A simple test of transitions for a {@link Light}.
     */
    @Test
    public void testSimpleLightSwitchMachine()
        {
        final AtomicInteger count           = new AtomicInteger(0);
        final String        sDaemonPoolName = "fsm-unit:NonBlockingFiniteStateMachineTest:testSimpleLightSwitchMachine";

        // build the finite state machine model
        SimpleModel<Light> model = new SimpleModel<>(Light.class);

        model.addTransition(new Transition<>("Turn On", Light.OFF, Light.ON, new TraceAction<>()));
        model.addTransition(new Transition<>("Turn Off", Light.ON, Light.OFF, new TraceAction<>()));
        model.addTransition(new Transition<>("Break It",
                EnumSet.of(Light.ON, Light.OFF),
                Light.BROKEN,
                new TraceAction<>()));

        model.addStateEntryAction(Light.ON, new StateEntryAction<Light>()
        {
        @Override
        public Instruction onEnterState(Light previousState,
                Light newState,
                Event<Light> event,
                ExecutionContext context)
            {
            assertTrue(Thread.currentThread().getName().startsWith(sDaemonPoolName));

            System.out.println("The light is on.");
            count.incrementAndGet();

            return Instruction.NOTHING;
            }
        });

        model.addStateEntryAction(Light.OFF, new StateEntryAction<Light>()
        {
        @Override
        public Instruction onEnterState(Light previousState,
                Light newState,
                Event<Light> event,
                ExecutionContext context)
            {
            assertTrue(Thread.currentThread().getName().startsWith(sDaemonPoolName));

            System.out.println("The light is off.");

            return Instruction.NOTHING;
            }
        });

        model.addStateEntryAction(Light.BROKEN, new StateEntryAction<Light>()
        {
        @Override
        public Instruction onEnterState(Light previousState,
                Light newState,
                Event<Light> event,
                ExecutionContext context)
            {
            assertTrue(Thread.currentThread().getName().startsWith(sDaemonPoolName));

            System.out.println("The light is broken.");

            return Instruction.NOTHING;
            }
        });

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setName(sDaemonPoolName);

        // build the finite state machine from the model
        NonBlockingFiniteStateMachine<Light> machine = new NonBlockingFiniteStateMachine<Light>("Light Bulb",
                model,
                Light.OFF,
                deps,
                true,
                null);

        machine.start();

        machine.process(SwitchEvent.SWITCH_ON);

        machine.process(SwitchEvent.SWITCH_OFF);

        machine.process(SwitchEvent.SWITCH_ON);

        machine.process(SwitchEvent.SWITCH_ON);

        machine.process(SwitchEvent.SWITCH_OFF);

        machine.process(SwitchEvent.SWITCH_OFF);

        machine.process(SwitchEvent.TOGGLE_TOO_FAST);

        machine.process(SwitchEvent.SWITCH_ON);

        machine.process(SwitchEvent.SWITCH_OFF);

        assertThat(machine.quiesceThenStop(), is(true));

        assertThat(count.get(), is(2));

        assertThat(machine.getState(), is(Light.BROKEN));

        assertThat(machine.getTransitionCount(), is(5L));

        assertThat(machine.stop(), is(false));
        }


    /**
     * Ensure that a). a scheduled {@link SubsequentEvent} is processed when
     * no interleaving of other events occur, and b). a scheduled
     * {@link SubsequentEvent} is not processed when interleaving of other
     * events does occur.
     */
    @Test
    public void testSubsequentEventProcessing()
        {
        SimpleModel<Motor> model = new SimpleModel<>(Motor.class);

        model.addTransition(new Transition<>("Turn On", Motor.STOPPED, Motor.RUNNING, new TraceAction<>()));
        model.addTransition(new Transition<>("Turn Off", Motor.RUNNING, Motor.STOPPED, new TraceAction<>()));

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setName("fsm-unit:NonBlockingFiniteStateMachineTest:testSubsequentEventProcessing");

        NonBlockingFiniteStateMachine<Motor> machine =
                new NonBlockingFiniteStateMachine<>("Subsequent Testing Model",
                        model,
                        Motor.STOPPED,
                        deps,
                        false,
                        null);

        machine.start();

        machine.processLater(new SubsequentEvent<>(MotorEvent.TURN_ON), 1, TimeUnit.SECONDS);

        // ensure that a subsequent event is executed when there's no interleaving
        Eventually.assertDeferred(() -> machine.getTransitionCount(), is(1L));
        assertThat(machine.getState(), is(Motor.RUNNING));

        // ensure that a subsequent event is not executed when there's interleaving
        TrackingEvent<Motor> event = new TrackingEvent<>(new SubsequentEvent<>(MotorEvent.TURN_ON));

        machine.processLater(event, 1, TimeUnit.SECONDS);

        machine.process(MotorEvent.TURN_OFF);
        machine.process(MotorEvent.TURN_ON);
        machine.process(MotorEvent.TURN_OFF);

        Eventually.assertDeferred(() -> machine.getTransitionCount(), is(4L));

        assertThat(machine.getState(), is(Motor.STOPPED));

        assertThat(event.accepted(), is(true));
        assertThat(event.evaluated(), is(false));
        assertThat(event.processed(), is(false));

        assertThat(machine.stop(), is(true));
        }


    /**
     * Ensure that only a single {@link CoalescedEvent} is processed.
     */
    @Test
    public void testCoalescedEventProcessing()
        {
        SimpleModel<Motor> model = new SimpleModel<>(Motor.class);

        model.addTransition(new Transition<>("Turn On", Motor.STOPPED, Motor.RUNNING, new TraceAction<>()));
        model.addTransition(new Transition<>("Turn Off", Motor.RUNNING, Motor.STOPPED, new TraceAction<>()));

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setName("fsm-unit:NonBlockingFiniteStateMachineTest:testCoalescedEventProcessing");

        NonBlockingFiniteStateMachine<Motor> machine =
                new NonBlockingFiniteStateMachine<>("Subsequent Testing Model",
                        model,
                        Motor.STOPPED,
                        deps,
                        false,
                        null);

        machine.start();

        // ensure that the first submitted coalesced event is processed
        // and the others are not
        TrackingEvent<Motor> eventA = new TrackingEvent<>(MotorEvent.TURN_ON);
        TrackingEvent<Motor> eventB = new TrackingEvent<>(MotorEvent.TURN_ON);
        TrackingEvent<Motor> eventC = new TrackingEvent<>(MotorEvent.TURN_ON);

        machine.processLater(new CoalescedEvent<>(eventA, Process.FIRST), 3, TimeUnit.SECONDS);
        machine.processLater(new CoalescedEvent<>(eventB, Process.FIRST), 2, TimeUnit.SECONDS);
        machine.processLater(new CoalescedEvent<>(eventC, Process.FIRST),
                             1, TimeUnit.SECONDS);

        Eventually.assertDeferred(() -> machine.getTransitionCount(), is(1L));
        assertThat(machine.getState(), is(Motor.RUNNING));

        assertThat(eventA.accepted(), is(true));
        Eventually.assertDeferred(() -> eventA.evaluated(), is(true));
        assertThat(eventA.processed(), is(true));

        assertThat(eventB.accepted(), is(true));
        Eventually.assertDeferred(() -> eventB.evaluated(), is(false));
        assertThat(eventB.processed(), is(false));

        assertThat(eventC.accepted(), is(true));
        Eventually.assertDeferred(() -> eventC.evaluated(), is(false));
        assertThat(eventC.processed(), is(false));

        // ensure that the most recently submitted coalesced event is processed
        // and the others are not
        TrackingEvent<Motor> event1 = new TrackingEvent<>(MotorEvent.TURN_OFF);
        TrackingEvent<Motor> event2 = new TrackingEvent<>(MotorEvent.TURN_OFF);
        TrackingEvent<Motor> event3 = new TrackingEvent<>(MotorEvent.TURN_OFF);

        machine.processLater(new CoalescedEvent<>(event1, Process.MOST_RECENT), 3, TimeUnit.SECONDS);
        machine.processLater(new CoalescedEvent<>(event2, Process.MOST_RECENT), 2, TimeUnit.SECONDS);
        machine.processLater(new CoalescedEvent<>(event3, Process.MOST_RECENT), 1, TimeUnit.SECONDS);

        Eventually.assertDeferred(() -> machine.getTransitionCount(), is(2L));
        assertThat(machine.getState(), is(Motor.STOPPED));

        assertThat(event3.accepted(), is(true));
        Eventually.assertDeferred(() -> event3.evaluated(), is(true));
        assertThat(event3.processed(), is(true));

        assertThat(event2.accepted(), is(true));
        Eventually.assertDeferred(() -> event2.evaluated(), is(false));
        assertThat(event2.processed(), is(false));

        assertThat(event1.accepted(), is(true));
        Eventually.assertDeferred(() -> event1.evaluated(), is(false));
        assertThat(event1.processed(), is(false));

        assertThat(machine.stop(), is(true));
        }
    }
