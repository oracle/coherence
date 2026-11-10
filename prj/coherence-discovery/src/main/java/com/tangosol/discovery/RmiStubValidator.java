/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.discovery;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamConstants;

import java.util.Base64;

/**
 * Recognize the bounded serialized forms of the JDK's JRMP RMIServer stubs.
 * This reads the wire grammar, never Java objects. In particular, it must not
 * use ObjectInputStream: reading even a legitimate RemoteObject can register
 * a remote reference with RMI distributed garbage collection.
 *
 * @author phf  2026.11.04
 * @since 27.01
 */
final class RmiStubValidator
        implements ObjectStreamConstants
    {
    private RmiStubValidator(byte[] ab)
        {
        f_bytes = new ByteArrayInputStream(ab);
        f_in    = new DataInputStream(f_bytes);
        }

    /**
     * Validate a complete, canonical Base64-encoded JDK JRMP server stub.
     * Unknown descriptors, annotations, references, factories and trailing data
     * are rejected rather than interpreted by a Java deserializer.
     *
     * @param sEncoded  the part following {@code /stub/}
     *
     * @throws IOException if the encoding is unsupported or malformed
     */
    static void validate(String sEncoded)
            throws IOException
        {
        if (sEncoded == null || sEncoded.isEmpty() || sEncoded.length() > MAX_ENCODED_CHARS)
            {
            throw invalid("encoded length");
            }

        byte[] ab;
        try
            {
            ab = Base64.getDecoder().decode(sEncoded);
            }
        catch (IllegalArgumentException e)
            {
            throw invalid("Base64 encoding");
            }
        if (ab.length > MAX_BYTES || !Base64.getEncoder().encodeToString(ab).equals(sEncoded))
            {
            throw invalid("noncanonical or oversized encoding");
            }

        new RmiStubValidator(ab).readStub();
        }

    private void readStub()
            throws IOException
        {
        if (f_in.readUnsignedShort() != (STREAM_MAGIC & 0xffff)
                || f_in.readUnsignedShort() != STREAM_VERSION)
            {
            throw invalid("stream header");
            }
        expect(TC_OBJECT);
        switch (f_in.readUnsignedByte())
            {
            case TC_CLASSDESC:
                descriptorBody("javax.management.remote.rmi.RMIServerImpl_Stub", 2L, SC_SERIALIZABLE, 0);
                descriptor("java.rmi.server.RemoteStub", -1585587260594494182L, SC_SERIALIZABLE, 0);
                remoteObjectDescriptor();
                break;

            case TC_PROXYCLASSDESC:
                if (f_in.readInt() != 1 || !"javax.management.remote.rmi.RMIServer".equals(readUtf(f_in, 256)))
                    {
                    throw invalid("proxy interfaces");
                    }
                expect(TC_ENDBLOCKDATA);
                expect(TC_CLASSDESC);
                descriptorFields("java.lang.reflect.Proxy", -2222568056686623797L, SC_SERIALIZABLE, 1);
                expect('L');
                if (!"h".equals(readUtf(f_in, 8)))
                    {
                    throw invalid("proxy handler field");
                    }
                expect(TC_STRING);
                if (!"Ljava/lang/reflect/InvocationHandler;".equals(readUtf(f_in, 128)))
                    {
                    throw invalid("proxy handler signature");
                    }
                expect(TC_ENDBLOCKDATA);
                expect(TC_NULL);
                expect(TC_OBJECT);
                descriptor("java.rmi.server.RemoteObjectInvocationHandler", 2L, SC_SERIALIZABLE, 0);
                remoteObjectDescriptor();
                break;

            default:
                throw invalid("root descriptor");
            }

        readRemoteReference();
        if (f_bytes.available() != 0)
            {
            throw invalid("trailing data");
            }
        }

    private void remoteObjectDescriptor()
            throws IOException
        {
        descriptor("java.rmi.server.RemoteObject", -3215090123894869218L,
                SC_SERIALIZABLE | SC_WRITE_METHOD, 0);
        expect(TC_NULL);
        }

    private void descriptor(String sClass, long nUid, int nFlags, int cFields)
            throws IOException
        {
        expect(TC_CLASSDESC);
        descriptorBody(sClass, nUid, nFlags, cFields);
        }

    private void descriptorBody(String sClass, long nUid, int nFlags, int cFields)
            throws IOException
        {
        descriptorFields(sClass, nUid, nFlags, cFields);
        expect(TC_ENDBLOCKDATA);
        }

    private void descriptorFields(String sClass, long nUid, int nFlags, int cFields)
            throws IOException
        {
        if (!sClass.equals(readUtf(f_in, 256)) || f_in.readLong() != nUid
                || f_in.readUnsignedByte() != nFlags || f_in.readUnsignedShort() != cFields)
            {
            throw invalid("class schema");
            }
        }

    private void readRemoteReference()
            throws IOException
        {
        BlockInput      blocks = new BlockInput();
        DataInputStream data   = new DataInputStream(blocks);
        String          sRef   = readUtf(data, 32);
        int             nFormat;
        if ("UnicastRef".equals(sRef))
            {
            nFormat = 0;
            }
        else if ("UnicastRef2".equals(sRef))
            {
            nFormat = data.readUnsignedByte();
            if (nFormat != 0 && nFormat != 1)
                {
                throw invalid("endpoint format");
                }
            }
        else
            {
            throw invalid("remote reference type");
            }

        String sHost = readUtf(data, MAX_HOST_CHARS * 3);
        if (sHost.isEmpty() || sHost.length() > MAX_HOST_CHARS)
            {
            throw invalid("endpoint host length");
            }
        for (int i = 0; i < sHost.length(); i++)
            {
            if (Character.isISOControl(sHost.charAt(i)))
                {
                throw invalid("endpoint host character");
                }
            }
        int nPort = data.readInt();
        if (nPort <= 0 || nPort > 65535)
            {
            throw invalid("endpoint port");
            }

        if (nFormat == 1)
            {
            blocks.exhausted();
            expect(TC_OBJECT);
            descriptor("javax.rmi.ssl.SslRMIClientSocketFactory", -8310631444933958385L, SC_SERIALIZABLE, 0);
            expect(TC_NULL);
            }

        // ObjID and UID contain only fixed-width primitive data
        data.readLong();
        data.readInt();
        data.readLong();
        data.readShort();
        if (data.readUnsignedByte() != 0)
            {
            throw invalid("result-stream flag");
            }
        blocks.exhausted();
        expect(TC_ENDBLOCKDATA);
        }

    private void expect(int nExpected)
            throws IOException
        {
        if (f_in.readUnsignedByte() != nExpected)
            {
            throw invalid("serialization token");
            }
        }

    private static String readUtf(DataInputStream in, int cbMax)
            throws IOException
        {
        int cb = in.readUnsignedShort();
        if (cb > cbMax)
            {
            throw invalid("UTF length");
            }
        byte[] ab = new byte[cb + 2];
        ab[0] = (byte) (cb >>> 8);
        ab[1] = (byte) cb;
        in.readFully(ab, 2, cb);
        return new DataInputStream(new ByteArrayInputStream(ab)).readUTF();
        }

    private static IOException invalid(String sReason)
        {
        return new IOException("Unsupported encoded RMI stub: " + sReason);
        }

    /**
     * Read primitive custom data across serialization block boundaries. Object
     * tokens are consumed only by the fixed grammar above, never by this reader.
     */
    private class BlockInput
            extends InputStream
        {
        @Override
        public int read()
                throws IOException
            {
            if (m_cbRemaining == 0)
                {
                int nToken = f_in.readUnsignedByte();
                if (nToken == TC_BLOCKDATA)
                    {
                    m_cbRemaining = f_in.readUnsignedByte();
                    }
                else if (nToken == TC_BLOCKDATALONG)
                    {
                    m_cbRemaining = f_in.readInt();
                    }
                else
                    {
                    throw invalid("primitive block token");
                    }
                if (m_cbRemaining <= 0 || m_cbRemaining > MAX_BYTES || m_cbRemaining > f_bytes.available())
                    {
                    throw invalid("primitive block length");
                    }
                }
            --m_cbRemaining;
            return f_in.readUnsignedByte();
            }

        @Override
        public int read(byte[] ab, int of, int cb)
                throws IOException
            {
            if (of < 0 || cb < 0 || of > ab.length - cb)
                {
                throw new IndexOutOfBoundsException();
                }
            // InputStream's default implementation can suppress an IOException
            // after a partial read; malformed tokens must always reject the stub
            for (int i = 0; i < cb; i++)
                {
                ab[of + i] = (byte) read();
                }
            return cb;
            }

        void exhausted()
                throws IOException
            {
            if (m_cbRemaining != 0)
                {
                throw invalid("unexpected primitive data");
                }
            }

        private int m_cbRemaining;
        }

    private final ByteArrayInputStream f_bytes;
    private final DataInputStream      f_in;

    private static final int MAX_BYTES         = 4096;
    private static final int MAX_ENCODED_CHARS = ((MAX_BYTES + 2) / 3) * 4;
    private static final int MAX_HOST_CHARS    = 1024;
    }
