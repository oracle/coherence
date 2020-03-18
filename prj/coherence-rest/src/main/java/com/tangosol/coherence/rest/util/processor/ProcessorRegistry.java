/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.processor;

import com.tangosol.coherence.rest.config.ProcessorConfig;

import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.SafeHashMap;

import java.util.Collection;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A registry for {@link ProcessorFactory} instances.
 *
 * @author vp 2011.07.08
 */
public class ProcessorRegistry
    {

    //----- constructors -----------------------------------------------------

    /**
     * Construct an ProcessorRegistry.
     * <p>
     * By default the following built-in processors will be registered:
     * <ul>
     *   <li>increment</li>
     *   <li>multiply</li>
     *   <li>post-increment</li>
     *   <li>post-multiply</li>
     * </ul>
     */
    public ProcessorRegistry()
        {
        register("increment",      new NumberIncrementorFactory(false));
        register("multiply",       new NumberMultiplierFactory(false));
        register("post-increment", new NumberIncrementorFactory(true));
        register("post-multiply",  new NumberMultiplierFactory(true));
        }

    /**
     * Construct an ProcessorRegistry that includes built-in processors in
     * addition to the specified processors.
     *
     * @param colConfig processor configurations
     */
    public ProcessorRegistry(Collection<ProcessorConfig> colConfig)
        {
        this();
        register(colConfig);
        }

    //----- ProcessorRegistry methods ---------------------------------------

    /**
     * Returns a configured processor.
     *
     * @param sRequest the processor request
     *
     * @return processor
     */
    public InvocableMap.EntryProcessor getProcessor(String sRequest)
        {
        Matcher m = PROCESSOR_REQUEST_PATTERN.matcher(sRequest);
        if (!m.matches())
            {
            throw new IllegalArgumentException("bad processor request syntax: "
                    + sRequest);
            }

        String sName = m.group(1);
        String sArgs = m.group(2);

        ProcessorFactory factory = m_mapRegistry.get(sName);
        if (factory == null)
            {
            throw new IllegalArgumentException("missing factory or processor: "
                    + sName);
            }

        String[] asArgs = new String[0];
        if (sArgs != null && sArgs.length() > 0)
            {
            asArgs = sArgs.split(",");
            for (int i = 0, c = asArgs.length; i < c; ++i)
                {
                asArgs[i] = asArgs[i].trim();
                }
            }
        return factory.getProcessor(asArgs);
        }

    /**
     * Registers a processor factory with the given name.
     *
     * @param sName   the processor name
     * @param factory the processor factory
     */
    public void register(String sName, ProcessorFactory factory)
        {
        m_mapRegistry.put(sName, factory);
        }

    /**
     * Registers processor factory with the given name.
     * <p>
     * A {@link DefaultProcessorFactory} will be used if the <code>clz</code>
     * parameter is a class that implements InvocableMap.EntryProcessor.
     *
     * @param sName  the processor name
     * @param clz    the processor or processor factory class
     */
    public void register(String sName, Class clz)
        {
        try
            {
            ProcessorFactory factory;
            if (ProcessorFactory.class.isAssignableFrom(clz))
                {
                factory = (ProcessorFactory) clz.newInstance();
                }
            else if (InvocableMap.EntryProcessor.class.isAssignableFrom(clz))
                {
                factory = new DefaultProcessorFactory(clz);
                }
            else
                {
                throw new IllegalArgumentException(clz.getName()
                        + " is not an EntryProcessor or ProcessorFactory");
                }
            register(sName, factory);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Register a collection of processors.
     *
     * @param colConfig  the processor configurations
     */
    public void register(Collection<ProcessorConfig> colConfig)
        {
        for (ProcessorConfig config : colConfig)
            {
            register(config.getProcessorName(), config.getProcessorClass());
            }
        }


    // ----- constants ------------------------------------------------------

    /**
     * Regex pattern that defines the processor request syntax.
     */
    public static final String PROCESSOR_REQUEST_REGEX
            = "\\s*(\\w(?:\\w|-)*)\\((.*)\\)";

    /**
     * Regex pattern that defines the processor request syntax.
     */
    private static final Pattern PROCESSOR_REQUEST_PATTERN
            = Pattern.compile("^" + PROCESSOR_REQUEST_REGEX);

    // ----- data members ---------------------------------------------------

    /**
     * Registry of the processor factories.
     */
    private Map<String, ProcessorFactory> m_mapRegistry = new SafeHashMap<>();
    }
