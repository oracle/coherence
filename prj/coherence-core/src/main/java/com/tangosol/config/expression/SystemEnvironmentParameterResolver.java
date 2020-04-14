/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import com.tangosol.coherence.config.Config;

/**
 * A {@link SystemEnvironmentParameterResolver} is a {@link ParameterResolver} that is
 * an environment getter for Coherence environment properties implemented using
 * {@link System#getenv(String)}.

 * @author jf 2015.04.15
 * @since Coherence 12.2.1
 */
public class SystemEnvironmentParameterResolver implements ParameterResolver
    {
    // ----- ParameterResolver methods --------------------------------
    /**
     * Resolve Coherence system property <code>sName</code>
     * <p>
     * Log a WARNING if SecurityException occurs while accessing sName.
     *
     * @param sName system property name
     *
     * @return a {@link Parameter} representing the value of <code>sName</code> or
     * null if system property not found or if SecurityException was handled.
     */
    @Override
    public Parameter resolve(String sName)
        {
        String sValue = Config.getenv(sName);
        return sValue == null ? null : new Parameter(sName, sValue);
        }
    }
