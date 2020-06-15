/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

/**
 * Creates {@link Session}s for use by applications requiring Coherence-based
 * resources, including {@link NamedCache}s, often for specific
 * {@link ClassLoader}s, for deployed modules.
 *
 * @see Session
 *
 * @author bo 2015.07.27
 */
public interface SessionProvider
    {
    // ----- SessionProvider methods ----------------------------------------

    /**
     * Create a {@link Session} using the specified {@link Option}s.
     *
     * @param options  the {@link Session.Option}s for creating the {@link Session}
     *
     * @return a new {@link Session}
     *
     * @throws IllegalArgumentException
     *              when a {@link Session} can't be creating using the
     *              specified {@link Option}.
     */
    Session createSession(Session.Option... options);

    // ----- Option interface -----------------------------------------------

    /**
     * An immutable option for creating and configuring {@link SessionProvider}s.
     */
    interface Option
        {
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Acquire the {@link SessionProvider} based on the current calling context
     * and the provided {@link Option}s.
     *
     * @param options  the {@link Option}s for acquiring the {@link SessionProvider}
     *
     * @return a {@link SessionProvider}
     *
     * @throws IllegalArgumentException
     *              when a {@link SessionProvider} can't be acquired using the
     *              specified {@link Option}s
     *
     * @throws IllegalStateException
     *              when a {@link SessionProvider} can't be auto-detected
     */
    static SessionProvider get(Option... options)
        {
        return CacheFactory.getCacheFactoryBuilder();
        }
    }
