/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.aggregator;

import com.tangosol.coherence.dslquery.UniversalExtractorBuilder;

import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.IdentityExtractor;

import java.lang.reflect.Constructor;

import java.util.Arrays;

import static com.tangosol.util.extractor.AbstractExtractor.VALUE;

/**
 * The default implementation of {@link AggregatorFactory}.
 * <p>
 * This {@link AggregatorFactory} implementation is used for aggregators that
 * accept a single {@link ValueExtractor} argument in the constructor and
 * require no additional configuration.
 *
 * @author vp 2011.07.07
 * @author ic 2011.07.14
 */
public class DefaultAggregatorFactory
        implements AggregatorFactory
    {

    //----- constructors ----------------------------------------------------

    /**
     * Construct a DefaultAggregatorFactory instance.
     *
     * @param clzAggr the aggregator class
     */
    public DefaultAggregatorFactory(Class clzAggr)
        {
        if (clzAggr == null || !InvocableMap.EntryAggregator.class.isAssignableFrom(clzAggr))
            {
            throw new IllegalArgumentException(clzAggr
                    + " is not a valid aggregator class");
            }

        Constructor ctor = getConstructor(clzAggr.getConstructors());
        if (ctor == null)
            {
            throw new IllegalArgumentException("the aggregator class \""
                    + clzAggr.getName()
                    + "\" cannot be used with the DefaultAggregatorFactory");
            }
        m_ctorAggr = ctor;
        }

    //----- AggregatorFactory interface -------------------------------------

    /**
     * Return an aggregator instantiated by calling an aggregator class
     * constructor. The invoked constructor is matched using the following
     * rules:
     * <ul>
     *   <li>If the argument array is empty and a constructor that accepts a
     *       single {@link ValueExtractor} parameter exists, it will be
     *       invoked with an {@link IdentityExtractor} instance.
     *   </li>
     *   <li>If the argument array is empty and a constructor that accepts a
     *       single {@link ValueExtractor} parameter does not exists, the
     *       default constructor is invoked.
     *   </li>
     *   <li>If the argument array size is 1 and a constructor that accepts a
     *       single {@link ValueExtractor} parameter exists, it will be
     *       invoked with an MVEL-based ValueExtractor implementation.
     *   </li>
     *   <li>If none of the above rules are matched, an exception is thrown.</li>
     * </ul>
     *
     * @param asArgs  aggregator configuration arguments
     *
     * @return an aggregator instance
     *
     * @throws IllegalArgumentException if an appropriate constructor cannot
     *         be found
     */
    public InvocableMap.EntryAggregator getAggregator(String... asArgs)
        {
        ValueExtractor extractor = null;
        switch (asArgs.length)
            {
            case 0:
                if (m_ctorAggr.getParameterTypes().length == 1)
                    {
                    extractor = IdentityExtractor.INSTANCE;
                    }
                break;
            case 1:
                extractor = new UniversalExtractorBuilder().realize("", VALUE, asArgs[0]);
                break;
            default:
                throw new IllegalArgumentException("DefaultAggregatorFactory "
                        + "cannot be used with the given arguments: "
                        + Arrays.toString(asArgs));
            }

        return createAggregator(extractor);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create and initialize a new aggregator instance.
     *
     * @param extractor  {@link ValueExtractor} to use for a constructor
     *                   parameter
     *
     * @return an aggregator instance
     */
    protected InvocableMap.EntryAggregator createAggregator(ValueExtractor extractor)
        {
        try
            {
            Object[] ao = extractor == null ? null : new ValueExtractor[] {extractor};
            return (InvocableMap.EntryAggregator) m_ctorAggr.newInstance(ao);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Search the given constructors for a constructor that accepts a single
     * {@link ValueExtractor} parameter. If found, return the constructor;
     * otherwise return the public default constructor, if available.
     *
     * @param aCtors  constructor array
     *
     * @return constructor that accepts a single <tt>ValueExtractor</tt>
     *         parameter or, if such a constructor is not found, the public
     *         default constructor if available
     */
    protected Constructor getConstructor(Constructor[] aCtors)
        {
        Constructor ctorDefault   = null;
        Constructor ctorExtractor = null;
        for (Constructor ctor : aCtors)
            {
            Class[] aclzParams = ctor.getParameterTypes();
            int     cclzParams = aclzParams.length;
            if (cclzParams == 0)
                {
                ctorDefault = ctor;
                }
            else if (cclzParams == 1 && ValueExtractor.class.isAssignableFrom(aclzParams[0]))
                {
                ctorExtractor = ctor;
                break;
                }
            }
        return ctorExtractor == null ? ctorDefault : ctorExtractor;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Aggregator constructor.
     */
    private final Constructor m_ctorAggr;
    }
