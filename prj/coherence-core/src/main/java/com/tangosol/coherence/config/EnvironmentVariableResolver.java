/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;


import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;


/**
 * An abstraction that allows us to make environment variable resolution
 * customizable.
 *
 * @author as  2019.10.11
 */
public interface EnvironmentVariableResolver
    {
    /**
     * Return the value of the specified environment variable.
     *
     * @param sVarName  the name of the environment variable to return
     *
     * @return the value of the specified environment variable
     */
    public String getEnv(String sVarName);

    /**
     * Return the value of the specified environment variable, or the specified
     * default value if the variable doesn't exist.
     *
     * @param sVarName       the name of the environment variable to return
     * @param sDefaultValue  the default value to return if the variable
     *                       doesn't exist
     *
     * @return the value of the specified environment variable, or the specified
     *         default value if the variable doesn't exist
     */
    public default String getEnv(String sVarName, String sDefaultValue)
        {
        return Optional.ofNullable(getEnv(sVarName)).orElse(sDefaultValue);
        }

    /**
     * Return an instance of a {@link EnvironmentVariableResolver} discovered by
     * the {@code ServiceLoader}, or a default instance if none are discovered.
     *
     * @return an instance of a {@code EnvironmentVariableResolver}
     */
    public static EnvironmentVariableResolver getInstance()
        {
        ServiceLoader<EnvironmentVariableResolver> serviceLoader =
                ServiceLoader.load(EnvironmentVariableResolver.class);
        Iterator<EnvironmentVariableResolver> resolvers = serviceLoader.iterator();
        return resolvers.hasNext() ? resolvers.next() : new Default();
        }

    // ---- inner class: Default --------------------------------------------

    /**
     * Default {@link EnvironmentVariableResolver} implementation.
     * <p>
     * This implementation simply delegates to {@link System#getenv(String)}.
     */
    class Default implements EnvironmentVariableResolver
        {
        @Override
        public String getEnv(String sVarName)
            {
            return System.getenv(sVarName);
            }
        }
    }
