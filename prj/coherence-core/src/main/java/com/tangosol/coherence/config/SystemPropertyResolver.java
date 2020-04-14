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
 * An abstraction that allows us to make system property resolution
 * customizable.
 *
 * @author as  2019.10.11
 */
public interface SystemPropertyResolver
    {
    /**
     * Return the value of the specified system property.
     *
     * @param sPropertyName  the name of the property to return
     *
     * @return the value of the specified system property
     */
    public String getProperty(String sPropertyName);

    /**
     * Return the value of the specified system property, or the specified
     * default value if the property doesn't exist.
     *
     * @param sPropertyName  the name of the property to return
     * @param sDefaultValue  the default value to return if the property
     *                       doesn't exist
     *
     * @return the value of the specified system property, or the specified
     *         default value if the property doesn't exist
     */
    public default String getProperty(String sPropertyName, String sDefaultValue)
        {
        return Optional.ofNullable(getProperty(sPropertyName)).orElse(sDefaultValue);
        }

    /**
     * Return an instance of a {@link SystemPropertyResolver} discovered by
     * the {@code ServiceLoader}, or a default instance if none are discovered.
     *
     * @return an instance of a {@code SystemPropertyResolver}
     */
    public static SystemPropertyResolver getInstance()
        {
        ServiceLoader<SystemPropertyResolver> serviceLoader =
                ServiceLoader.load(SystemPropertyResolver.class);
        Iterator<SystemPropertyResolver> resolvers = serviceLoader.iterator();
        return resolvers.hasNext() ? resolvers.next() : new Default();
        }

    // ---- inner class: Default --------------------------------------------

    /**
     * Default {@link SystemPropertyResolver} implementation.
     * <p>
     * This implementation simply delegates to {@link System#getProperty(String)}.
     */
    class Default implements SystemPropertyResolver
        {
        @Override
        public String getProperty(String sPropertyName)
            {
            return System.getProperty(sPropertyName);
            }
        }
    }
