/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package session;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.component.util.SafeNamedCache;
import com.tangosol.net.CoherenceSession;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;
import com.tangosol.net.options.WithConfiguration;
import com.tangosol.net.topic.NamedTopic;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Functional Tests for {@link Session}s.
 *
 * @author bo 2015.07.27
 */
public class SessionTests
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
            assertThat(namedCache1.as(SafeNamedCache.class), is(not(namedCache2.as(SafeNamedCache.class))));

            // validate for both sessions.
            namedCache1 = session1.getCache("my-cache");
            assertThat(namedCache1.isDestroyed(), is(Boolean.FALSE));
            assertThat(namedCache1.isActive(), is(Boolean.TRUE));
            assertThat(namedCache1.as(SafeNamedCache.class), is(namedCache2.as(SafeNamedCache.class)));
            }
        }

    /**
     * Ensure we can auto-detect and create a {@link Session} against the
     * implicit deployment and then get some resources from it.
     */
    @Test
    public void shouldAcquireNamedTopicFromSession()
            throws Exception
        {
        try (Session session = Session.create(WithConfiguration.using("default-coherence-cache-config.xml")))
            {
            assertThat(session, is(not(nullValue())));

            NamedTopic<String> topic = session.getTopic("my-topic");

            assertThat(topic.isActive(), is(true));
            assertThat(topic, is(not(nullValue())));
            }
        }

    /**
     * Ensure that a {@link Session} returns the same {@link NamedTopic} instance
     * when asked for the same name / type assertion.
     */
    @Test
    public void shouldReturnSameNamedTopicWithSameType()
            throws Exception
        {
        try (Session session = Session.create(WithConfiguration.using("default-coherence-cache-config.xml")))
            {
            NamedTopic<String> topic = session.getTopic("my-topic", ValueTypeAssertion.withType(String.class));

            NamedTopic<String> anotherTopic = session.getTopic("my-topic", ValueTypeAssertion.withType(String.class));

            assertThat(topic, is(anotherTopic));
            }
        }

    /**
     * Ensure that a {@link Session} returns the same {@link NamedTopic} instance
     * when asked for the same name / type assertion.
     */
    @Test
    public void shouldReturnSameNamedTopicWithRawTypes()
            throws Exception
        {
        try (Session session = Session.create(WithConfiguration.using("default-coherence-cache-config.xml")))
            {
            NamedTopic<String> topic = session.getTopic("my-topic", ValueTypeAssertion.withRawTypes());

            NamedTopic<String> anotherTopic = session.getTopic("my-topic", ValueTypeAssertion.withRawTypes());

            assertThat(topic, is(anotherTopic));
            }
        }

    /**
     * Ensure that a {@link Session} returns the same {@link NamedTopic} instance
     * when asked for the same name / type assertion.
     */
    @Test
    public void shouldReturnSameNamedTopic()
            throws Exception
        {
        try (Session session = Session.create(WithConfiguration.using("default-coherence-cache-config.xml")))
            {
            NamedTopic<String> topic = session.getTopic("my-topic");

            NamedTopic<String> anotherTopic = session.getTopic("my-topic");

            assertThat(topic, is(anotherTopic));
            }
        }

    /**
     * Ensure that a {@link Session} doesn't return a released {@link NamedTopic}
     * after it is released.
     */
    @Test
    public void shouldNotReturnReleasedNamedTopic()
            throws Exception
        {
        try (Session session = Session.create(WithConfiguration.using("default-coherence-cache-config.xml")))
            {
            NamedTopic topic = session.getTopic("my-topic");

            topic.release();

            NamedTopic anotherTopic = session.getTopic("my-topic");

            // the first topic should be inactive (released)
            assertThat(topic.isActive(), is(false));

            // the second topic should be active
            assertThat(anotherTopic.isActive(), is(true));

            // and not the same as the second topic
            assertThat(System.identityHashCode(topic), is(not(System.identityHashCode(anotherTopic))));
            }
        }

    /**
     * Ensure that a {@link Session} doesn't return a destroyed {@link NamedTopic}
     * after it is destroyed.
     */
    @Test
    public void shouldNotReturnDestroyedNamedTopic()
            throws Exception
        {
        try (Session session = Session.create(WithConfiguration.using("default-coherence-cache-config.xml")))
            {
            NamedTopic topic = session.getTopic("my-topic");

            topic.destroy();
            Eventually.assertDeferred(()-> topic.isDestroyed(), is(true));

            NamedTopic anotherTopic = session.getTopic("my-topic");

            // the first topic should be inactive (destroyed)
            assertThat(topic.isActive(), is(false));

            // and not the same as the second topic
            assertThat(topic == anotherTopic, is(false));
            assertThat(System.identityHashCode(topic), is(not(System.identityHashCode(anotherTopic))));
            }
        }

    /**
     * Ensure that closing a {@link Session} releases acquired {@link NamedTopic}s.
     */
    @Test
    public void shouldReleaseNamedTopicWhenSessionClosed()
            throws Exception
        {
        NamedTopic namedTopic;

        try (Session session = Session.create(WithConfiguration.using("default-coherence-cache-config.xml")))
            {
            namedTopic = session.getTopic("my-topic");

            // the topic should be active
            assertThat(namedTopic.isActive(), is(true));
            }

        // the topic should be inactive
        assertThat(namedTopic.isActive(), is(false));
        }

    /**
     * Ensure that two {@link Session}s produce different {@link NamedTopic}
     * instances for the same named topic.
     * @throws Exception
     */
    @Test
    public void shouldReturnDifferentNamedTopicsForDifferentSessions()
            throws Exception
        {
        try (Session session1 = Session.create(WithConfiguration.using("default-coherence-cache-config.xml"));
             Session session2 = Session.create(WithConfiguration.using("default-coherence-cache-config.xml")))
            {
            NamedTopic namedTopic1 = session1.getTopic("my-topic");
            NamedTopic namedTopic2 = session2.getTopic("my-topic");

            assertThat(namedTopic1, is(not(namedTopic2)));

            namedTopic1.release();

            assertThat(namedTopic1.isActive(), is(false));
            assertThat(namedTopic2.isActive(), is(true));

            namedTopic2.release();

            assertThat(namedTopic1.isActive(), is(false));
            assertThat(namedTopic2.isActive(), is(false));
            }
        }

    /**
     * Ensure that closing a {@link Session} releases acquired {@link NamedTopic}s
     * and doesn't effect {@link NamedTopic}s from other {@link Session}s.
     */
    @Test
    public void shouldReleaseNamedTopicWhenSessionClosedButNotEffectOtherSessions()
            throws Exception
        {
        try (Session session1 = Session.create(WithConfiguration.using("default-coherence-cache-config.xml"));
             Session session2 = Session.create(WithConfiguration.using("default-coherence-cache-config.xml")))
            {
            NamedTopic namedTopic1 = session1.getTopic("my-topic");
            NamedTopic namedTopic2 = session2.getTopic("my-topic");

            assertThat(namedTopic1, is(not(namedTopic2)));

            assertThat(namedTopic1.isActive(), is(true));
            assertThat(namedTopic2.isActive(), is(true));

            session1.close();

            assertThat(namedTopic1.isActive(), is(false));
            assertThat(namedTopic2.isActive(), is(true));

            namedTopic2.release();

            assertThat(namedTopic1.isActive(), is(false));
            assertThat(namedTopic2.isActive(), is(false));
            }
        }

    /**
     * Ensure that two {@link Session}s produce different {@link NamedTopic}
     * instances for the same named topic and that if one topic is destroyed,
     * and if Session.getTopic is called on the second session, the topic should
     * be ensured.
     * @throws Exception
     */
    @Test
    public void shouldNotReturnInactiveTopicWhenNamedTopicDestroyedByOtherSession()
            throws Exception
        {
        try (Session session1 = Session.create(WithConfiguration.using("default-coherence-cache-config.xml"));
             Session session2 = Session.create(WithConfiguration.using("default-coherence-cache-config.xml")))
            {
            NamedTopic namedTopic1 = session1.getTopic("my-topic");
            NamedTopic namedTopic2 = session2.getTopic("my-topic");

            assertThat(namedTopic1, is(not(namedTopic2)));

            namedTopic1.destroy();

            // both session NT considered destroyed since shared internal NT
            assertThat(namedTopic1.isDestroyed(), is(Boolean.TRUE));
            assertThat(namedTopic1.isActive(), is(Boolean.FALSE));
            assertThat(namedTopic1.isReleased(), is(Boolean.TRUE));
            assertThat(namedTopic2.isDestroyed(), is(Boolean.TRUE));
            assertThat(namedTopic2.isReleased(), is(Boolean.TRUE));

            // validate that destroyed topic is detected and not returned.
            namedTopic2 = session2.getTopic("my-topic");
            assertThat(namedTopic2.isDestroyed(), is(Boolean.FALSE));
            assertThat(namedTopic2.isActive(), is(Boolean.TRUE));

            // validate for both sessions.
            namedTopic1 = session1.getTopic("my-topic");
            assertThat(namedTopic1.isDestroyed(), is(Boolean.FALSE));
            assertThat(namedTopic1.isActive(), is(Boolean.TRUE));
            }
        }
    }
