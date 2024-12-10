/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.oracle.coherence.testing.SystemPropertyIsolation;
import org.junit.ClassRule;

import java.io.IOException;

import java.util.*;

import java.util.concurrent.atomic.AtomicReference;

import java.util.logging.Logger;

/**
 * Unit test support base class
 * <p/>
 *
 */
public class TestSupport
    {
    /**
     * Method description
     *
     * @return
     */
    static private JcacheTestContext internalGetJcacheTestContext()
        {
        JcacheTestContext                result      = null;
        String                           testContext = System.getProperty("TestCtx");
        ServiceLoader<JcacheTestContext> ldr         = ServiceLoader.load(JcacheTestContext.class);

        System.out.println("env TestCtx=" + testContext);

        if (testContext != null)
            {
            for (JcacheTestContext ctx : ldr)
                {
                if (ctx.getClass().getCanonicalName().toLowerCase().contains(testContext.toLowerCase()))
                    {
                    result = ctx;
                    break;
                    }
                }
            }

        // if no match, just take first context.
        if (result == null && ldr != null && ldr.iterator().hasNext())
            {
            result = ldr.iterator().next();

            if (testContext != null)
                {
                System.out.println("WARNING: could not find a JcacheTestContext that contains " + testContext
                                   + " defaulting to " + result.getClass().getCanonicalName());

                }
            }

        assert(result != null);
        System.out.println("Using JcacheTextContext=" + result.getClass().getCanonicalName());

        return result;
        }

    /**
     * Method description
     *
     * @return
     */
    static public JcacheTestContext getJcacheTestContext()
        {
        if (ctx.get() == null)
            {
            ctx.compareAndSet(null, internalGetJcacheTestContext());
            }

        return ctx.get();
        }

    protected String getTestCacheName()
        {
        return getClass().getName();
        }

    protected Class<?> getUnwrapClass(Class<?> unwrappableClass)
        {
        // contains check since null values are allowed
        if (this.unwrapClasses.containsKey(unwrappableClass))
            {
            return this.unwrapClasses.get(unwrappableClass);
            }

        final Properties unwrapProperties = getUnwrapProperties();
        final String     unwrapClassName  = unwrapProperties.getProperty(unwrappableClass.getName());

        if (unwrapClassName == null || unwrapClassName.trim().length() == 0)
            {
            this.unwrapClasses.put(unwrappableClass, null);

            return null;
            }

        try
            {
            final Class<?> unwrapClass = Class.forName(unwrapClassName);

            this.unwrapClasses.put(unwrappableClass, unwrapClass);

            return unwrapClass;
            }
        catch (ClassNotFoundException e)
            {
            LOG.warning("Failed to load unwrap class " + unwrapClassName + " for unwrappable class: "
                        + unwrappableClass);
            this.unwrapClasses.put(unwrappableClass, null);

            return null;
            }

        }

    private Properties getUnwrapProperties()
        {
        if (this.unwrapProperties != null)
            {
            return this.unwrapProperties;
            }

        final Properties unwrapProperties = new Properties();

        try
            {
            unwrapProperties.load(getClass().getResourceAsStream("/unwrap.properties"));
            }
        catch (IOException e)
            {
            LOG.warning("Failed to load unwrap.properties from classpath");
            }

        this.unwrapProperties = unwrapProperties;

        return unwrapProperties;
        }

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();

    /**
     * The logger
     */
    protected static final Logger                           LOG = Logger.getLogger(TestSupport.class.getName());
    static private final AtomicReference<JcacheTestContext> ctx = new AtomicReference<JcacheTestContext>(null);

    private final Map<Class<?>, Class<?>> unwrapClasses = Collections.synchronizedMap(new HashMap<Class<?>,
                                                              Class<?>>());
    private Properties unwrapProperties;
    }
