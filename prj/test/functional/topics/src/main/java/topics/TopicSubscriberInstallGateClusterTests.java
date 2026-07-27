/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.function.Remote;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.assertThrows;

/**
 * Cluster-client topic subscriber install gate tests.
 *
 * @author Aleks Seovic  2026.05.14
 * @since 26.04
 */
public class TopicSubscriberInstallGateClusterTests
    {
    @Before
    public void setup() throws Exception
        {
        m_sClusterOld      = System.getProperty(PROP_CLUSTER);
        m_sCacheConfigOld  = System.getProperty(PROP_CACHE_CONFIG);
        m_sLocalStorageOld = System.getProperty(PROP_LOCAL_STORAGE);
        m_sModeOld         = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sSecurityModeOld = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
        m_sWkaOld          = System.getProperty(PROP_WKA);
        m_sLocalhostOld    = System.getProperty(PROP_LOCALHOST);

        String sHost = LocalPlatform.get().getLoopbackAddress().getHostAddress();
        setProperty(PROP_WKA, sHost);
        setProperty(PROP_LOCALHOST, sHost);
        setProperty(PROP_CLUSTER, clusterName());
        setProperty(PROP_CACHE_CONFIG, "topic-cache-config.xml");
        setProperty(PROP_LOCAL_STORAGE, "true");
        setProperty(CoherenceMode.PROP_COHERENCE_MODE, "prod");
        setProperty(CoherenceMode.PROP_SECURITY_MODE, CoherenceMode.SECURITY_MODE_HARDENED);
        CoherenceModeHelper.restore("prod");
        CoherenceModeHelper.restoreSecurityMode(CoherenceMode.SECURITY_MODE_HARDENED);

        m_coherence = Coherence.clusterMember().startAndWait();
        m_session   = m_coherence.getSession();
        }

    @After
    public void cleanup()
        {
        Coherence.closeAll();
        CacheFactory.shutdown();
        restoreProperty(PROP_CLUSTER, m_sClusterOld);
        restoreProperty(PROP_CACHE_CONFIG, m_sCacheConfigOld);
        restoreProperty(PROP_LOCAL_STORAGE, m_sLocalStorageOld);
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, m_sSecurityModeOld);
        restoreProperty(PROP_WKA, m_sWkaOld);
        restoreProperty(PROP_LOCALHOST, m_sLocalhostOld);
        CoherenceModeHelper.reset();
        }

    @Test
    public void rejectsUnannotatedFilterWhenEnsuringSubscriberGroup()
        {
        NamedTopic<String> topic = topic();

        assertThrows(SecurityException.class, () ->
                topic.ensureSubscriberGroup("gate-group", new PlainFilter(), null));
        }

    @Test
    public void rejectsUnannotatedFilterWhenCreatingSubscriber()
        {
        NamedTopic<String> topic = topic();

        assertThrows(SecurityException.class, () ->
                topic.createSubscriber(Subscriber.withFilter(new PlainFilter())));
        }

    @Test
    public void rejectsUnannotatedExtractorWhenEnsuringSubscriberGroup()
        {
        NamedTopic<String> topic = topic();

        assertThrows(SecurityException.class, () ->
                topic.ensureSubscriberGroup("gate-extractor", null, new PlainExtractor()));
        }

    @Test
    public void rejectsUnannotatedExtractorWhenCreatingSubscriber()
        {
        NamedTopic<String> topic = topic();

        assertThrows(SecurityException.class, () ->
                topic.createSubscriber(Subscriber.withConverter(new PlainExtractor())));
        }

    @Test
    public void rejectsNestedExtractorWhenCreatingFilteredSubscriber()
        {
        NamedTopic<String> topic = topic();

        assertThrows(SecurityException.class, () ->
                topic.createSubscriber(Subscriber.withFilter(new EqualsFilter<>(new PlainExtractor(), "value"))));
        }

    @Test
    public void allowsAnnotatedFilterWhenEnsuringSubscriberGroup()
        {
        NamedTopic<String> topic = topic();

        topic.ensureSubscriberGroup("gate-annotated", new AnnotatedFilter(), null);
        }

    @Test
    public void allowsAnnotatedExtractorWhenEnsuringSubscriberGroup()
        {
        NamedTopic<String> topic = topic();

        topic.ensureSubscriberGroup("gate-annotated-extractor", null, new AnnotatedExtractor());
        }

    private NamedTopic<String> topic()
        {
        return m_session.getTopic("java-c4a-gate-" + f_testName.getMethodName());
        }

    private String clusterName()
        {
        return "TC4a-" + Integer.toUnsignedString(f_testName.getMethodName().hashCode(), 36)
                + '-' + Long.toString(System.nanoTime(), 36);
        }

    private static void setProperty(String sName, String sValue)
        {
        System.setProperty(sName, sValue);
        }

    private static void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    public static class PlainFilter
            implements Filter<String>
        {
        @Override
        public boolean evaluate(String value)
            {
            return true;
            }
        }

    @Remote.Executable
    public static class AnnotatedFilter
            implements Filter<String>
        {
        @Override
        public boolean evaluate(String value)
            {
            return true;
            }
        }

    public static class PlainExtractor
            implements ValueExtractor<String, String>
        {
        @Override
        public String extract(String target)
            {
            return target;
            }
        }

    @Remote.Executable
    public static class AnnotatedExtractor
            implements ValueExtractor<String, String>
        {
        @Override
        public String extract(String target)
            {
            return target;
            }
        }

    private static final String PROP_CLUSTER       = "coherence.cluster";
    private static final String PROP_CACHE_CONFIG  = "coherence.cacheconfig";
    private static final String PROP_LOCAL_STORAGE = "coherence.distributed.localstorage";
    private static final String PROP_WKA           = "coherence.wka";
    private static final String PROP_LOCALHOST     = "coherence.localhost";

    @Rule
    public final TestName f_testName = new TestName();

    private Coherence m_coherence;
    private Session   m_session;
    private String    m_sClusterOld;
    private String    m_sCacheConfigOld;
    private String    m_sLocalStorageOld;
    private String    m_sModeOld;
    private String    m_sSecurityModeOld;
    private String    m_sWkaOld;
    private String    m_sLocalhostOld;
    }
