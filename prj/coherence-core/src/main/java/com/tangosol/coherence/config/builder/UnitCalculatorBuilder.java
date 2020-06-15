/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
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


import com.tangosol.net.cache.ConfigurableCacheMap.UnitCalculator;
import com.tangosol.net.cache.LocalCache;


/**
 * The {@link UnitCalculatorBuilder} class builds a {@link UnitCalculator}.
 *
 * @author pfm  2012.01.07
 * @since Coherence 12.1.2
 */
public class UnitCalculatorBuilder
        extends DefaultBuilderCustomization<UnitCalculator>
        implements ParameterizedBuilder<UnitCalculator>
    {
    // ----- UnitCalculatorBuilder methods  ---------------------------------

    /**
     * Return the {@link UnitCalculator} type.
     *
     * @param resolver  the {@link ParameterResolver}
     *
     * @return  the type of {@link UnitCalculator}
     */
    public String getUnitCalculatorType(ParameterResolver resolver)
        {
        return m_exprCalculator.evaluate(resolver);
        }

    /**
     * Set the {@link UnitCalculator} type.
     *
     * @param expr  the {@link UnitCalculator} type
     */
    @Injectable
    public void setUnitCalculatorType(Expression<String> expr)
        {
        m_exprCalculator = expr;
        }

    // ----- ParameterizedBuilder methods  ----------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean realizes(Class<?> clzClass, ParameterResolver resolver, ClassLoader loader)
        {
        return getClass().isAssignableFrom(clzClass);
        }

    /**
     * {@inheritDoc}
     */
    public UnitCalculator realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        UnitCalculator                       calculator = null;
        ParameterizedBuilder<UnitCalculator> bldr       = getCustomBuilder();

        if (bldr == null)
            {
            // use a built-in calculator
            String sType = getUnitCalculatorType(resolver);

            if (sType.equalsIgnoreCase("FIXED"))
                {
                calculator = LocalCache.INSTANCE_FIXED;
                }
            else if (sType.equalsIgnoreCase("BINARY"))
                {
                calculator = LocalCache.INSTANCE_BINARY;
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
     * The UnitCalculator type.
     */
    private Expression<String> m_exprCalculator = new LiteralExpression<String>("FIXED");
    }
