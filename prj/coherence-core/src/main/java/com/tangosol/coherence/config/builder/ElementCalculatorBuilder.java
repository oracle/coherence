/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.topic.BinaryElementCalculator;

import com.tangosol.net.topic.FixedElementCalculator;

import com.tangosol.net.topic.NamedTopic.ElementCalculator;


/**
 * The {@link ElementCalculatorBuilder} class builds an {@link ElementCalculator}.
 *
 * @author Jonathan Knight  2021.05.17
 * @since 21.06
 */
public class ElementCalculatorBuilder
        extends DefaultBuilderCustomization<ElementCalculator>
        implements ParameterizedBuilder<ElementCalculator>
    {
    // ----- ElementCalculatorBuilder methods  ---------------------------------

    /**
     * Returns the {@link ElementCalculator} type.
     *
     * @param resolver  the {@link ParameterResolver}
     *
     * @return the type of {@link ElementCalculator}
     */
    public String getElementCalculatorType(ParameterResolver resolver)
        {
        return m_exprCalculator.evaluate(resolver);
        }

    /**
     * Set the {@link ElementCalculator} type.
     *
     * @param expr  the {@link ElementCalculator} type
     */
    @Injectable
    public void setElementCalculatorType(Expression<String> expr)
        {
        m_exprCalculator = expr;
        }

    /**
     * Returns {@code true} if this builder realizes the specified type.
     *
     * @param clzClass  the required type
     * @param resolver  the parameter resolver to use
     * @param loader    the classloader
     *
     * @return {@code true} if this builder realizes the specified type.
     */
    public boolean realizes(Class<?> clzClass, ParameterResolver resolver, ClassLoader loader)
        {
        return getClass().isAssignableFrom(clzClass);
        }

    // ----- ParameterizedBuilder methods  ----------------------------------

    @Override
    public ElementCalculator realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        ElementCalculator                       calculator = null;
        ParameterizedBuilder<ElementCalculator> bldr       = getCustomBuilder();

        if (bldr == null)
            {
            // use a built-in calculator
            String sType = getElementCalculatorType(resolver);

            if (sType.equalsIgnoreCase("FIXED"))
                {
                calculator = FixedElementCalculator.INSTANCE;
                }
            else if (sType.equalsIgnoreCase("BINARY"))
                {
                calculator = BinaryElementCalculator.INSTANCE;
                }
            }
        else
            {
            // create a custom calculator
            calculator = bldr.realize(resolver, loader, listParameters);
            }

        return calculator;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ElementCalculator} type.
     */
    private Expression<String> m_exprCalculator = new LiteralExpression<String>("BINARY");
    }
