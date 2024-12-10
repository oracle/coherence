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
 * Defines the transitions for a {@link FiniteStateMachine}.
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transitions
    {
    /**
     * The {@link Transition}s for the {@link FiniteStateMachine}.
     */
    Transition[] value();
    }
