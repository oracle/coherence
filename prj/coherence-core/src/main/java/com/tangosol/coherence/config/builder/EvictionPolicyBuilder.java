/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder.ReflectionSupport;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.ParameterResolver;


import com.tangosol.net.cache.ConfigurableCacheMap.EvictionPolicy;
import com.tangosol.net.cache.LocalCache;


/**
 * The {@link EvictionPolicyBuilder} builds a {@link EvictionPolicy}.
 *
 * @author pfm 2012.01.07
 * @since Coherence 12.1.2
 */
public class EvictionPolicyBuilder
        extends DefaultBuilderCustomization<EvictionPolicy>
        implements ParameterizedBuilder<EvictionPolicy>, ReflectionSupport
    {
    // ----- EvictionPolicyBuilder methods  ---------------------------------

    /**
     * Obtains the EvictionPolicy type.
     *
     * @param resolver  the {@link ParameterResolver}
     *
     * @return  the type of EvictionPolicy
     */
    public String getEvictionType(ParameterResolver resolver)
        {
        return m_exprPolicy.evaluate(resolver);
        }

    /**
     * Set the EvictionPolicy type.
     *
     * @param expr  the EvictionPolicy type
     */
    @Injectable
    public void setEvictionType(Expression<String> expr)
        {
        m_exprPolicy = expr;
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
    public EvictionPolicy realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        EvictionPolicy                       policy;
        ParameterizedBuilder<EvictionPolicy> bldr = getCustomBuilder();

        if (bldr == null)
            {
            // use a Coherence built-in EvictionPolicy
            String sType = getEvictionType(resolver).toUpperCase();

            switch (sType)
                {
                case "HYBRID":
                    policy = LocalCache.INSTANCE_HYBRID;
                    break;
                case "LRU":
                    policy = LocalCache.INSTANCE_LRU;
                    break;
                case "LFU":
                    policy = LocalCache.INSTANCE_LFU;
                    break;
                default:
                    throw new IllegalArgumentException(
                        "Error: the <eviction-policy> value " + sType +
                            "is invalid");
                }
            }
        else
            {
            // create a custom Eviction
            policy = bldr.realize(resolver, loader, listParameters);
            }

        return policy;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link EvictionPolicy}.
     */
    private Expression<String> m_exprPolicy = new LiteralExpression("HYBRID");
    }
