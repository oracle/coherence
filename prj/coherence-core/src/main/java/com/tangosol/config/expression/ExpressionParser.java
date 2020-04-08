/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import java.text.ParseException;

/**
 * An {@link ExpressionParser} parses a {@link String} representation of some calculation to produce an
 * {@link Expression}, that of which when evaluated will return an expected type of value.
 *
 * @author bo  2011.10.18
 * @since Coherence 12.1.2
 */
public interface ExpressionParser
    {
    /**
     * Attempts to parse the provided {@link String} to produce an {@link Expression} of an expected type.
     *
     * @param sExpression    the {@link String} representation of the {@link Expression}
     * @param clzResultType  the type of value the {@link Expression} will return when evaluated
     *
     * @return an {@link Expression} that will when evaluated will produce the required type
     *
     * @throws ParseException when an error occurred attempting to parse the {@link String}
     */
    public <T> Expression<T> parse(String sExpression, Class<T> clzResultType)
            throws ParseException;
    }
