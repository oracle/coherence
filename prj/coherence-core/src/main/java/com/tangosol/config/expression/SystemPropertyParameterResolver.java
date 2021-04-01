/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import com.tangosol.coherence.config.Config;

/**
 * A {@link SystemPropertyParameterResolver} is a {@link ParameterResolver} that is
 * based on property methods on {@link System}.
 * <p>
 * Replaces {@link com.tangosol.config.expression.PropertiesParameterResolver(java.util.Map)} since it required
 * read and write access for all Properties for simple property access.
 * {@link SystemPropertyParameterResolver#resolve(String)} only requires read access for property specified.
 *
 * @author jf 2015.04.15
 * @since Coherence 12.2.1
 */
public class SystemPropertyParameterResolver
        implements ParameterResolver
    {
    // ----- ParameterResolver methods --------------------------------------

    /**
     * Resolve Coherence system property <code>sName</code>
     *
     * @param sName system property name
     *
     * @return a {@link Parameter} representing the value of <code>sName</code> or
     * null if system property not found or if SecurityException was handled.
     */
    @Override
    public Parameter resolve(String sName)
        {
        String sValue = Config.getProperty(sName);

        return sValue == null ? null : new Parameter(sName, sValue);
        }

    // ----- SystemPropertyParameterResolver methods ------------------------

    /**
     * Return property's value as requested type.
     *  
     *   @param sName         property name
     *   @param clzTypeValue  coerce system property's value from string to instance of this class
     *   @param <T>           property value's target type
     *   @return              null if property has no value or return property's value coerced from string
     *                        to requested type.
     *  
     *   Throws exceptions listed in {@link Value#as(Class)} when coercion fails.
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(String sName, Class<T> clzTypeValue)
        {
        Parameter p = resolve(sName);

        return p == null ? null : p.evaluate(this).as(clzTypeValue);
        }

    //----- constants -------------------------------------------------------
    /**
     * This singleton instance of the {@link SystemPropertyParameterResolver}.
     */
    public static final SystemPropertyParameterResolver INSTANCE = new SystemPropertyParameterResolver();

    }
