/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management;

import com.tangosol.internal.http.BaseHttpHandler;

import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        extends BaseHttpHandler.BodyWriter<Map<String, Object>>, Comparable<MapJsonBodyHandler>
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
     * Return {@code true} if this {@link MapJsonBodyHandler} is enabled.
     *
     * @return {@code true} if this {@link MapJsonBodyHandler} is enabled
     */
    default boolean isEnabled()
        {
        return true;
        }

    /**
     * Return the priority of this {@link MapJsonBodyHandler}.
     * <p>
     * The priority is used to determine which instance to use if multiple
     * implementations are loaded by the service loader. The implementation
     * that returns the <b>lowest</b> priority will be used.
     * <p>
     * The default implementation of this method returns {@link #DEFAULT_PRIORITY}.
     *
     * @return the priority of this {@link MapJsonBodyHandler}
     */
    default int getPriority()
        {
        return DEFAULT_PRIORITY;
        }

    // ----- Comparable methods ---------------------------------------------

    @Override
    default int compareTo(MapJsonBodyHandler o)
        {
        return Integer.compare(getPriority(), o.getPriority());
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Load a {@link MapJsonBodyHandler} using the JVM's {@link ServiceLoader} mechanism.
     *
     * @return a {@link MapJsonBodyHandler} loaded via the {@link ServiceLoader}
     *
     * @throws IllegalStateException if no {@link MapJsonBodyHandler} can be loaded
     */
    static MapJsonBodyHandler ensureMapJsonBodyHandler()
        {
        ServiceLoader<MapJsonBodyHandler> loader = ServiceLoader.load(MapJsonBodyHandler.class);

        List<MapJsonBodyHandler> list = new ArrayList<>();
        for (MapJsonBodyHandler handler : loader)
            {
            if (handler.isEnabled())
                {
                list.add(handler);
                }
            }

        if (list.size() > 0)
            {
            list.sort(Comparator.naturalOrder());
            return list.get(0);
            }

        throw new IllegalStateException("Cannot find an instance of " + MapJsonBodyHandler.class
                + " to load via the ServiceLoader. Add the coherence-json module to the classpath.");
        }

    // ----- constants ------------------------------------------------------

    /**
     * The default priority.
     */
    int DEFAULT_PRIORITY = 0;
    }
