/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.common.base.Timeout;

import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.util.fsm.annotations.OnEnterState;
import com.tangosol.util.fsm.annotations.OnTransition;
import com.tangosol.util.fsm.annotations.Transition;
import com.tangosol.util.fsm.annotations.Transitions;

import com.tangosol.util.fsm.misc.Light;
import com.tangosol.util.fsm.misc.SwitchEvent;

import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the {@link NonBlockingFiniteStateMachine}
 *
 * @author Brian Oliver
 */
public class NonBlockingFiniteStateMachineAnnotationTest
    {
    /**
     * A simple test of transitions for a {@link Light}.
     */
    @Test
    public void testSimpleLightSwitchMachine() throws InterruptedException
        {
        MyFiniteStateMachineModel            myModel         = new MyFiniteStateMachineModel();
        AnnotationDrivenModel<Light>         model           = new AnnotationDrivenModel<Light>(Light.class, myModel);

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setName(
          "fsm-unit:NonBlockingFiniteStateMachineAnnotationTest:testSimpleLightSwitchMachine");

        NonBlockingFiniteStateMachine<Light> machine = new NonBlockingFiniteStateMachine<Light>("Light Bulb",
                model,
                Light.OFF,
                deps,
                true,
                null);

        machine.start();

        Eventually.assertDeferred(machine::getState, is(Light.OFF));

        machine.process(SwitchEvent.SWITCH_ON);

        machine.process(SwitchEvent.SWITCH_OFF);

        machine.process(SwitchEvent.SWITCH_ON);

        machine.process(SwitchEvent.SWITCH_ON);

        machine.process(SwitchEvent.SWITCH_OFF);

        machine.process(SwitchEvent.SWITCH_OFF);

        machine.process(SwitchEvent.TOGGLE_TOO_FAST);

        machine.process(SwitchEvent.SWITCH_ON);

        machine.process(SwitchEvent.SWITCH_OFF);

        try (Timeout timeout = Timeout.after(1, TimeUnit.MINUTES))
          {
          assertThat(machine.quiesceThenStop(), is(true));
          }
        catch(InterruptedException e)
          {
          throw e;
          }

        assertThat(machine.stop(), is(false));

        assertThat(myModel.getCount(), is(2L));

        assertThat(machine.getState(), is(Light.BROKEN));

        assertThat(machine.getTransitionCount(), is(5L));
        }

    /**
     * An example of a simple annotated {@link Transitions}.
     */
    @Transitions({@Transition(
            name       = "Turning On",
            fromStates = "OFF",
            toState    = "ON"
    ) , @Transition(
            name       = "Turning Off",
            fromStates = "ON",
            toState    = "OFF"
    ) , @Transition(
            name       = "Broken",
            fromStates = {"ON", "OFF"},
            toState    = "BROKEN"
    ) })
    public static class MyFiniteStateMachineModel
        {
        /**
         * A counter for the number of times a {@link Light} has been turned on
         */
        private final AtomicInteger m_count = new AtomicInteger(0);

        /**
         * Obtains the number of times the {@link MyFiniteStateMachineModel} has been
         * turned on.
         *
         * @return  the count of times turned on
         */
        public long getCount()
            {
            return m_count.get();
            }

        /**
         * A callback when a {@link Transitions} transitions from
         * {@link Light#OFF} to {@link Light#ON}.
         */
        @OnTransition(
                fromStates = "OFF",
                toStates   = "ON"
        )
        public void onTurningOn(String sTransitionName,
                Light stateFrom,
                Light stateTo,
                Event<Light> event,
                ExecutionContext context) throws RollbackTransitionException
            {
            System.out.printf("(executing '%s' transition #%d from %s to %s due to %s event)\n\n",
                    sTransitionName,
                    context.getTransitionCount() + 1,
                    stateFrom,
                    stateTo,
                    event);
            }

        /**
         * A callback when a {@link Transitions} transitions from
         * {@link Light#ON} to {@link Light#OFF}.
         */
        @OnTransition(
                fromStates = "ON",
                toStates   = "OFF"
        )
        public void onTurningOff(String sTransitionName,
                Light stateFrom,
                Light stateTo,
                Event<Light> event,
                ExecutionContext context) throws RollbackTransitionException
            {
            System.out.printf("(executing '%s' transition #%d from %s to %s due to %s event)\n\n",
                    sTransitionName,
                    context.getTransitionCount() + 1,
                    stateFrom,
                    stateTo,
                    event);
            }

        /**
         * A callback when a {@link Transitions} transitions from
         * {@link Light#ON} or {@link Light#OFF}
         * to {@link Light#BROKEN}.
         */
        @OnTransition(
                fromStates = {"ON", "OFF"},
                toStates   = "BROKEN"
        )
        public void onBreakingTransition(String sTransitionName,
                Light stateFrom,
                Light stateTo,
                Event<Light> event,
                ExecutionContext context) throws RollbackTransitionException
            {
            System.out.printf("(executing '%s' transition #%d from %s to %s due to %s event)\n\n",
                    sTransitionName,
                    context.getTransitionCount() + 1,
                    stateFrom,
                    stateTo,
                    event);
            }

        /**
         * A callback when the {@link Light} state of a
         * {@link Transitions} becomes {@link Light#ON}
         */
        @OnEnterState("ON")
        public Instruction turnedOn(Light previousState,
                Light newState,
                Event<Light> event,
                ExecutionContext context)
            {
            System.out.println("The light is on.");
            m_count.incrementAndGet();

            return Instruction.NOTHING;
            }

        /**
         * A callback when the {@link Light} state of a
         * {@link Transitions} becomes {@link Light#OFF}
         */
        @OnEnterState("OFF")
        public Instruction turnedOff(Light previousState,
                Light newState,
                Event<Light> event,
                ExecutionContext context)
            {
            System.out.println("The light is off.");

            return Instruction.NOTHING;
            }

        /**
         * A callback when the {@link Light} state of a
         * {@link Transitions} becomes {@link Light#BROKEN}
         */
        @OnEnterState("BROKEN")
        public Instruction broken(Light previousState,
                Light newState,
                Event<Light> event,
                ExecutionContext context)
            {
            System.out.println("The light is broken.");

            return Instruction.NOTHING;
            }
        }
    }
