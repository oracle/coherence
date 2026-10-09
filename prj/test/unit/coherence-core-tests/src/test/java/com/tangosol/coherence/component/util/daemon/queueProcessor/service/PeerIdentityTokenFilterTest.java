/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.util.daemon.queueProcessor.service;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.internal.SerializationAllowlist;
import com.tangosol.io.pof.PofPrincipal;

import com.tangosol.net.security.DefaultIdentityAsserter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import java.security.Principal;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.Subject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for bridge-filtered Extend identity-token deserialization.
 *
 * @author OpenAI  2026.05.16
 */
@RunWith(Parameterized.class)
public class PeerIdentityTokenFilterTest
    {
    @Parameterized.Parameters(name = "security={0}")
    public static Object[] modes()
        {
        return new Object[] {"compatibility", "hardened"};
        }

    public PeerIdentityTokenFilterTest(String sSecurityMode)
        {
        f_sSecurityMode = sSecurityMode;
        }

    @Before
    public void setUp()
        {
        m_scope = CoherenceModeHelper.securityMode(f_sSecurityMode);
        m_sAllowed = System.getProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
        System.clearProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
        }

    @After
    public void tearDown()
        {
        if (m_sAllowed == null)
            {
            System.clearProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
            }
        else
            {
            System.setProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED, m_sAllowed);
            }
        m_scope.close();
        }

    @Test
    public void shouldReturnNullIdentityToken()
        {
        assertNull(peer().deserializeIdentityToken(null));
        }

    @Test
    public void shouldApplyModeWhenAssertingPassiveSubjectIdentityToken()
        {
        allowPassiveSubjectFixture();
        Peer    peer    = peer();
        Subject subject = new Subject();

        subject.getPrincipals().add(new PofPrincipal("CN=Manager, OU=MyUnit"));

        Object oToken = peer.deserializeIdentityToken(peer.serializeIdentityToken(subject));

        assertTrue(oToken instanceof Subject);
        assertTrue(((Subject) oToken).getPrincipals().contains(new PofPrincipal("CN=Manager, OU=MyUnit")));
        if ("hardened".equals(f_sSecurityMode))
            {
            // allowing passive decoding does not make the token proof of identity
            assertThrows(SecurityException.class, () -> DefaultIdentityAsserter.INSTANCE.assertIdentity(oToken, null));
            }
        else
            {
            assertSame(oToken, DefaultIdentityAsserter.INSTANCE.assertIdentity(oToken, null));
            }
        }

    @Test
    public void shouldRejectUnconfiguredSubjectIdentityTokenInHardenedMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            Peer   peer    = peer();
            byte[] abToken = peer.serializeIdentityToken(new Subject());

            assertThrows(SecurityException.class, () -> peer.deserializeIdentityToken(abToken));
            }
        }

    @Test
    public void shouldRejectCustomIdentityTokenBeforeReadObject()
        {
        Peer   peer    = peer();
        byte[] abToken = peer.serializeIdentityToken(new MaterializationProbe());

        MaterializationProbe.reset();

        assertThrows(SecurityException.class, () -> peer.deserializeIdentityToken(abToken));
        assertFalse(MaterializationProbe.wasMaterialized());
        }

    @Test
    public void shouldRejectSubjectPrincipalCustomNumberBeforeReadObject()
        {
        allowPassiveSubjectFixture();
        Peer    peer    = peer();
        Subject subject = new Subject();

        subject.getPrincipals().add(new NumberProbe());
        byte[] abToken = peer.serializeIdentityToken(subject);

        NumberProbe.reset();

        assertThrows(SecurityException.class, () -> peer.deserializeIdentityToken(abToken));
        assertFalse(NumberProbe.wasMaterialized());
        }

    @Test
    public void shouldAllowExplicitlyAllowlistedCustomIdentityToken()
        {
        Peer   peer     = peer();
        String sClass   = MaterializationProbe.class.getName();
        String sCurrent = System.getProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
        byte[] abToken  = peer.serializeIdentityToken(new MaterializationProbe());

        try
            {
            System.setProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED, sClass);
            MaterializationProbe.reset();

            Object oToken = peer.deserializeIdentityToken(abToken);

            assertTrue(oToken instanceof MaterializationProbe);
            assertTrue(MaterializationProbe.wasMaterialized());
            }
        finally
            {
            if (sCurrent == null)
                {
                System.clearProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
                }
            else
                {
                System.setProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED, sCurrent);
                }
            }
        }

    private void allowPassiveSubjectFixture()
        {
        if ("hardened".equals(f_sSecurityMode))
            {
            System.setProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED, String.join(";",
                    "javax.security.auth.Subject",
                    "javax.security.auth.Subject$SecureSet",
                    "com.tangosol.io.pof.PofPrincipal"));
            }
        }

    private Peer peer()
        {
        return new TestPeer();
        }

    /**
     * Inert peer that exercises Peer identity-token serialization without
     * starting an acceptor.
     */
    private static class TestPeer
            extends Peer
        {
        TestPeer()
            {
            super(null, null, false);
            setSerializer(new DefaultSerializer());
            }
        }

    /**
     * Serializable probe that records if Java deserialization reaches
     * readObject.
     */
    public static class MaterializationProbe
            implements Serializable
        {
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
            {
            MATERIALIZED.set(true);
            in.defaultReadObject();
            }

        static void reset()
            {
            MATERIALIZED.set(false);
            }

        static boolean wasMaterialized()
            {
            return MATERIALIZED.get();
            }

        private static final AtomicBoolean MATERIALIZED = new AtomicBoolean();

        private static final long serialVersionUID = 1L;
        }

    /**
     * Serializable Number probe that records if Java deserialization reaches
     * readObject.
     */
    public static class NumberProbe
            extends Number
            implements Principal
        {
        @Override
        public String getName()
            {
            return "number-probe";
            }

        @Override
        public int intValue()
            {
            return 0;
            }

        @Override
        public long longValue()
            {
            return 0L;
            }

        @Override
        public float floatValue()
            {
            return 0.0f;
            }

        @Override
        public double doubleValue()
            {
            return 0.0d;
            }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
            {
            MATERIALIZED.set(true);
            in.defaultReadObject();
            }

        static void reset()
            {
            MATERIALIZED.set(false);
            }

        static boolean wasMaterialized()
            {
            return MATERIALIZED.get();
            }

        private static final AtomicBoolean MATERIALIZED = new AtomicBoolean();

        private static final long serialVersionUID = 1L;
        }

    private CoherenceModeHelper.ModeScope m_scope;

    private String m_sAllowed;

    private final String f_sSecurityMode;
    }
