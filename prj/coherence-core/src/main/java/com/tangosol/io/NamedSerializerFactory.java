/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.OperationalContext;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.util.Map;

/**
 * A factory that produces {@link Serializer} instances for a given name.
 *
 * @author Jonathan Knight  2020.09.22
 * @since 20.06
 */
public interface NamedSerializerFactory
    {
    /**
     * Produces instances of a named {@link Serializer}.
     *
     * @param sName   the name of the serializer
     * @param loader  the {@link ClassLoader} to use to create a {@link Serializer}
     *
     * @return an instance of a named {@link Serializer}
     *
     * @throws NullPointerException     if the name parameter is null
     * @throws IllegalArgumentException if no serializer is discoverable with the specified name
     */
    Serializer getNamedSerializer(String sName, ClassLoader loader);

    /**
     * The default implementation of {@link NamedSerializerFactory} that looks up
     * {@link Serializer} instances by name in the Coherence operational configuration.
     */
    NamedSerializerFactory DEFAULT = (sName, loader) ->
        {
        if (sName == null || sName.trim().isEmpty())
            {
            // no name specified so return the default serializer.
            return ExternalizableHelper.ensureSerializer(loader);
            }

        Map<String, SerializerFactory> map     = ((OperationalContext) CacheFactory.getCluster()).getSerializerMap();
        SerializerFactory              factory = map.get(sName);
        if (factory == null)
            {
            throw new IllegalStateException("Cannot find a serializer configuration for name '" + sName + "'");
            }
        return factory.createSerializer(loader);
        };
    }
