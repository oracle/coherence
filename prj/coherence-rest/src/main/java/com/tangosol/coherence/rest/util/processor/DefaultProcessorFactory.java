/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.processor;

import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap;

import java.lang.reflect.Constructor;

import java.util.Arrays;

/**
 * The default implementation of {@link ProcessorFactory}.
 * <p>
 * This {@link ProcessorFactory} implementation is used for processors that
 * require a single {@link String} argument in the constructor and
 * require no additional configuration.
 *
 * @author par 2012.03.13
 *
 * @since Coherence 3.7.1
 */
public class DefaultProcessorFactory
        implements ProcessorFactory
    {

    //----- constructors ----------------------------------------------------

    /**
     * Construct a DefaultProcessorFactory instance.
     *
     * @param clzProcessor  the processor class
     */
    public DefaultProcessorFactory(Class clzProcessor)
        {
        if (clzProcessor == null || 
            !InvocableMap.EntryProcessor.class.isAssignableFrom(clzProcessor))
            {
            throw new IllegalArgumentException(clzProcessor
                    + " is not a valid processor class");
            }

        Constructor ctor = getConstructor(clzProcessor.getConstructors());
        if (ctor == null)
            {
            throw new IllegalArgumentException("the processor class \""
                    + clzProcessor.getName()
                    + "\" cannot be used with the DefaultProcessorFactory");
            }
        m_ctorProcessor = ctor;
        }

    //----- ProcessorFactory interface -------------------------------------

    /**
     * Return a processor instantiated by calling a processor class
     * constructor.
     *
     * @param asArgs  configuration arguments
     *
     * @return a processor instance
     *
     * @throws IllegalArgumentException if an appropriate constructor cannot
     *         be found
     */
    public InvocableMap.EntryProcessor getProcessor(String... asArgs)
        {
        String sProperty = null;
        switch (asArgs.length)
            {
            case 0:
                break;
            case 1:
                sProperty = asArgs[0];
                break;
            default:
                throw new IllegalArgumentException("DefaultProcessorFactory "
                        + "cannot be used with the given arguments: "
                        + Arrays.toString(asArgs));
            }

        return createProcessor(sProperty);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create and initialize a new processor instance.
     *
     * @param sProperty  property to be handled by processor
     *
     * @return a processor instance
     */
    protected InvocableMap.EntryProcessor createProcessor(String sProperty)
        {
        try
            {
            return sProperty == null
                    ? (InvocableMap.EntryProcessor) m_ctorProcessor.newInstance()
                    : (InvocableMap.EntryProcessor) m_ctorProcessor.newInstance(sProperty);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Search the given constructors for a constructor that accepts a
     * {@link String} parameter.  If found, return the constructor,
     * otherwise return the public default constructor, if available.
     *
     * @param aCtors  constructor array
     *
     * @return default constructor
     */
    protected Constructor getConstructor(Constructor[] aCtors)
        {
        Constructor ctorDefault   = null;
        Constructor ctorProperty  = null;
        for (Constructor ctor : aCtors)
            {
            Class[] aclzParams = ctor.getParameterTypes();
            int     cclzParams = aclzParams.length;
            if (cclzParams == 0)
                {
                ctorDefault = ctor;
                }
             else if (cclzParams == 1 
                     && String.class.isAssignableFrom(aclzParams[0]))
                {
                ctorProperty = ctor;
                break;
                }
          
            }
        return ctorProperty == null ? ctorDefault : ctorProperty;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Processor constructor.
     */
    private final Constructor m_ctorProcessor;
    }
