/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net;

import com.tangosol.net.InetAddressHelper;

import com.tangosol.internal.net.cluster.ClusterDependencies;
import com.tangosol.internal.net.cluster.DefaultClusterDependencies;
import com.tangosol.internal.net.cluster.DefaultMemberIdentity;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

import static org.junit.Assert.*;


/**
* Regression test for COH-3645.
*
* @author gg Jul 30, 2010
*/
public class MemberTest
    {
    @Test
    public void testLiteSerialization()
            throws Exception
        {
        Member member = instantiateMember(1);
        testLiteSerialization(member);

        member = instantiateMember(1);
        member.initCommSupport();
        member.getRecentPacketQueue().add(new Object()); // COH-3645
        testLiteSerialization(member);
        }

    @Test
    public void testStandardSerialization()
            throws Exception
        {
        Member member = instantiateMember(1);
        testStandardSerialization(member);

        member = instantiateMember(1);
        member.initCommSupport();
        member.getRecentPacketQueue().add(new Object()); // COH-3645
        testStandardSerialization(member);
        }

    protected static void testLiteSerialization(Member member)
            throws IOException
        {
        Binary binMember = ExternalizableHelper.toBinary(member);
        Member memberCopy = (Member) ExternalizableHelper.fromBinary(binMember,
            ExternalizableHelper.ensureSerializer(null));

        assertTrue("Lite serialization failed", member.equals(memberCopy));
        }

    protected static void testStandardSerialization(Member member)
            throws IOException, ClassNotFoundException
        {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(member);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(
            new ByteArrayInputStream(baos.toByteArray()));
        Member memberCopy = (Member) ois.readObject();
        assertTrue("Serialization failed", member.equals(memberCopy));
        }

    protected static Member instantiateMember(int nMemberId)
        {
        DefaultClusterDependencies cfg = new DefaultClusterDependencies();
        DefaultMemberIdentity memberIdentity = new DefaultMemberIdentity();

        cfg.setEdition(ClusterDependencies.EDITION_GRID);
        cfg.setMode(ClusterDependencies.LICENSE_MODE_DEVELOPMENT);
        memberIdentity.setClusterName("test-cluster");
        memberIdentity.setSiteName("test-site");
        memberIdentity.setRackName("test-rack");
        memberIdentity.setMachineName("test-machine");
        memberIdentity.setProcessName("test-process");
        memberIdentity.setMemberName("test-member" + nMemberId);
        memberIdentity.setRoleName("test-role");
        memberIdentity.setPriority(5);

        cfg.setMemberIdentity(memberIdentity);

        InetAddress addr;
        try
            {
            addr = InetAddressHelper.getLocalHost();
            }
        catch (UnknownHostException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        Member member = new Member();
        int[] an =
            {
            2,  // cpu count
            1,  // socket count
            3   // machine id
            };
        member.configure(cfg, addr, 8080 + nMemberId, 8080 + nMemberId, an);
        member.setId(nMemberId);
        return member;
        }
    }