/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;


import com.oracle.bedrock.runtime.Platform;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A group of {@link Platform}s.
 *
 * @author jk  2020.01.29
 */
public class PlatformGroup<P extends Platform> implements Iterable<P>, Closeable
{
    /**
     * The {@link Platform}s that make up this {@link PlatformGroup}.
     */
    private final Map<String, P> platforms;

    /**
     * Construct an empty {@link PlatformGroup}.
     */
    public PlatformGroup()
    {
        this(null);
    }

    /**
     * Construct an {@link PlatformGroup} made up of the specified
     * {@link Platform}s.
     *
     * @param platforms  the {@link Platform}s that this {@link PlatformGroup} will contain
     */
    public PlatformGroup(Map<String, P> platforms)
    {
        this.platforms = new HashMap<String,P>();
        if (platforms != null)
        {
            this.platforms.putAll(platforms);
        }
    }


    /**
     * Obtain the number of {@link Platform}s in this {@link PlatformGroup}.
     *
     * @return the number of {@link Platform}s in this {@link PlatformGroup}
     */
    public int size()
    {
        return platforms.size();
    }

    /**
     * Obtain the {@link Platform} with the specified name from
     * this {@link PlatformGroup}.
     *
     * @param name  the name of the {@link Platform} to obtain
     *
     * @return the {@link Platform} with the specified name or null
     *         if this {@link PlatformGroup} contains no {@link Platform}
     *         with the specified name
     */
    @SuppressWarnings("unchecked")
    public <T extends P> T getPlatform(String name)
    {
        return (T) platforms.get(name);
    }

    /**
     * Add an existing {@link Platform} to this PlatformGroup.
     *
     * @param platform  the {@link Platform} to add
     *
     * @throws IllegalArgumentException if a {@link Platform} with the same name
     *                                  already exists in this {@link PlatformGroup}
     */
    public void addPlatform(P platform)
    {
        String name = platform.getName();
        if (platforms.containsKey(name))
        {
            throw new IllegalArgumentException("This PlatformGroup already contains a platform with the name " + name);
        }

        platforms.put(name, platform);
    }

    /**
     * Obtain an {@link Iterator} that will iterate over
     * the {@link Platform}s contained within this
     * {@link PlatformGroup}.
     *
     * @return an {@link Iterator} that will iterate over
     *         the {@link Platform}s contained within this
     *         {@link PlatformGroup}
     */
    @Override
    public Iterator<P> iterator()
    {
        return Collections.unmodifiableCollection(platforms.values()).iterator();
    }

    /**
     * Close the {@link Platform} with the specified name
     * and remove it from this {@link PlatformGroup}.
     *
     * @param name  the name of the {@link Platform} to close
     */
    public void closePlatform(String name)
    {
        P platform = platforms.remove(name);
        if (platform instanceof Closeable)
        {
            try
            {
                ((Closeable) platform).close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Close this {@link PlatformGroup} by closing all of the
     * {@link Platform}s that are contained within it that also
     * implement {@link java.io.Closeable}.
     *
     * @throws IOException if an error occurs
     *
     * @see java.io.Closeable
     */
    @Override
    public void close() throws IOException
    {
        for (P platform : platforms.values())
        {
            if (platform instanceof Closeable)
            {
                try
                {
                    ((Closeable) platform).close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        platforms.clear();
    }
}
