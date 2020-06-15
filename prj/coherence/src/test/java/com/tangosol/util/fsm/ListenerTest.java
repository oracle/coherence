/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;
import com.tangosol.util.fsm.misc.Motor;
import com.tangosol.util.fsm.misc.MotorEvent;
import com.tangosol.util.fsm.misc.TraceAction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.testsupport.deferred.Eventually.within;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link FiniteStateMachineListener}s.
 *
 * @author pfm  2013.04.10
 * @author bko  2015.07.21
 */
public class ListenerTest
    {
    /**
     * Setup a Finite State Machine for testing.
     */
    @Before
    public void setup()
        {
        SimpleModel<Motor> model = new SimpleModel<Motor>(Motor.class);

        model.addTransition(new Transition<>("Turn On", Motor.STOPPED, Motor.RUNNING, new TraceAction<>()));
        model.addTransition(new Transition<>("Turn Off", Motor.RUNNING, Motor.STOPPED, new TraceAction<>()));

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setName(
                "NonBlockingFiniteStateMachineAnnotationTest:LifecycleTest");

        machine = new NonBlockingFiniteStateMachine<>("Motor",
                                                      model,
                                                      Motor.STOPPED,
                                                      deps,
                                                      true,
                                                      null);
        }

    /**
     * Cleanup the Finite State Machine used for testing.
     */
    @After
    public void cleanup()
        {
        try
            {
            machine.stop();
            }
        catch (IllegalStateException e)
            {
            // we don't care if we can't stop it if it's already stopped or
            // hasn't started
            }
        }

    /**
     * Ensure multiple-listeners are notified when a Finite State Machine
     * performs transitions.
     */
    @Test
    public void shouldObserveTransitionsWithMultipleListeners()
        {
        // test that 2 listeners are notified
        TestListener listener1 = new TestListener(Motor.STOPPED, Motor.RUNNING);
        TestListener listener2 = new TestListener(Motor.STOPPED, Motor.RUNNING);

        machine.addListener(listener1);
        machine.addListener(listener2);

        assertThat(machine.start(), is(true));

        machine.process(MotorEvent.TURN_ON);

        Eventually.assertDeferred(listener1::isMatch, is(true), within(10,
                                                                              TimeUnit.SECONDS));
        Eventually.assertDeferred(listener2::isMatch, is(true), within(10,
                                                                              TimeUnit.SECONDS));
        listener1.reset();
        listener2.reset();

        machine.process(MotorEvent.TURN_OFF);

        // test again after transition to original state
        machine.process(MotorEvent.TURN_ON);

        Eventually.assertDeferred(() -> listener1.isMatch(), is(true), within(10, TimeUnit.SECONDS));
        Eventually.assertDeferred(() -> listener2.isMatch(), is(true), within(10,
                                                                              TimeUnit.SECONDS));
        listener1.reset();
        listener2.reset();

        machine.process(MotorEvent.TURN_OFF);

        // test remove
        machine.removeListener(listener1);

        machine.process(MotorEvent.TURN_ON);

        Eventually.assertDeferred(() -> listener2.isMatch(), is(true));

        assertThat(listener1.isMatch(), is(false));
        }

    // ----- inner class TestListener ---------------------------------------

    /**
     * TestListener will validate that the event was properly delivered.
     */
    public static class TestListener
            implements FiniteStateMachineListener<Motor>
        {
        public TestListener(Motor previousState, Motor nextState)
            {
            m_expectedPreviousState = previousState;
            m_expectedNextState     = nextState;
            }

        @Override
        public void onTransition(Motor stateFrom, Motor stateTo)
            {
            System.out.println("Listener received:" + stateFrom + ", " + stateTo);

            m_fMatch = stateFrom == m_expectedPreviousState && stateTo == m_expectedNextState;
            }

        public boolean isMatch()
            {
            return m_fMatch;
            }

        public void reset()
            {
            m_fMatch = false;
            }

        private Motor m_expectedPreviousState;
        private Motor m_expectedNextState;
        private volatile boolean m_fMatch;
        }

    /**
     * The {@link FiniteStateMachine} for testing.
     */
    private NonBlockingFiniteStateMachine<Motor> machine;
    }
