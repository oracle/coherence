/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;


import com.tangosol.coherence.config.builder.EvictionPolicyBuilder;
import com.tangosol.coherence.config.builder.UnitCalculatorBuilder;
import com.tangosol.coherence.config.unit.Seconds;
import com.tangosol.coherence.config.unit.Units;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.ParameterResolver;

/**
 * The {@link AbstractJournalScheme} contains functionality common to all
 * Journal schemes.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public abstract class AbstractJournalScheme<T>
        extends AbstractLocalCachingScheme<T>
    {

    /**
     * Return the EvictionPolicyBuilder used to build an EvictionPolicy.
     *
     * @return the builder
     */
    public EvictionPolicyBuilder getEvictionPolicyBuilder()
        {
        return m_bldrEvictionPolicy;
        }

    /**
     * Set the EvictionPolicyBuilder.
     *
     * @param bldr  the EvictionPolicyBuilder
     */
    @Injectable("eviction-policy")
    public void setEvictionPolicyBuilder(EvictionPolicyBuilder bldr)
        {
        m_bldrEvictionPolicy = bldr;
        }

    /**
     * Return the amount of time since the last update that entries
     * are kept by the cache before being expired. Entries that have expired
     * are not accessible and are evicted the next time a client accesses the
     * cache. Any attempt to read an expired entry results in a reloading of
     * the entry from the CacheStore.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the expiry delay
     */
    public Seconds getExpiryDelay(ParameterResolver resolver)
        {
        return m_exprExpiryDelay.evaluate(resolver);
        }

    /**
     * Set the expiry delay.
     *
     * @param expr  the expiry delay expression
     */
    @Injectable
    public void setExpiryDelay(Expression<Seconds> expr)
        {
        m_exprExpiryDelay = expr;
        }

    /**
     * Return the limit of cache size. Contains the maximum number of units
     * that can be placed  n the cache before pruning occurs. An entry is the
     * unit of measurement, unless it is overridden by an alternate unit-calculator.
     * When this limit is exceeded, the cache begins the pruning process,
     * evicting entries according to the eviction policy.  Legal values are
     * positive integers or zero. Zero implies no limit.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the high units
     */
    public Units getHighUnits(ParameterResolver resolver)
        {
        return m_exprHighUnits.evaluate(resolver);
        }

    /**
     * Set the high units.
     *
     * @param expr  the high units expression
     */
    @Injectable
    public void setHighUnits(Expression<Units> expr)
        {
        m_exprHighUnits = expr;
        }

    /**
     * Return the lowest number of units that a cache is pruned down to when
     * pruning takes place.  A pruning does not necessarily result in a cache
     * containing this number of units, however a pruning never results in a
     * cache containing less than this number of units. An entry is the unit
     * of measurement, unless it is overridden by an alternate unit-calculator.
     * When pruning occurs entries continue to be evicted according to the
     * eviction policy until this size. Legal values are positive integers or
     * zero. Zero implies the default. The default value is 75% of the high-units
     * setting (that is, for a high-units setting of 1000 the default low-units
     * is 750).
     *
     * @param resolver  the ParameterResolver
     *
     * @return the low units
     */
    public Units getLowUnits(ParameterResolver resolver)
        {
        return m_exprLowUnits.evaluate(resolver);
        }

    /**
     * Set the low units.
     *
     * @param expr  the low units
     */
    @Injectable
    public void setLowUnits(Expression<Units> expr)
        {
        m_exprLowUnits = expr;
        }

    /**
     * Return the UnitCalculatorBuilder used to build a UnitCalculator.
     *
     * @return the unit calculator
     */
    public UnitCalculatorBuilder getUnitCalculatorBuilder()
        {
        return m_bldrUnitCalculator;
        }

    /**
     * Set the UnitCalculatorBuilder.
     *
     * @param builder  the UnitCalculatorBuilder
     */
    @Injectable("unit-calculator")
    public void setUnitCalculatorBuilder(UnitCalculatorBuilder builder)
        {
        m_bldrUnitCalculator = builder;
        }

    /**
     * Return the unit-factor element specifies the factor by which the units,
     * low-units and high-units properties are adjusted. Using a BINARY unit
     * calculator, for example, the factor of 1048576 could be used to count
     * megabytes instead of bytes.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the unit factor
     */
    public int getUnitFactor(ParameterResolver resolver)
        {
        return m_exprUnitFactor.evaluate(resolver);
        }

    /**
     * Set the unit factor.
     *
     * @param expr  the unit factor expression
     */
    @Injectable
    public void setUnitFactor(Expression<Integer> expr)
        {
        m_exprUnitFactor = expr;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link UnitCalculatorBuilder}.
     */
    private UnitCalculatorBuilder m_bldrUnitCalculator;

    /**
     * The {@link EvictionPolicyBuilder}.
     */
    private EvictionPolicyBuilder m_bldrEvictionPolicy;

    /**
     * The duration that a value will live in the cache, or zero for no timeout.
     */
    private Expression<Seconds> m_exprExpiryDelay = new LiteralExpression<Seconds>(new Seconds(0));

    /**
     * The high units.
     */
    private Expression<Units> m_exprHighUnits = new LiteralExpression<Units>(new Units(0));

    /**
     * The low units.
     */
    private Expression<Units> m_exprLowUnits = new LiteralExpression<Units>(new Units(0));

    /**
     * The unit-factor.
     */
    private Expression<Integer> m_exprUnitFactor = new LiteralExpression<Integer>(Integer.valueOf(1));
    }
