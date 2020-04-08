/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package session;

import com.tangosol.coherence.component.util.SafeNamedCache;
import com.tangosol.net.CoherenceSession;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import common.AbstractFunctionalTest;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

/**
 * Functional Tests for {@link Session}s.
 *
 * @author bo 2015.07.27
 */
public class SessionTest
        extends AbstractFunctionalTest
    {
    /**
     * Ensure we can auto-detect and create a {@link Session} against the
     * implicit deployment.
     */
    @Test
    public void shouldAutoDetectSession()
            throws Exception
        {
        try (Session session = Session.create())
            {
            assertThat(session, is(not(nullValue())));
            }
        }

    /**
     * Ensure we can auto-detect and create a {@link Session} against the
     * implicit deployment and then get some resources from it.
     */
    @Test
    public void shouldAcquireNamedCacheFromSession()
            throws Exception
        {
        try (Session session = Session.create())
            {
            assertThat(session, is(not(nullValue())));

            NamedCache<String, String> namedCache = session.getCache("my-cache");

            assertThat(namedCache.isActive(), is(true));
            assertThat(namedCache, is(not(nullValue())));
            }
        }

    /**
     * Ensure we can create a {@link Session} using {@link CoherenceSession}.
     */
    @Test
    public void shouldCreateSession()
            throws Exception
        {
        try (Session session = new CoherenceSession())
            {
            assertThat(session, is(not(nullValue())));

            NamedCache namedCache = session.getCache("my-cache");

            assertThat(namedCache, is(not(nullValue())));
            }
        }

    /**
     * Ensure that a {@link Session} returns the same {@link NamedCache} instance
     * when asked for the same name / type assertion.
     */
    @Test
    public void shouldReturnSameNamedCache()
            throws Exception
        {
        try (Session session = new CoherenceSession())
            {
            NamedCache namedCache = session.getCache("my-cache");

            NamedCache anotherCache = session.getCache("my-cache");

            assertThat(namedCache, is(anotherCache));
            }
        }

    /**
     * Ensure that a {@link Session} doesn't return a released {@link NamedCache}
     * after it is released.
     */
    @Test
    public void shouldNotReturnReleasedNamedCache()
            throws Exception
        {
        try (Session session = new CoherenceSession())
            {
            NamedCache namedCache = session.getCache("my-cache");

            namedCache.release();

            NamedCache anotherCache = session.getCache("my-cache");

            // the first cache should be inactive (released)
            assertThat(namedCache.isActive(), is(false));

            // the second cache should be active
            assertThat(anotherCache.isActive(), is(true));

            // and not the same as the second cache
            assertThat(namedCache, is(not(anotherCache)));
            }
        }

    /**
     * Ensure that a {@link Session} doesn't return a destroyed {@link NamedCache}
     * after it is destroyed.
     */
    @Test
    public void shouldNotReturnDestroyedNamedCache()
            throws Exception
        {
        try (Session session = new CoherenceSession())
            {
            NamedCache namedCache = session.getCache("my-cache");

            namedCache.destroy();

            NamedCache anotherCache = session.getCache("my-cache");

            // the first cache should be inactive (destroyed)
            assertThat(namedCache.isActive(), is(false));

            // and not the same as the second cache
            assertThat(namedCache, is(not(anotherCache)));
            }
        }

    /**
     * Ensure that closing a {@link Session} releases acquired {@link NamedCache}s.
     */
    @Test
    public void shouldReleaseNamedCacheWhenSessionClosed()
            throws Exception
        {
        NamedCache namedCache;

        try (Session session = new CoherenceSession())
            {
            namedCache = session.getCache("my-cache");

            // the cache should be active
            assertThat(namedCache.isActive(), is(true));
            }

        // the cache should be inactive
        assertThat(namedCache.isActive(), is(false));
        }

    /**
     * Ensure that two {@link Session}s produce different {@link NamedCache}
     * instances for the same named cache.
     * @throws Exception
     */
    @Test
    public void shouldReturnDifferentNamedCachesForDifferentSessions()
            throws Exception
        {
        try (Session session1 = new CoherenceSession(); Session session2 = new CoherenceSession())
            {
            NamedCache namedCache1 = session1.getCache("my-cache");
            NamedCache namedCache2 = session2.getCache("my-cache");

            assertThat(namedCache1, is(not(namedCache2)));

            namedCache1.release();

            assertThat(namedCache1.isActive(), is(false));
            assertThat(namedCache2.isActive(), is(true));

            namedCache2.release();

            assertThat(namedCache1.isActive(), is(false));
            assertThat(namedCache2.isActive(), is(false));
            }
        }


    /**
     * Ensure that closing a {@link Session} releases acquired {@link NamedCache}s
     * and doesn't effect {@link NamedCache}s from other {@link Session}s.
     */
    @Test
    public void shouldReleaseNamedCacheWhenSessionClosedButNotEffectOtherSessions()
            throws Exception
        {
        try (Session session1 = new CoherenceSession(); Session session2 = new CoherenceSession())
            {
            NamedCache namedCache1 = session1.getCache("my-cache");
            NamedCache namedCache2 = session2.getCache("my-cache");

            assertThat(namedCache1, is(not(namedCache2)));

            assertThat(namedCache1.isActive(), is(true));
            assertThat(namedCache2.isActive(), is(true));

            session1.close();

            assertThat(namedCache1.isActive(), is(false));
            assertThat(namedCache2.isActive(), is(true));

            namedCache2.release();

            assertThat(namedCache1.isActive(), is(false));
            assertThat(namedCache2.isActive(), is(false));
            }
        }

    /**
     * Ensure that two {@link Session}s produce different {@link NamedCache}
     * instances for the same named cache and that if one cache is destroyed,
     * and if Session.getCache is called on the second session, the cache should
     * be ensured.
     * @throws Exception
     */
    @Test
    public void shouldNotReturnInactiveCacheWhenNamedCacheDestroyedByOtherSession()
            throws Exception
        {
        try (Session session1 = new CoherenceSession(); Session session2 = new CoherenceSession())
            {
            NamedCache namedCache1 = session1.getCache("my-cache");
            NamedCache namedCache2 = session2.getCache("my-cache");

            assertThat(namedCache1, is(not(namedCache2)));

            // share same internal NamedCache
            assertThat(namedCache1.as(SafeNamedCache.class), is(namedCache2.as(SafeNamedCache.class)));

            namedCache1.destroy();

            // both session NC considered destroyed since shared internal NC
            assertThat(namedCache1.isDestroyed(), is(Boolean.TRUE));
            assertThat(namedCache1.isActive(), is(Boolean.FALSE));
            assertThat(namedCache1.isReleased(), is(Boolean.TRUE));
            assertThat(namedCache2.isDestroyed(), is(Boolean.TRUE));
            assertThat(namedCache2.isReleased(), is(Boolean.TRUE));

            // validate that destroyed cache is detected and not returned.
            namedCache2 = session2.getCache("my-cache");
            assertThat(namedCache2.isDestroyed(), is(Boolean.FALSE));
            assertThat(namedCache2.isActive(), is(Boolean.TRUE));
            assertNotEquals(namedCache1.as(SafeNamedCache.class), is(namedCache2.as(SafeNamedCache.class)));

            // validate for both sessions.
            namedCache1 = session1.getCache("my-cache");
            assertThat(namedCache1.isDestroyed(), is(Boolean.FALSE));
            assertThat(namedCache1.isActive(), is(Boolean.TRUE));
            assertThat(namedCache1.as(SafeNamedCache.class), is(namedCache2.as(SafeNamedCache.class)));
            }
        }
    }
