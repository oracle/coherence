/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management;

import com.tangosol.internal.http.BaseHttpHandler;

import java.io.InputStream;
import java.io.OutputStream;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * A class that can serialize and deserialize {@link Map Maps}
 * to and from json.
 *
 * @author Jonathan Knight  2022.01.25
 * @since 22.06
 */
public interface MapJsonBodyHandler
        extends BaseHttpHandler.BodyWriter<Map<String, Object>>
    {
    @Override
    void write(Map<String, Object> body, OutputStream out);

    /**
     * Read a json {@link InputStream} and deserialize into a
     * {@link Map}.
     *
     * @param in  the {@link InputStream} containing json data
     *
     * @return a {@link Map} of the deserialized json data
     */
    Map<String, Object> readMap(InputStream in);

    /**
     * Load a {@link MapJsonBodyHandler} using the JVM's {@link ServiceLoader} mechanism.
     *
     * @return a {@link MapJsonBodyHandler} loaded via the {@link ServiceLoader}
     *
     * @throws IllegalStateException if no {@link MapJsonBodyHandler} can be loaded
     */
    static MapJsonBodyHandler ensureMapJsonBodyHandler()
        {
        ServiceLoader<MapJsonBodyHandler> loader   = ServiceLoader.load(MapJsonBodyHandler.class);
        Iterator<MapJsonBodyHandler>      iterator = loader.iterator();

        if (iterator.hasNext())
            {
            return iterator.next();
            }

        throw new IllegalStateException("Cannot find an instance of " + MapJsonBodyHandler.class
                + " to load via the ServiceLoader. Add the coherence-json module to the classpath.");
        }
    }
