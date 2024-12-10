/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import static org.junit.Assert.assertEquals;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.pof.PofBufferReader;
import com.tangosol.io.pof.PofBufferWriter;
import com.tangosol.io.pof.SafeConfigurablePofContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

public class MemberTest
    {

    @Test
    public void testSerialize() throws IOException, ClassNotFoundException
        {
        Member member = (Member) CacheFactory.getCluster().getLocalMember();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        oos.writeObject(member);

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
        Member member2 = (Member) ois.readObject();
        assertEquals(member, member2);
        }

    @Test
    public void testSerializePof() throws IOException, ClassNotFoundException
        {
        Member member = (Member) CacheFactory.getCluster().getLocalMember();

        ByteArrayWriteBuffer output = new ByteArrayWriteBuffer(1024);
        SafeConfigurablePofContext pofContext = new SafeConfigurablePofContext();
        pofContext.setEnableAutoTypeDiscovery(false);
        PofBufferWriter writer = new PofBufferWriter(output.getBufferOutput(), pofContext);
        writer.writeObject(0, member);

        PofBufferReader reader = new PofBufferReader(output.getBufferOutput().getBuffer().getUnsafeReadBuffer().getBufferInput(), pofContext);
        Member member2 = (Member) reader.readObject(0);
        assertEquals(member, member2);
        }
    }
