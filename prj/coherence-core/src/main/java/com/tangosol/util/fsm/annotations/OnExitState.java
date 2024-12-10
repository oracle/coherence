/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm.annotations;

import com.tangosol.util.fsm.Event;
import com.tangosol.util.fsm.ExecutionContext;
import com.tangosol.util.fsm.FiniteStateMachine;
import com.tangosol.util.fsm.StateExitAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a {@link StateExitAction} that should be executed when a
 * {@link FiniteStateMachine} leaves a specified state.
 * <p>
 * Note: The signature of the annotated method should be the same as that
 * defined by {@link StateExitAction#onExitState(Enum, Event, ExecutionContext)}
 * <p>
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnExitState
    {
    /**
     * The name of the state being exited.
     */
    String value();
    }
