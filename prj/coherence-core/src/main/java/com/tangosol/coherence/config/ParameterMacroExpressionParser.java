/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.ExpressionParser;

import java.text.ParseException;

/**
 * A {@link ParameterMacroExpressionParser} is an {@link ExpressionParser} for Coherence Parameter Macros.
 *
 * @author bo  2011.10.18
 * @since Coherence 12.1.2
 */
public class ParameterMacroExpressionParser
        implements ExpressionParser
    {
    // ----- ExpressionParser interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Expression<T> parse(String sExpression, Class<T> clzResultType)
            throws ParseException
        {
        return new ParameterMacroExpression<T>(sExpression, clzResultType);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The singleton instance of the {@link ParameterMacroExpressionParser}.
     */
    public final static ExpressionParser INSTANCE = new ParameterMacroExpressionParser();
    }
