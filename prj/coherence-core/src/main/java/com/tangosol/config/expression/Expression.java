/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

/**
 * A {@link Expression} represents a calculation to be evaluated at runtime, during which, one or more
 * {@link Parameter}s may be required.
 *
 * @param <T>  the type of value returned when the {@link Expression} is evaluated
 *
 * @author bo  2011.06.24
 * @since Coherence 12.1.2
 */
public interface Expression<T>
    {
    /**
     * Evaluates the {@link Expression} to produce a value of type T.
     *
     * @param resolver  the {@link ParameterResolver} for resolving any parameters used by the {@link Expression}
     *
     * @return The result of evaluating the expression
     */
    public T evaluate(ParameterResolver resolver);
    }
