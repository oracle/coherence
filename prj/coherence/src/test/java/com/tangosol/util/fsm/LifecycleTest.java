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

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link FiniteStateMachine} lifecycle.
 *
 * @author pfm  2013.04.10
 * @author bko  2015.07.21
 */
public class LifecycleTest
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
     * Ensure that a Finite State Machine starts in the correct states.
     */
    @Test
    public void shouldStartFiniteStateMachine()
        {
        assertThat(machine.getState(), is(nullValue()));

        assertThat(machine.start(), is(true));

        machine.process(MotorEvent.TURN_ON);

        Eventually.assertDeferred(machine::getState, is(Motor.RUNNING));

        assertThat(machine.start(), is(false));
        }

    /**
     * Ensure that a Finite State Machine stops.
     */
    @Test
    public void shouldStopFiniteStateMachine()
        {
        assertThat(machine.start(), is(true));

        assertThat(machine.stop(), is(true));

        machine.process(MotorEvent.TURN_ON);

        assertThat(machine.stop(), is(false));
        }

    /**
     * Ensure that a Finite State Machine can't be restarted.
     */
    @Test(expected = IllegalStateException.class)
    public void shouldNotRestartFiniteStateMachine()
        {
        assertThat(machine.start(), is(true));

        assertThat(machine.stop(), is(true));

        machine.start();
        }

    /**
     * Ensure that a Finite State Machine can't be stopped if it hasn't started.
     */
    @Test(expected = IllegalStateException.class)
    public void shouldNotStopAnUnstartedFiniteStateMachine()
        {
        machine.stop();
        }

    /**
     * The {@link FiniteStateMachine} for testing.
     */
    private NonBlockingFiniteStateMachine<Motor> machine;
    }
