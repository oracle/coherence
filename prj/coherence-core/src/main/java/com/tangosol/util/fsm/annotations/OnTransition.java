/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm.annotations;

import com.tangosol.util.fsm.Event;
import com.tangosol.util.fsm.ExecutionContext;
import com.tangosol.util.fsm.Transition;
import com.tangosol.util.fsm.TransitionAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines an {@link TransitionAction} to be performed as part of a {@link Transition}.
 * <p>
 * Note: The signature of the annotated method should be the same as that
 * defined by {@link TransitionAction#onTransition(String, Enum, Enum, Event, ExecutionContext)}
 * <p>
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnTransition
    {
    /**
     * The name of the states from which the {@link Transition} may occur.
     */
    String[] fromStates();

    /**
     * The name of the states to which the {@link Transition} will occur.
     */
    String[] toStates();
    }
