/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.oracle.coherence.persistence.PersistenceException;

import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.ReadBuffer.BufferInput;
import com.tangosol.io.Serializer;
import com.tangosol.io.WriteBuffer.BufferOutput;

import com.tangosol.net.internal.QuorumInfo;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.IOException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Narrow codec for product-owned persistence metadata values.
 *
 * @author Aleks Seovic  2026.05.15
 * @since 26.04
 */
final class PersistenceMetadataCodec
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Static helper.
     */
    private PersistenceMetadataCodec()
        {
        }

    // ----- primitive metadata --------------------------------------------

    /**
     * Encode a partition count.
     *
     * @param cPartitions  the partition count
     *
     * @return the encoded partition count
     */
    static Binary encodePartitionCount(int cPartitions)
        {
        validateRange("partition count", cPartitions, 1, MAX_PARTITION_COUNT);
        return encodeInt(cPartitions);
        }

    /**
     * Decode a partition count.
     *
     * @param buf  the encoded partition count
     *
     * @return the decoded partition count
     */
    static int decodePartitionCount(ReadBuffer buf)
        {
        int cPartitions = decodeInt(buf, "partition count");
        validateRange("partition count", cPartitions, 1, MAX_PARTITION_COUNT);
        return cPartitions;
        }

    /**
     * Encode a persistence version.
     *
     * @param nVersion  the persistence version
     *
     * @return the encoded persistence version
     */
    static Binary encodePersistenceVersion(int nVersion)
        {
        validateRange("persistence version", nVersion, 1, CachePersistenceHelper.PERSISTENCE_VERSION);
        return encodeString(String.valueOf(nVersion));
        }

    /**
     * Decode a persistence version.
     *
     * @param buf  the encoded persistence version
     *
     * @return the decoded persistence version
     */
    static int decodePersistenceVersion(ReadBuffer buf)
        {
        int nVersion = decodeIntOrString(buf, "persistence version");
        validateRange("persistence version", nVersion, 1, CachePersistenceHelper.PERSISTENCE_VERSION);
        return nVersion;
        }

    /**
     * Decode an integer value from a fixed metadata scalar format.
     *
     * @param buf    the encoded value
     * @param sName  the metadata name
     *
     * @return the decoded value
     */
    private static int decodeInt(ReadBuffer buf, String sName)
        {
        try
            {
            BufferInput in = checkedInput(buf, sName, MAX_SCALAR_BYTES);
            int         n  = in.readUnsignedByte();
            if (n != ExternalizableHelper.FMT_INT)
                {
                throw rejected(sName, "unexpected metadata format " + n);
                }

            int nValue = ExternalizableHelper.readInt(in);
            requireEnd(in, sName);
            return nValue;
            }
        catch (IOException e)
            {
            throw rejected(sName, e);
            }
        }

    /**
     * Decode an integer or decimal string value from a fixed metadata scalar
     * format.
     *
     * @param buf    the encoded value
     * @param sName  the metadata name
     *
     * @return the decoded value
     */
    private static int decodeIntOrString(ReadBuffer buf, String sName)
        {
        try
            {
            BufferInput in = checkedInput(buf, sName, MAX_SCALAR_BYTES);
            int         n  = in.readUnsignedByte();
            int         nValue;
            switch (n)
                {
                case ExternalizableHelper.FMT_INT:
                    nValue = ExternalizableHelper.readInt(in);
                    break;

                case ExternalizableHelper.FMT_STRING:
                    nValue = parseInt(ExternalizableHelper.readUTF(in), sName);
                    break;

                default:
                    throw rejected(sName, "unexpected metadata format " + n);
                }

            requireEnd(in, sName);
            return nValue;
            }
        catch (IOException e)
            {
            throw rejected(sName, e);
            }
        }

    /**
     * Encode an integer value using the same safe scalar shape used by the
     * default serializer for Integer values.
     *
     * @param nValue  the value to encode
     *
     * @return the encoded value
     */
    private static Binary encodeInt(int nValue)
        {
        try
            {
            ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(1 + BufferOutput.MAX_PACKED_INT_SIZE);
            BufferOutput         out = buf.getBufferOutput();
            out.writeByte(ExternalizableHelper.FMT_INT);
            ExternalizableHelper.writeInt(out, nValue);
            return buf.toBinary();
            }
        catch (IOException e)
            {
            throw CachePersistenceHelper.ensurePersistenceException(e, "Unable to encode persistence metadata");
            }
        }

    /**
     * Encode a string value using the same safe scalar shape used by the
     * default serializer for String values.
     *
     * @param sValue  the value to encode
     *
     * @return the encoded value
     */
    private static Binary encodeString(String sValue)
        {
        try
            {
            ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(16);
            BufferOutput         out = buf.getBufferOutput();
            out.writeByte(ExternalizableHelper.FMT_STRING);
            ExternalizableHelper.writeUTF(out, sValue);
            return buf.toBinary();
            }
        catch (IOException e)
            {
            throw CachePersistenceHelper.ensurePersistenceException(e, "Unable to encode persistence metadata");
            }
        }

    // ----- quorum metadata -----------------------------------------------

    /**
     * Decode quorum metadata from the retained QuorumInfo format.
     *
     * @param bin         the encoded quorum metadata
     * @param serializer  the metadata serializer
     *
     * @return the decoded quorum metadata
     */
    static QuorumInfo decodeQuorum(Binary bin, Serializer serializer)
        {
        validateQuorumEnvelope(bin);
        try
            {
            return ExternalizableHelper.fromBinary(bin, serializer, in ->
                {
                in.setObjectInputFilter(QuorumObjectInputFilter.INSTANCE.proxy());
                return in;
                }, QuorumInfo.class);
            }
        catch (RuntimeException e)
            {
            throw rejected("quorum info", e);
            }
        }

    /**
     * Validate the top-level quorum envelope before any object can be
     * instantiated.
     *
     * @param bin  the encoded quorum metadata
     */
    private static void validateQuorumEnvelope(Binary bin)
        {
        try
            {
            BufferInput in = checkedInput(bin, "quorum info", MAX_QUORUM_BYTES);
            int         n  = in.readUnsignedByte();
            if (n != ExternalizableHelper.FMT_OBJ_EXT)
                {
                throw rejected("quorum info", "unexpected metadata format " + n);
                }

            String sClass = ExternalizableHelper.readUTF(in);
            if (!QuorumInfo.class.getName().equals(sClass))
                {
                throw rejected("quorum info", "unexpected metadata class " + sClass);
                }
            }
        catch (IOException e)
            {
            throw rejected("quorum info", e);
            }
        }

    // ----- snapshot names -------------------------------------------------

    /**
     * Validate that the supplied snapshot name is already a safe file name.
     *
     * @param sSnapshot  the snapshot name
     *
     * @return the validated snapshot name
     */
    static String validateSnapshotName(String sSnapshot)
        {
        if (sSnapshot == null)
            {
            throw new IllegalArgumentException("null snapshot name");
            }

        String sName = sSnapshot.trim();
        if (sName.isEmpty())
            {
            throw new IllegalArgumentException("empty snapshot name");
            }

        String sSafe = com.tangosol.io.FileHelper.toFilename(sName);
        if (!sName.equals(sSafe))
            {
            throw new IllegalArgumentException("invalid snapshot name \"" + sSnapshot
                    + "\"; use \"" + sSafe + "\"");
            }

        return sName;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return a checked metadata input.
     *
     * @param buf    the metadata bytes
     * @param sName  the metadata name
     * @param cbMax  the maximum length
     *
     * @return the checked input
     */
    private static BufferInput checkedInput(ReadBuffer buf, String sName, int cbMax)
        {
        if (buf == null)
            {
            throw rejected(sName, "missing metadata");
            }

        int cb = buf.length();
        if (cb == 0 || cb > cbMax)
            {
            throw rejected(sName, "metadata length " + cb + " is outside the allowed range");
            }

        return buf.getBufferInput();
        }

    /**
     * Require that the metadata input has been fully consumed.
     *
     * @param in     the input
     * @param sName  the metadata name
     *
     * @throws IOException if the input cannot be inspected
     */
    private static void requireEnd(BufferInput in, String sName)
            throws IOException
        {
        if (in.available() != 0)
            {
            throw rejected(sName, "metadata contains trailing bytes");
            }
        }

    /**
     * Parse a decimal integer.
     *
     * @param sValue  the string value
     * @param sName   the metadata name
     *
     * @return the parsed value
     */
    private static int parseInt(String sValue, String sName)
        {
        try
            {
            return Integer.parseInt(sValue);
            }
        catch (NumberFormatException e)
            {
            throw rejected(sName, e);
            }
        }

    /**
     * Validate that the specified value is within range.
     *
     * @param sName  the metadata name
     * @param n      the value
     * @param nMin   the minimum inclusive value
     * @param nMax   the maximum inclusive value
     */
    private static void validateRange(String sName, int n, int nMin, int nMax)
        {
        if (n < nMin || n > nMax)
            {
            throw rejected(sName, "value " + n + " is outside the allowed range");
            }
        }

    /**
     * Return a controlled rejection exception.
     *
     * @param sName    the metadata name
     * @param sReason  the rejection reason
     *
     * @return a persistence exception
     */
    private static PersistenceException rejected(String sName, String sReason)
        {
        return CachePersistenceHelper.ensurePersistenceException(null,
                "Invalid persistence metadata for " + sName + ": " + sReason);
        }

    /**
     * Return a controlled rejection exception.
     *
     * @param sName  the metadata name
     * @param cause  the rejection cause
     *
     * @return a persistence exception
     */
    private static PersistenceException rejected(String sName, Throwable cause)
        {
        return CachePersistenceHelper.ensurePersistenceException(cause,
                "Invalid persistence metadata for " + sName);
        }

    // ----- inner class: QuorumObjectInputFilter ---------------------------

    /**
     * Exact class filter for retained QuorumInfo metadata.
     */
    private static class QuorumObjectInputFilter
            implements InvocationHandler
        {
        /**
         * Singleton filter.
         */
        private static final QuorumObjectInputFilter INSTANCE = new QuorumObjectInputFilter();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
            String sName = method.getName();
            if ("checkInput".equals(sName))
                {
                Class<?> clz = serialClass(args[0]);
                if (clz == null)
                    {
                    return status("UNDECIDED");
                    }

                return isAllowed(clz) ? status("ALLOWED") : status("REJECTED");
                }
            else if ("toString".equals(sName))
                {
                return getClass().getName();
                }
            else if ("hashCode".equals(sName))
                {
                return System.identityHashCode(this);
                }
            else if ("equals".equals(sName))
                {
                return proxy == args[0];
                }
            return null;
            }

        /**
         * Return true if the class is allowed by the quorum codec.
         *
         * @param clz  the class to check
         *
         * @return true if the class is allowed
         */
        private boolean isAllowed(Class<?> clz)
            {
            if (clz.isArray())
                {
                Class<?> clzComponent = clz.getComponentType();
                return clzComponent.isPrimitive() || isAllowed(clzComponent);
                }

            return clz.isPrimitive()
                    || QuorumInfo.class.getName().equals(clz.getName())
                    || QUORUM_MEMBER_CLASS.equals(clz.getName());
            }

        /**
         * Return this handler's ObjectInputFilter proxy.
         *
         * @return this handler's ObjectInputFilter proxy
         */
        private Object proxy()
            {
            Object proxy = m_proxy;
            if (proxy == null && CLZ_FILTER != null)
                {
                proxy = Proxy.newProxyInstance(CLZ_FILTER.getClassLoader(), new Class<?>[] {CLZ_FILTER}, this);
                m_proxy = proxy;
                }
            return proxy;
            }

        /**
         * This handler's ObjectInputFilter proxy.
         */
        private Object m_proxy;
        }

    /**
     * Return the serial class from an ObjectInputFilter.FilterInfo proxy.
     *
     * @param filterInfo  the filter info
     *
     * @return the serial class
     *
     * @throws Exception if the serial class cannot be read
     */
    private static Class<?> serialClass(Object filterInfo) throws Exception
        {
        return (Class<?>) METHOD_SERIAL_CLASS.invoke(filterInfo);
        }

    /**
     * Return an ObjectInputFilter.Status enum value.
     *
     * @param sStatus  the status name
     *
     * @return an ObjectInputFilter.Status enum value
     */
    private static Enum status(String sStatus)
        {
        return Enum.valueOf((Class<Enum>) CLZ_STATUS, sStatus);
        }

    /**
     * Return an ObjectInputFilter class for the current runtime.
     *
     * @return an ObjectInputFilter class for the current runtime
     */
    private static Class<?> filterClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter");
                }
            catch (ClassNotFoundException ignored)
                {
                return null;
                }
            }
        }

    /**
     * Return an ObjectInputFilter.Status class for the current runtime.
     *
     * @return an ObjectInputFilter.Status class for the current runtime
     */
    private static Class<?> statusClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter$Status");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter$Status");
                }
            catch (ClassNotFoundException ignored)
                {
                return null;
                }
            }
        }

    /**
     * Return an ObjectInputFilter.FilterInfo class for the current runtime.
     *
     * @return an ObjectInputFilter.FilterInfo class for the current runtime
     */
    private static Class<?> filterInfoClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter$FilterInfo");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter$FilterInfo");
                }
            catch (ClassNotFoundException ignored)
                {
                return null;
                }
            }
        }

    /**
     * Return the serialClass method for the current runtime FilterInfo type.
     *
     * @return the serialClass method
     */
    private static Method serialClassMethod()
        {
        try
            {
            return CLZ_FILTER_INFO == null ? null : CLZ_FILTER_INFO.getMethod("serialClass");
            }
        catch (NoSuchMethodException e)
            {
            throw new IllegalStateException(e);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Maximum encoded scalar metadata length.
     */
    private static final int MAX_SCALAR_BYTES = 64;

    /**
     * Maximum accepted partition count.
     */
    private static final int MAX_PARTITION_COUNT = 1 << 20;

    /**
     * Maximum encoded quorum metadata length.
     */
    private static final int MAX_QUORUM_BYTES = 16 * 1024 * 1024;

    /**
     * Product member implementation serialized inside retained QuorumInfo.
     */
    private static final String QUORUM_MEMBER_CLASS = "com.tangosol.coherence.component.net.Member";

    /**
     * The runtime ObjectInputFilter class.
     */
    private static final Class<?> CLZ_FILTER = filterClass();

    /**
     * The runtime ObjectInputFilter.Status class.
     */
    private static final Class<?> CLZ_STATUS = statusClass();

    /**
     * The runtime ObjectInputFilter.FilterInfo class.
     */
    private static final Class<?> CLZ_FILTER_INFO = filterInfoClass();

    /**
     * The FilterInfo.serialClass method.
     */
    private static final Method METHOD_SERIAL_CLASS = serialClassMethod();
    }
