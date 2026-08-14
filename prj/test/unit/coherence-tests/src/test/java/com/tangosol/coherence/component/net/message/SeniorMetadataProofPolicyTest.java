/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.net.message;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService;
import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.util.UUID;

import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Method;
import java.net.InetAddress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for PEER-01 Slice D2 senior-metadata proof policy wiring.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 15.1.2.0
 */
public class SeniorMetadataProofPolicyTest
    {
    @After
    public void cleanup()
            throws Exception
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sOriginalMode);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, m_sOriginalSecurityMode);
        restoreProperty(PROP_SENIOR_METADATA_PROOF_REQUIRED, m_sOriginalRequired);
        resetMode();
        }

    @Test
    public void shouldKeepDefaultSettingFalseAndBehaviorNeutral()
            throws Exception
        {
        setMode("prod");
        setProofRequired(false);
        PolicyClusterService service = new PolicyClusterService(false, true);
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, true);

        assertFalse(service.isPolicyRequired(heartbeat));
        assertFalse(service.isPolicyEnforced(heartbeat));

        writeHeartbeat(heartbeat);

        assertNull(heartbeat.getSeniorMetadataProof());
        assertEquals(0, service.getWouldRejectCount());
        assertEquals(0, service.getDebugAllowCount());
        }

    @Test
    public void shouldObserveExplicitRequiredSetting()
            throws Exception
        {
        setProofRequired(true);
        PolicyClusterService service = new PolicyClusterService(false, true);

        assertTrue(service.isPolicyRequired(heartbeat(service, true)));
        }

    @Test
    public void shouldRejectExplicitRequiredCompatibilityDisabledProvider()
            throws Exception
        {
        setMode("prod");
        setSecurityMode(CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        setProofRequired(true);
        PolicyClusterService service = new PolicyClusterService(false, true);
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, true);

        java.io.IOException e = assertThrows(java.io.IOException.class, () -> writeHeartbeat(heartbeat));
        assertEquals("senior metadata proof required: proof not produced", e.getMessage());
        assertNull(heartbeat.getSeniorMetadataProof());
        assertEquals(0, service.getWouldRejectCount());
        assertEquals(0, service.getDebugAllowCount());
        }

    @Test
    public void shouldNotRequireSeniorMetadataProofForHardenedModeAlone()
            throws Exception
        {
        setMode("dev");
        setSecurityMode(CoherenceMode.SECURITY_MODE_HARDENED);
        setProofRequired(false);
        PolicyClusterService service = new PolicyClusterService(false, true);
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, true);

        writeHeartbeat(heartbeat);
        assertNull(heartbeat.getSeniorMetadataProof());
        assertEquals(0, service.getWouldRejectCount());
        assertEquals(0, service.getDebugAllowCount());
        }

    @Test
    public void shouldNotEnforceUnlessExplicitlyRequired()
            throws Exception
        {
        setMode("prod");
        setSecurityMode(CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        setProofRequired(false);
        PolicyClusterService service = new PolicyClusterService(false, true);
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, true);

        writeHeartbeat(heartbeat);
        assertNull(heartbeat.getSeniorMetadataProof());

        setProofRequired(true);
        assertThrows(java.io.IOException.class, () -> writeHeartbeat(heartbeat(service, true)));
        }

    @Test
    public void shouldRejectProofRequiredIncompatibleRecipients()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);
        PolicyClusterService service = new PolicyClusterService(true, true);
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, true);

        service.setRecipientsCompatible(false);

        java.io.IOException e = assertThrows(java.io.IOException.class, () -> writeHeartbeat(heartbeat));
        assertEquals("senior metadata proof required: incompatible recipient set", e.getMessage());
        assertNull(heartbeat.getSeniorMetadataProof());
        }

    @Test
    public void shouldRejectProofRequiredDisabledProvider()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);
        PolicyClusterService service = new PolicyClusterService(false, true);
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, true);

        java.io.IOException e = assertThrows(java.io.IOException.class, () -> writeHeartbeat(heartbeat));
        assertEquals("senior metadata proof required: proof not produced", e.getMessage());
        assertNull(heartbeat.getSeniorMetadataProof());
        }

    @Test
    public void shouldAllowProofRequiredCompatibleEnabledProvider()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);
        PolicyClusterService service = new PolicyClusterService(true, true);
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, true);

        writeHeartbeat(heartbeat);

        assertNotNull(heartbeat.getSeniorMetadataProof());
        assertEquals(0, service.getWouldRejectCount());
        assertEquals(0, service.getDebugAllowCount());
        }

    @Test
    public void shouldAllowProofRequiredDirectedHeartbeatWithToMemberOnly()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);
        PolicyClusterService service = new PolicyClusterService(true, true);
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, false);

        writeHeartbeat(heartbeat);

        assertNotNull(heartbeat.getSeniorMetadataProof());
        assertEquals(0, service.getWouldRejectCount());
        assertEquals(0, service.getDebugAllowCount());
        }

    @Test
    public void shouldAllowProofRequiredDirectedKillWithToMemberOnly()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);
        PolicyClusterService service = new PolicyClusterService(true, true);
        ClusterService.SeniorMemberKill kill = kill(service, false);

        writeKill(kill);

        assertNotNull(kill.getSeniorMetadataProof());
        assertEquals(0, service.getWouldRejectCount());
        assertEquals(0, service.getDebugAllowCount());
        }

    @Test
    public void shouldRejectProofRequiredIncompatibleToMemberOnly()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);
        PolicyClusterService service = new PolicyClusterService(true, true);
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, false);

        service.setRecipientsCompatible(false);

        java.io.IOException e = assertThrows(java.io.IOException.class, () -> writeHeartbeat(heartbeat));
        assertEquals("senior metadata proof required: incompatible recipient set", e.getMessage());
        assertNull(heartbeat.getSeniorMetadataProof());
        }

    @Test
    public void shouldKeepTrueBroadcastUnknownRecipientModeBehavior()
            throws Exception
        {
        setProofRequired(true);

        setMode("prod");
        setSecurityMode(CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        PolicyClusterService prodCompatibility = new PolicyClusterService(true, true);
        java.io.IOException eCompatibility = assertThrows(java.io.IOException.class,
                () -> writeHeartbeat(noRecipientHeartbeat(prodCompatibility)));
        assertEquals("senior metadata proof required: unknown recipient set", eCompatibility.getMessage());

        setMode("dev");
        setSecurityMode(CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        PolicyClusterService devCompatibility = new PolicyClusterService(true, true);
        java.io.IOException eDev = assertThrows(java.io.IOException.class,
                () -> writeHeartbeat(noRecipientHeartbeat(devCompatibility)));
        assertEquals("senior metadata proof required: unknown recipient set", eDev.getMessage());

        setMode("prod");
        setSecurityMode(CoherenceMode.SECURITY_MODE_HARDENED);
        PolicyClusterService hardened = new PolicyClusterService(true, true);
        java.io.IOException eHardened = assertThrows(java.io.IOException.class,
                () -> writeHeartbeat(noRecipientHeartbeat(hardened)));
        assertEquals("senior metadata proof required: unknown recipient set", eHardened.getMessage());
        }

    @Test
    public void shouldNotRequireSeniorMetadataProofForHardenedModeTrueBroadcast()
            throws Exception
        {
        setMode("prod");
        setSecurityMode(CoherenceMode.SECURITY_MODE_HARDENED);
        setProofRequired(false);
        PolicyClusterService hardened = new PolicyClusterService(true, true);

        writeHeartbeat(noRecipientHeartbeat(hardened));
        assertEquals(0, hardened.getWouldRejectCount());
        assertEquals(0, hardened.getDebugAllowCount());
        }

    @Test
    public void shouldBoundPendingCapabilityAndRecoverAcrossMultipleRecipients() throws Exception
        {
        setProofRequired(true);
        CapabilityClusterService service = new CapabilityClusterService();
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, true);
        Member memberThree = member(3);
        MemberSet recipients = new MemberSet();
        recipients.add(member(2));
        recipients.add(memberThree);
        heartbeat.setToMemberSet(recipients);
        heartbeat.getMemberSet().add(memberThree);

        service.addRecipient(memberThree, ClusterService.VERSION_BARRIER, false);
        service.setCapabilityTime(1_000L);
        assertTrue(service.isCapabilityPending(heartbeat));

        service.removeRecipient(2);
        Member memberFour = member(4);
        recipients.remove(2);
        recipients.add(memberFour);
        heartbeat.getMemberSet().add(memberFour);
        service.addRecipient(memberFour, ClusterService.VERSION_BARRIER, false);
        service.setCapabilityTime(1_005L);
        assertTrue(service.isCapabilityPending(heartbeat));

        service.setCapabilityTime(1_011L);
        assertFalse(service.isCapabilityPending(heartbeat));
        service.setRecipientsCompatible(false);
        java.io.IOException failure = assertThrows(java.io.IOException.class, () -> writeHeartbeat(heartbeat));
        assertEquals("senior metadata proof required: incompatible recipient set", failure.getMessage());

        service.setRecipientVersion(3, "26.1.0", true);
        service.setRecipientVersion(4, "26.1.0", true);
        service.setRecipientsCompatible(true);
        assertFalse(service.isCapabilityPending(heartbeat));
        writeHeartbeat(heartbeat);
        assertNotNull(heartbeat.getSeniorMetadataProof());
        }

    @Test
    public void shouldTreatJoinedVersionBarrierAsActualIncompatibleMember() throws Exception
        {
        setProofRequired(true);
        CapabilityClusterService service = new CapabilityClusterService();
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, true);
        service.setRecipientVersion(2, ClusterService.VERSION_BARRIER, true);

        assertFalse(service.isCapabilityPending(heartbeat));
        service.setRecipientsCompatible(false);
        assertThrows(java.io.IOException.class, () -> writeHeartbeat(heartbeat));
        }

    private static ClusterService.SeniorMemberHeartbeat heartbeat(PolicyClusterService service, boolean fDirected)
            throws Exception
        {
        ClusterService.SeniorMemberHeartbeat heartbeat = new ClusterService.SeniorMemberHeartbeat();

        heartbeat.setService(service);
        heartbeat.setFromMember(member(1));
        heartbeat.setToMember(member(2));
        if (fDirected)
            {
            heartbeat.setToMemberSet(SingleMemberSet.instantiate(member(2)));
            }
        heartbeat.setLastReceivedMillis(101L);
        MemberSet setMember = new MemberSet();
        setMember.add(member(1));
        setMember.add(member(2));
        heartbeat.setMemberSet(setMember);
        heartbeat.setWkaEnabled(true);
        heartbeat.setLastJoinTime(202L);
        return heartbeat;
        }

    private static ClusterService.SeniorMemberHeartbeat noRecipientHeartbeat(PolicyClusterService service)
            throws Exception
        {
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, false);
        heartbeat.setToMember(null);
        heartbeat.setMemberSet(new MemberSet());
        return heartbeat;
        }

    private static ClusterService.SeniorMemberKill kill(PolicyClusterService service, boolean fDirected)
            throws Exception
        {
        ClusterService.SeniorMemberKill kill = new ClusterService.SeniorMemberKill();

        kill.setService(service);
        kill.setFromMember(member(1));
        kill.setToMember(member(2));
        if (fDirected)
            {
            kill.setToMemberSet(SingleMemberSet.instantiate(member(2)));
            }
        return kill;
        }

    private static void writeHeartbeat(ClusterService.SeniorMemberHeartbeat heartbeat)
            throws Exception
        {
        heartbeat.write(new ByteArrayWriteBuffer(1024).getBufferOutput());
        }

    private static void writeKill(ClusterService.SeniorMemberKill kill)
            throws Exception
        {
        kill.write(new ByteArrayWriteBuffer(1024).getBufferOutput());
        }

    private static Member member(int nId)
            throws Exception
        {
        Member member = new Member();
        member.configureDead(nId, new UUID(TIMESTAMP + nId, InetAddress.getByName("127.0.0.1"),
                7574 + nId, nId), TIMESTAMP + nId);
        return member;
        }

    private static void setMode(String sMode)
            throws Exception
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        resetMode();
        }

    private static void setSecurityMode(String sSecurityMode)
            throws Exception
        {
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, sSecurityMode);
        resetMode();
        }

    private static void setProofRequired(boolean fRequired)
        {
        System.setProperty(PROP_SENIOR_METADATA_PROOF_REQUIRED, Boolean.toString(fRequired));
        }

    private static void restoreProperty(String sProperty, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sProperty);
            }
        else
            {
            System.setProperty(sProperty, sValue);
            }
        }

    private static void resetMode()
            throws Exception
        {
        Method method = CoherenceMode.class.getDeclaredMethod("resetForTesting");
        method.setAccessible(true);
        method.invoke(null);
        }

    public static class PolicyClusterService
            extends SeniorMetadataProofProductionTest.ProductionClusterService
        {
        PolicyClusterService(boolean fProviderEnabled, boolean fSenior)
            {
            super(fProviderEnabled, fSenior);
            }

        PolicyClusterService(boolean fProviderEnabled, int nThisMemberId, int nSeniorMemberId, boolean fSenior)
            {
            super(fProviderEnabled, nThisMemberId, nSeniorMemberId, fSenior);
            }

        boolean isPolicyRequired(Message msg)
            {
            return isSeniorMetadataProofRequired(msg);
            }

        boolean isPolicyEnforced(Message msg)
            {
            return isSeniorMetadataProofEnforced(msg);
            }

        int getWouldRejectCount()
            {
            return m_cWouldReject;
            }

        int getDebugAllowCount()
            {
            return m_cDebugAllow;
            }

        String getLastWouldRejectReason()
            {
            return m_sLastWouldRejectReason;
            }

        String getLastDebugAllowReason()
            {
            return m_sLastDebugAllowReason;
            }

        @Override
        protected void onSeniorMetadataProofWouldReject(Message msg, String sReason)
            {
            m_cWouldReject++;
            m_sLastWouldRejectReason = sReason;
            }

        @Override
        protected void onSeniorMetadataProofDebugAllow(Message msg, String sReason)
            {
            m_cDebugAllow++;
            m_sLastDebugAllowReason = sReason;
            }

        private int    m_cWouldReject;
        private int    m_cDebugAllow;
        private String m_sLastWouldRejectReason;
        private String m_sLastDebugAllowReason;
        }

    public static class CapabilityClusterService
            extends PolicyClusterService
        {
        CapabilityClusterService() throws Exception
            {
            super(true, true);
            f_setMembers.add(member(1));
            f_setMembers.add(member(2));
            f_setMembers.setThisMember((Member) f_setMembers.getMember(1));
            f_setMembers.setServiceVersion(1, "26.1.0");
            f_setMembers.setServiceJoined(1);
            f_setMembers.setServiceVersion(2, ClusterService.VERSION_BARRIER);
            }

        @Override
        public MasterMemberSet getClusterMemberSet()
            {
            return f_setMembers == null ? super.getClusterMemberSet() : f_setMembers;
            }

        @Override
        protected long getSeniorMetadataCapabilityTimeMillis()
            {
            return m_lCapabilityTime;
            }

        @Override
        protected long getSeniorMetadataCapabilityPendingMillis()
            {
            return 10L;
            }

        boolean isCapabilityPending(Message msg)
            {
            return isSeniorMetadataProofCapabilityPending(msg);
            }

        void setCapabilityTime(long lTime)
            {
            m_lCapabilityTime = lTime;
            }

        void setRecipientVersion(int nId, String sVersion, boolean fJoined)
            {
            f_setMembers.setServiceVersion(nId, sVersion);
            if (fJoined)
                {
                f_setMembers.setServiceJoined(nId);
                }
            }

        void addRecipient(Member member, String sVersion, boolean fJoined)
            {
            f_setMembers.add(member);
            setRecipientVersion(member.getId(), sVersion, fJoined);
            }

        void removeRecipient(int nId)
            {
            f_setMembers.remove(nId);
            }

        private final MasterMemberSet f_setMembers = new MasterMemberSet();
        private long m_lCapabilityTime;
        }

    private static final String PROP_SENIOR_METADATA_PROOF_REQUIRED =
            "coherence.security.peer.senior-metadata-proof.required";

    private static final long TIMESTAMP = 123456789L;

    private final String m_sOriginalMode         = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
    private final String m_sOriginalSecurityMode = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
    private final String m_sOriginalRequired     = System.getProperty(PROP_SENIOR_METADATA_PROOF_REQUIRED);
    }
