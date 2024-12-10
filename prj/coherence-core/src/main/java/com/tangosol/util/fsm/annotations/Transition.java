/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm.annotations;

import com.tangosol.util.fsm.FiniteStateMachine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a transition for a {@link FiniteStateMachine}.
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 *
 * @see FiniteStateMachine
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transition
    {
    /**
     * The (optional) name for the {@link Transition}.
     * <p>
     * This name is for display and logging purposes only.  It does not need
     * to be unique.  When not specified a name will automatically be
     * generated.
     */
    String name() default "";

    /**
     * The name of the starting states for a {@link Transition}.
     */
    String[] fromStates();

    /**
     * The name of the ending state for a {@link Transition}.
     */
    String toState();
    }
