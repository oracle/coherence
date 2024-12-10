/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.io.BufferManagers;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.io.BufferManagerWriteBufferPool;
import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.internal.util.invoke.RemotableSupport;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ClassLoaderAware;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.DeltaCompressor;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.ExternalizableLiteSerializer;
import com.tangosol.io.ExternalizableType;
import com.tangosol.io.InputStreaming;
import com.tangosol.io.MultiBufferReadBuffer;
import com.tangosol.io.MultiBufferWriteBuffer;
import com.tangosol.io.ObjectStreamFactory;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.ReadBuffer.BufferInput;
import com.tangosol.io.Resolving;
import com.tangosol.io.ResolvingObjectInputStream;
import com.tangosol.io.ResolvingObjectOutputStream;
import com.tangosol.io.SerializationSupport;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerAware;
import com.tangosol.io.Utf8Reader;
import com.tangosol.io.WrapperBufferInput;
import com.tangosol.io.WrapperBufferOutput;
import com.tangosol.io.WrapperDataInputStream;
import com.tangosol.io.WrapperDataOutputStream;
import com.tangosol.io.WrapperObjectOutputStream;
import com.tangosol.io.WrapperOutputStream;
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.WriteBuffer.BufferOutput;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PofInputStream;
import com.tangosol.io.pof.PofOutputStream;
import com.tangosol.io.pof.RawDate;
import com.tangosol.io.pof.RawDateTime;
import com.tangosol.io.pof.RawTime;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheMap;

import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.XmlBean;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlSerializable;

import com.tangosol.util.function.Remote;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.io.StringWriter;
import java.io.UTFDataFormatException;

import java.lang.Math;
import java.lang.ref.WeakReference;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.net.URL;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.WeakHashMap;

import java.util.function.BinaryOperator;

/**
* Helpers for the Serializable, Externalizable and the ExternalizableLite
* interface.
* <p>
* <b>Note:</b> This class is configured via the
* <tt>ExternalizableHelper.xml</tt> document located in the same package as
* the class. The location of the configuration file can be overridden using
* the {@link #PROPERTY_CONFIG coherence.externalizable.config} system
* property.
*
* @author cp 2003.03.28
*/
@SuppressWarnings("Duplicates")
public abstract class ExternalizableHelper
        extends BitHelper
    {
    // ----- Serializable helpers -------------------------------------------

    /**
     * Write an object to a byte array.
     *
     * @param o  the object to write into a byte array
     *
     * @return a byte array containing the serialized form of the passed object
     *
     * @throws WrapperException  may contain an IOException
     */
    public static byte[] toByteArray(Object o)
        {
        return toByteArray(o, ensureSerializer(null));
        }

    /**
     * Write an object to a byte array using the specified Serializer.
     *
     * @param o           the object to write into a byte array
     * @param serializer  the Serializer to use
     *
     * @return a byte array containing the serialized form of the passed object
     *
     * @throws WrapperException  may contain an IOException
     */
    public static byte[] toByteArray(Object o, Serializer serializer)
        {
        try
            {
            return serializeInternal(serializer, o, false).toByteArray();
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
     * Convert a long to an 8-byte byte array.
     *
     * @param l   the long to convert
     * @param ab  the byte array to populate or null if a new byte array should
     *            be created
     *
     * @return an 8-byte byte array that represents the given long
     *
     * @see #toLong(byte[])
     */
    public static byte[] toByteArray(long l, byte[] ab)
        {
        if (ab == null || ab.length < 8)
            {
            ab = new byte[8];
            }

        // hi word
        int n = (int)  (l >>> 32);
        ab[0] = (byte) (n >>> 24);
        ab[1] = (byte) (n >>> 16);
        ab[2] = (byte) (n >>>  8);
        ab[3] = (byte) (n);

        // lo word
        n = (int) l;
        ab[4] = (byte) (n >>> 24);
        ab[5] = (byte) (n >>> 16);
        ab[6] = (byte) (n >>>  8);
        ab[7] = (byte) (n);

        return ab;
        }

    /**
     * Convert a byte array to a long.
     *
     * @param ab  the byte array to convert
     *
     * @return a long based on the provided byte array
     *
     * @see #toByteArray(long, byte[])
     */
    public static long toLong(byte[] ab)
        {
        return Byte.toUnsignedLong(ab[0]) << 56L |
               Byte.toUnsignedLong(ab[1]) << 48L |
               Byte.toUnsignedLong(ab[2]) << 40L |
               Byte.toUnsignedLong(ab[3]) << 32L |
               Byte.toUnsignedLong(ab[4]) << 24L |
               Byte.toUnsignedInt (ab[5]) << 16L |
               Byte.toUnsignedInt (ab[6]) <<  8L |
               Byte.toUnsignedInt (ab[7]);
        }

    /**
     * Read an object from a byte array.
     *
     * @param ab  the byte array containing the object
     *
     * @return  the object deserialized from the byte array
     *
     * @throws WrapperException  may contain an IOException
     */
    public static Object fromByteArray(byte[] ab)
        {
        return fromByteArray(ab, null);
        }

    /**
     * Read an object from a byte array.
     *
     * @param ab      the byte array containing the object
     * @param loader  the ClassLoader to use
     *
     * @return  the object deserialized from the byte array
     *
     * @throws WrapperException  may contain an IOException
     */
    public static Object fromByteArray(byte[] ab, ClassLoader loader)
        {
        try
            {
            return deserializeInternal(ensureSerializer(loader),
                new ByteArrayReadBuffer(ab), null /* supplier */, Object.class);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
     * Write an object into a Binary object.
     *
     * @param o  the object to write into a Binary object
     *
     * @return  a Binary object containing a serialized form of the passed
     *          object
     *
     * @throws WrapperException  may contain an IOException
     */
    public static Binary toBinary(Object o)
        {
        return toBinary(o, ensureSerializer(null));
        }

    /**
     * Write an object into a Binary object using the specified Serializer.
     *
     * @param o           the object to write into a Binary object
     * @param serializer  the Serializer to use
     *
     * @return  a Binary object containing a serialized form of the passed
     *          object
     *
     * @throws WrapperException  may contain an IOException
     */
    public static Binary toBinary(Object o, Serializer serializer)
        {
        try
            {
            return serializeInternal(serializer, o, true).toBinary();
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
     * Write an object into a Binary object using the specified Serializer.
     *
     * @param o           the object to write into a Binary object
     * @param serializer  the Serializer to use
     * @param buf         the reusable WriteBuffer to serialize into; this buffer
     *                    is not safe to reuse until the returned Binary has been
     *                    disposed of
     *
     * @return  a Binary object containing a serialized form of the passed
     *          object
     *
     * @throws WrapperException  may contain an IOException
     */
    public static Binary toBinary(Object o, Serializer serializer, WriteBuffer buf)
        {
        try
            {
            return serializeInternal(serializer, o, true, buf).toBinary();
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
     * Read an object from a Binary object.
     *
     * @param <T>  the class of the deserialized object
     * @param bin  the Binary object containing the serialized object
     *
     * @return  the object deserialized from the Binary object
     *
     * @throws WrapperException  may contain an IOException
     */
    public static <T> T fromBinary(Binary bin)
        {
        return fromBinary(bin, ensureSerializer(null));
        }

    /**
     * Read an object from a Binary object.
     *
     * @param <T>     the class of the deserialized object
     * @param bin     the Binary object containing the serialized object
     * @param loader  the ClassLoader to use
     *
     * @return  the object deserialized from the Binary object
     *
     * @throws WrapperException  may contain an IOException
     */
    public static <T> T fromBinary(Binary bin, ClassLoader loader)
        {
        return fromBinary(bin, ensureSerializer(loader));
        }

    /**
     * Read an object from a Binary object using the specified Serializer.
     *
     * @param <T>         the class of the deserialized object
     * @param bin         the Binary object containing the serialized object
     * @param serializer  the Serializer to use
     *
     * @return  the object deserialized from the Binary object
     *
     * @throws WrapperException  may contain an IOException
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromBinary(Binary bin, Serializer serializer)
        {
        return (T) fromBinary(bin, serializer, null, Object.class);
        }

    /**
     * Read an object from a Binary object using the specified Serializer and expected class.
     *
     * @param <T>         the class of the deserialized object
     * @param bin         the Binary object containing the serialized object
     * @param serializer  the Serializer to use
     * @param clazz       deserialize object as an instance of this class
     *
     * @return  the object deserialized from the Binary object
     *
     * @throws RuntimeException  may contain an IOException
     */
    public static <T> T fromBinary(Binary bin, Serializer serializer, Class<T> clazz)
        {
        return fromBinary(bin, serializer, null /* supplier */, clazz);
        }

    /**
     * Read an object from a Binary object using the specified Serializer.
     *
     * @param <T>         the class of the deserialized object
     * @param bin         the Binary object containing the serialized object
     * @param serializer  the Serializer to use
     * @param supplier    an optional Function that given a BufferInput returns
     *                    either the same or another BufferInput
     *
     * @return  the object deserialized from the Binary object
     *
     * @throws RuntimeException  may contain an IOException
     */
    public static <T> T fromBinary(Binary bin, Serializer serializer, Remote.Function<BufferInput, BufferInput> supplier)
        {
        try
            {
            return (T) deserializeInternal(serializer, bin, supplier, Object.class);
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Read an object from a Binary object using the specified Serializer and expected class.
     *
     * @param <T>         the class of the deserialized object
     * @param bin         the Binary object containing the serialized object
     * @param serializer  the Serializer to use
     * @param supplier    an optional Function that given a BufferInput returns
     *                    either the same or another BufferInput
     * @param clazz       deserialize object as an instance of this class
     *
     * @return  the object deserialized from the Binary object
     *
     * @throws RuntimeException  may contain an IOException
     */
    public static <T> T fromBinary(Binary bin, Serializer serializer, Remote.Function<BufferInput, BufferInput> supplier, Class<T> clazz)
        {
        try
            {
            return deserializeInternal(serializer, bin, supplier, clazz);
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Obtain a Serializer for the specified ClassLoader. This method is
     * intended to provide configurable indirection for the serialization of
     * application objects.
     *
     * @param loader  a ClassLoader
     *
     * @return the Serializer to use with the specified ClassLoader
     */
    public static Serializer ensureSerializer(ClassLoader loader)
        {
        loader = ensureClassLoader(loader);

        return s_mapSerializerByClassLoader.computeIfAbsent(loader, ExternalizableHelper::createDefaultSerializer);
        }

    /**
     * Create default serializer for the specified ClassLoader.
     *
     * @param loader  a ClassLoader
     *
     * @return the Serializer to use with the specified ClassLoader
     */
    private static Serializer createDefaultSerializer(ClassLoader loader)
        {
        Serializer ser = USE_POF_STREAMS && loader != NullImplementation.getClassLoader()
                         ? new ConfigurablePofContext()
                         : new DefaultSerializer();
        ((ClassLoaderAware) ser).setContextClassLoader(loader);
        return ser;
        }

    /**
     * Write an ExternalizableLite object into a Binary object. Unlike the
     * {@link #toBinary(Object) toBinary}, this method only serializes the
     * object's content and not the identity part. To reconstruct the object
     * frm that binary you would instantiate and "read" it as follows:
     * <pre>
     *   ExternalizableLite o = new MyLiteObject();
     *   o.readExternal(bin.getBufferInput());
     * </pre>
     *
     * @param o  the ExternalizableLite object to write into a Binary object
     *
     * @return  a Binary object containing a serialized form of the passed
     *          object
     *
     * @throws WrapperException  may contain an IOException
     */
    public static Binary toLiteBinary(ExternalizableLite o)
        {
        try
            {
            WriteBuffer buffer = new BinaryWriteBuffer(64);
            o.writeExternal(buffer.getBufferOutput());
            return buffer.toBinary();
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
     * Determine whether the sender of the content (the given DataInput)
     * runs a version that supersedes (greater or equal to) the specified
     * version.
     *
     * @param in  the DataInput to interrogate
     * @param nMajor     the major version
     * @param nMinor     the minor version
     * @param nMicro     the micro version
     * @param nPatchSet  the patch set version
     * @param nPatch     the patch version
     *
     * @return true iff the sender's version is greater or equal to the
     *         specified one
     *
     * @throws IllegalArgumentException if the DataInput is not a {@link
     *         WrapperBufferInput.VersionAwareBufferInput VersionAwareBufferInput}
     */
    public static boolean isVersionCompatible(DataInput in, int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        if (!(in instanceof WrapperBufferInput.VersionAwareBufferInput))
            {
            throw new IllegalArgumentException("Unexpected DataInput");
            }

        return ((WrapperBufferInput.VersionAwareBufferInput) in)
                .isVersionCompatible(nMajor, nMinor, nMicro, nPatchSet, nPatch);
        }

    /**
     * Determine whether the sender of the content (the given DataInput)
     * runs a version that supersedes (greater or equal to) the specified
     * version.
     *
     * @param in      the DataInput to interrogate
     * @param nYear   the year segment of the calendar based version
     * @param nMonth  the month segment of the calendar based version
     * @param nPatch  the patch segment of the calendar based version
     *
     * @return true iff the sender's version is greater or equal to the
     *         specified one
     *
     * @throws IllegalArgumentException if the DataInput is not a {@link
     *         WrapperBufferInput.VersionAwareBufferInput VersionAwareBufferInput}
     */
    public static boolean isVersionCompatible(DataInput in, int nYear, int nMonth, int nPatch)
        {
        if (!(in instanceof WrapperBufferInput.VersionAwareBufferInput))
            {
            throw new IllegalArgumentException("Unexpected DataInput");
            }

        return ((WrapperBufferInput.VersionAwareBufferInput) in)
                .isVersionCompatible(nYear, nMonth, nPatch);
        }

    /**
     * Determine whether the sender of the content (the given DataInput)
     * runs a version that supersedes (greater or equal to) the specified
     * version.
     *
     * @param in               the DataInput to interrogate
     * @param nEncodedVersion  the encoded version
     *
     * @return true iff the sender's version is greater or equal to the
     *         specified one
     *
     * @throws IllegalArgumentException if the DataInput is not a {@link
     *         WrapperBufferInput.VersionAwareBufferInput VersionAwareBufferInput}
     */
    public static boolean isVersionCompatible(DataInput in, int nEncodedVersion)
        {
        if (!(in instanceof WrapperBufferInput.VersionAwareBufferInput))
            {
            throw new IllegalArgumentException("Unexpected DataInput");
            }

        return ((WrapperBufferInput.VersionAwareBufferInput) in)
                .isVersionCompatible(nEncodedVersion);
        }

    /**
     * Determine whether all the recipients of the content (the given DataOutput)
     * run versions that supersede (greater or equal to) the specified
     * version.
     *
     * @param out        the DataOutput to interrogate
     * @param nMajor     the major version
     * @param nMinor     the minor version
     * @param nMicro     the micro version
     * @param nPatchSet  the patch set version
     * @param nPatch     the patch version
     *
     * @return true iff the sender's version is greater or equal to the
     *         specified one
     *
     * @throws IllegalArgumentException if the DataOutput is not a {@link
     *         WrapperBufferOutput.VersionAwareBufferOutput VersionAwareBufferOutput}
     */
    public static boolean isVersionCompatible(DataOutput out, int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        if (!(out instanceof WrapperBufferOutput.VersionAwareBufferOutput))
            {
            throw new IllegalArgumentException("Unexpected DataOutput");
            }

        return ((WrapperBufferOutput.VersionAwareBufferOutput) out)
                .isVersionCompatible(nMajor, nMinor, nMicro, nPatchSet, nPatch);
        }

    /**
     * Determine whether all the recipients of the content (the given DataOutput)
     * run versions that supersede (greater or equal to) the specified
     * version.
     *
     * @param out     the DataOutput to interrogate
     * @param nYear   the year segment of the calendar based version
     * @param nMonth  the month segment of the calendar based version
     * @param nPatch  the patch segment of the calendar based version
     *
     * @return true iff the sender's version is greater or equal to the
     *         specified one
     *
     * @throws IllegalArgumentException if the DataOutput is not a {@link
     *         WrapperBufferOutput.VersionAwareBufferOutput VersionAwareBufferOutput}
     */
    public static boolean isVersionCompatible(DataOutput out, int nYear, int nMonth, int nPatch)
        {
        if (!(out instanceof WrapperBufferOutput.VersionAwareBufferOutput))
            {
            throw new IllegalArgumentException("Unexpected DataOutput");
            }

        return ((WrapperBufferOutput.VersionAwareBufferOutput) out)
                .isVersionCompatible(nYear, nMonth, nPatch);
        }

    /**
     * Determine whether all the recipients of the content (the given DataOutput)
     * run versions that supersede (greater or equal to) the specified
     * version.
     *
     * @param out              the DataOutput to interrogate
     * @param nEncodedVersion  the encoded version
     *
     * @return true iff the sender's version is greater or equal to the
     *         specified one
     *
     * @throws IllegalArgumentException if the DataOutput is not a {@link
     *         WrapperBufferOutput.VersionAwareBufferOutput VersionAwareBufferOutput}
     */
    public static boolean isVersionCompatible(DataOutput out, int nEncodedVersion)
        {
        if (!(out instanceof WrapperBufferOutput.VersionAwareBufferOutput))
            {
            throw new IllegalArgumentException("Unexpected DataOutput");
            }

        return ((WrapperBufferOutput.VersionAwareBufferOutput) out)
                .isVersionCompatible(nEncodedVersion);
        }

    /**
     * Determine whether all the sender of the content (the given DataInput)
     * run versions that are the same version with the same or greater patch level.
     *
     * @param in              the DataInput to interrogate
     * @param nEncodedVersion  the encoded version to check
     *
     * @return true iff the sender's version is the same with a greater or equal patch
     *
     * @throws IllegalArgumentException if the DataOutput is not a {@link
     *         WrapperBufferOutput.VersionAwareBufferOutput VersionAwareBufferOutput}
     */
    public static boolean isPatchCompatible(DataInput in, int nEncodedVersion)
        {
        if (!(in instanceof WrapperBufferInput.VersionAwareBufferInput))
            {
            throw new IllegalArgumentException("Unexpected DataInput");
            }

        return ((WrapperBufferInput.VersionAwareBufferInput) in)
                .isPatchCompatible(nEncodedVersion);
        }

    /**
     * Determine whether all the recipients of the content (the given DataOutput)
     * run versions that are the same version with the same or greater patch level.
     *
     * @param out              the DataOutput to interrogate
     * @param nEncodedVersion  the encoded version to check
     *
     * @return true iff the recipient's version is the same with a greater or equal patch
     *
     * @throws IllegalArgumentException if the DataOutput is not a {@link
     *         WrapperBufferOutput.VersionAwareBufferOutput VersionAwareBufferOutput}
     */
    public static boolean isPatchCompatible(DataOutput out, int nEncodedVersion)
        {
        if (!(out instanceof WrapperBufferOutput.VersionAwareBufferOutput))
            {
            throw new IllegalArgumentException("Unexpected DataOutput");
            }

        return ((WrapperBufferOutput.VersionAwareBufferOutput) out)
                .isPatchCompatible(nEncodedVersion);
        }


    // ----- Externalizable helpers -----------------------------------------

    /**
     * Read a signed three-byte integer value from a stream.
     *
     * @param in  DataInput stream to read from
     *
     * @return a three-byte signed integer value as an int
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static int readTrint(DataInput in)
            throws IOException
        {
        int n;

        if (in instanceof PofInputStream)
            {
            n = in.readInt();
            }
        else
            {
            n =   (in.readUnsignedByte() << 16)
                | (in.readUnsignedByte() <<  8)
                | (in.readUnsignedByte()      );

            if ((n & 0x800000) != 0)
                {
                // sign-extend the negative value to 4 bytes
                n |= 0xFF000000;
                }
            }

        return n;
        }

    /**
     * Read an unsigned three-byte integer value from a stream.
     *
     * @param in  DataInput stream to read from
     *
     * @return a three-byte unsigned integer value as an int
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static int readUnsignedTrint(DataInput in)
            throws IOException
        {
        int n;

        if (in instanceof PofInputStream)
            {
            n = in.readInt();
            }
        else
            {
            n =   (in.readUnsignedByte() << 16)
                | (in.readUnsignedByte() <<  8)
                | (in.readUnsignedByte()      );
            }

        return n;
        }

    /**
     * Write a three-byte integer value to a stream.
     *
     * @param out  DataOutput stream to write to
     * @param n    a three-byte integer value passed as an int
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static void writeTrint(DataOutput out, int n)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            out.writeInt(n);
            }
        else
            {
            out.writeByte(n >>> 16);
            out.writeByte(n >>>  8);
            out.writeByte(n       );
            }
        }

    /**
     * Write a three-byte integer value to a stream.
     *
     * @param out  DataOutput stream to write to
     * @param l    a three-byte integer value passed as an long
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static void writeTrint(DataOutput out, long l)
            throws IOException
        {
        writeTrint(out, (int) (l & 0xFFFFFFL));
        }

    /**
     * Convert a long integer to a trint.
     *
     * @param l  the long value to convert to a trint
     *
     * @return  the equivalent unsigned 3-byte integer value (a "trint")
     */
    public static int makeTrint(long l)
        {
        return (int) (l & 0xFFFFFFL);
        }

    /**
     * Read an int that uses variable length storage in the buffer.
     *
     * @param in  a BufferInput to read from
     *
     * @return an int value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static int readInt(BufferInput in)
            throws IOException
        {
        return in.readPackedInt();
        }

    /**
     * Read an int that uses variable length storage in the stream.
     *
     * @param in  a DataInput stream to read from
     *
     * @return an int value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static int readInt(DataInput in)
            throws IOException
        {
        int n;

        if (in instanceof BufferInput)
            {
            n = ((BufferInput) in).readPackedInt();
            }
        else if (in instanceof PofInputStream)
            {
            n = in.readInt();
            }
        else
            {
            // this is an inlined version of BufferInput#readPackedInt()
            int     b     = in.readUnsignedByte();
                    n     = b & 0x3F;       // 6 bits of data in first byte
            int     cBits = 6;
            boolean fNeg  = (b & 0x40) != 0;    // seventh bit is a sign bit

            while ((b & 0x80) != 0)     // eighth bit is the continuation bit
                {
                b      = in.readUnsignedByte();
                n     |= ((b & 0x7F) << cBits);
                cBits += 7;
                }

            if (fNeg)
                {
                n = ~n;
                }
            }

        return n;
        }

    /**
     * Calculate the number of bytes needed to store a packed integer using
     * a variable-length format.
     * <p>
     * The format differs from DataOutput in that DataOutput always uses
     * a fixed-length 4-byte Big Endian binary format for int values.
     * The "packed" format includes a sign bit (0x40) and a continuation
     * bit (0x80) in the first byte, followed by the least 6 significant
     * bits of the int value. Subsequent bytes (each appearing only if
     * the previous byte had its continuation bit set) include a
     * continuation bit (0x80) and the next least 7 significant bits of
     * the int value. In this way, a 32-bit value is encoded into 1-5
     * bytes, depending on the magnitude of the int value being encoded.
     *
     * @param n  the value to calculate the packed length of
     *
     * @return the number of bytes needed to store the value
     */
    public static int calculatePackedLength(int n)
        {
        if (n < 0)
            {
            n = ~n;
            }
        return n < 0x40 ? 1 : (0x27 - Integer.numberOfLeadingZeros(n)) / 7;
        }

    /**
     * Write an int to a buffer using a variable length of storage.
     *
     * @param out  a BufferOutput to write to
     * @param n    an int value to write
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static void writeInt(BufferOutput out, int n)
            throws IOException
        {
        out.writePackedInt(n);
        }

    /**
     * Write an int to a stream using a variable length of storage.
     *
     * @param out  a DataOutput stream to write to
     * @param n    an int value to write
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static void writeInt(DataOutput out, int n)
            throws IOException
        {
        if (out instanceof BufferOutput)
            {
            ((BufferOutput) out).writePackedInt(n);
            }
        else if (out instanceof PofOutputStream)
            {
            out.writeInt(n);
            }
        else
            {
            // this is an inlined version of BufferOutput#writePackedInt()

            // first byte contains sign bit (bit 7 set if neg)
            int b = 0;
            if (n < 0)
                {
                b = 0x40;
                n = ~n;
                }

            // first byte contains only 6 data bits
            b |= (byte) (n & 0x3F);
            n >>>= 6;

            while (n != 0)
                {
                b |= 0x80; // bit 8 is a continuation bit
                out.writeByte(b);

                b = (n & 0x7F);
                n >>>= 7;
                }

            // remaining byte
            out.writeByte(b);
            }
        }

    /**
     * Read a 2-dimensional int-array from the stream.
     *
     * @param in  the input stream to read from
     *
     * @return the 2-dimensional int-array
     *
     * @throws IOException if an I/O Exception occurs
     */
    public static int[][] readIntArray2d(DataInput in)
            throws IOException
        {
        int     cSizeOuter = readInt(in);
        int     cSizeInner = readInt(in);
        int[][] aai        = new int[cSizeOuter][cSizeInner];
        for (int i = 0; i < cSizeOuter; i++)
            {
            int[] ai = aai[i];
            for (int j = 0; j < cSizeInner; j++)
                {
                ai[j] = readInt(in);
                }
            }

        return aai;
        }

    /**
     * Write a 2-dimensional int-array to the stream
     *
     * @param out  the output stream to write to
     * @param aai  the 2-dimensional int-array to write
     *
     * @throws IOException if an I/O Exception occurs
     * @throws NullPointerException if the array is null
     */
    public static void writeIntArray2d(DataOutput out, int[][] aai)
            throws IOException
        {
        int cSizeOuter = aai.length;
        int cSizeInner = cSizeOuter == 0 ? 0 : aai[0].length;

        writeInt(out, cSizeOuter);
        writeInt(out, cSizeInner);
        for (int i = 0; i < cSizeOuter; i++)
            {
            int[] ai = aai[i];
            for (int j = 0; j < cSizeInner; j++)
                {
                writeInt(out, ai[j]);
                }
            }
        }

    /**
     * Read a long that uses variable length storage in the buffer.
     *
     * @param in  a DataInput stream to read from
     *
     * @return a long value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static long readLong(BufferInput in)
            throws IOException
        {
        return in.readPackedLong();
        }

    /**
     * Read a long that uses variable length storage in the stream.
     *
     * @param in  a DataInput stream to read from
     *
     * @return a long value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static long readLong(DataInput in)
            throws IOException
        {
        long l;

        if (in instanceof BufferInput)
            {
            l = ((BufferInput) in).readPackedLong();
            }
        else if (in instanceof PofInputStream)
            {
            l = in.readLong();
            }
        else
            {
            // this is an inlined version of BufferInput#readPackedLong()
            int     b     = in.readUnsignedByte();
                    l     = b & 0x3F;   // only 6 bits of data in first byte
            int     cBits = 6;
            boolean fNeg  = (b & 0x40) != 0;    // seventh bit is a sign bit

            while ((b & 0x80) != 0)     // eighth bit is the continuation bit
                {
                b      = in.readUnsignedByte();
                l     |= (((long) (b & 0x7F)) << cBits);
                cBits += 7;
                }

            if (fNeg)
                {
                l = ~l;
                }
            }

        return l;
        }

    /**
     * Calculate the number of bytes needed to store a packed long using
     * a variable-length format.
     * <p>
     * The format differs from DataOutput in that DataOutput always uses
     * a fixed-length 8-byte Big Endian binary format for long values.
     * The "packed" format includes a sign bit (0x40) and a continuation
     * bit (0x80) in the first byte, followed by the least 6 significant
     * bits of the long value. Subsequent bytes (each appearing only if
     * the previous byte had its continuation bit set) include a
     * continuation bit (0x80) and the next least 7 significant bits of
     * the long value. In this way, a 64-bit value is encoded into 1-10
     * bytes, depending on the magnitude of the long value being encoded.
     *
     * @param l  the long value to calculate the packed length of
     *
     * @return the number of bytes needed to store the value
     */
    public static int calculatePackedLength(long l)
        {
        if (l < 0)
            {
            l = ~l;
            }
        return l < 0x40 ? 1 : (0x47 - Long.numberOfLeadingZeros(l)) / 7;
        }

    /**
     * Write a long to a buffer using a variable length of storage.
     *
     * @param out  a BufferOutput stream to write to
     * @param l    a long value to write
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static void writeLong(BufferOutput out, long l)
            throws IOException
        {
        out.writePackedLong(l);
        }

    /**
     * Write a long to a stream using a variable length of storage.
     *
     * @param out  a DataOutput stream to write to
     * @param l    a long value to write
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static void writeLong(DataOutput out, long l)
            throws IOException
        {
        if (out instanceof BufferOutput)
            {
            ((BufferOutput) out).writePackedLong(l);
            }
        else if (out instanceof PofOutputStream)
            {
            out.writeLong(l);
            }
        else
            {
            // this is an inlined version of BufferOutput#writePackedLong()

            // first byte contains sign bit (bit 7 set if neg)
            int b = 0;
            if (l < 0)
                {
                b = 0x40;
                l = ~l;
                }

            // first byte contains only 6 data bits
            b |= (byte) (((int) l) & 0x3F);
            l >>>= 6;

            while (l != 0)
                {
                b |= 0x80; // bit 8 is a continuation bit
                out.writeByte(b);

                b = (((int) l) & 0x7F);
                l >>>= 7;
                }

            // remaining byte
            out.writeByte(b);
            }
        }

    /**
     * Read a packed boolean array.
     *
     * @param in  a DataInput stream to read from
     *
     * @return a boolean array value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static boolean[] readBooleanArray(DataInput in)
            throws IOException
        {
        boolean[] af;

        if (in instanceof PofInputStream)
            {
            af = (boolean[]) ((PofInputStream) in).readObject();
            }
        else
            {
            int c = readInt(in);

            validateLoadArray(boolean[].class, c, in);
            af = c < CHUNK_THRESHOLD
                    ? readBooleanArray(in, c)
                    : readLargeBooleanArray(in, c);
            }

        return af;
        }

    /**
     * Write a packed boolean array.
     *
     * @param out  a DataOutput stream to write to
     * @param af   a boolean array value to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeBooleanArray(DataOutput out, boolean[] af)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(af);
            }
        else
            {
            int c  = af.length;
            writeInt(out, c);

            for (int of = 0, cb = (c + 7) / 8, i = 0; of < cb; ++of)
                {
                int nBits = 0;
                for (int nMask = 1; i < c && nMask <= 0xFF; nMask <<= 1)
                    {
                    if (af[i++])
                        {
                        nBits |= nMask;
                        }
                    }
                out.writeByte(nBits);
                }
            }
        }

    /**
     * Read a variable-length encoded byte array.
     *
     * @param in  a DataInput stream to read from
     *
     * @return a byte array value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static byte[] readByteArray(DataInput in)
            throws IOException
        {
        byte[] ab;

        if (in instanceof PofInputStream)
            {
            ab = (byte[]) ((PofInputStream) in).readObject();
            }
        else
            {
            int cb = readInt(in);

            validateLoadArray(byte[].class, cb, in);
            if (cb < CHUNK_THRESHOLD)
                {
                ab = new byte[cb];
                in.readFully(ab);
                }
            else
                {
                ab = readLargeByteArray(in, cb);
                }
            }

        return ab;
        }

    /**
     * Write a variable-length encoded byte array.
     *
     * @param out  a DataOutput stream to write to
     * @param ab   a byte array value to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeByteArray(DataOutput out, byte[] ab)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(ab);
            }
        else
            {
            int cb = ab.length;
            writeInt(out, cb);
            out.write(ab, 0, cb);
            }
        }

    /**
     * Read a variable-length encoded UTF packed String. The major difference
     * between this implementation and DataInputStream is that this is not
     * limited to 64KB String values.
     * <p>
     * Note: This format changed in Coherence 3.0; previously the leading
     * integer value was the number of characters, and currently it is the
     * number of bytes as per the java.io package implementations.
     *
     * @param in  a DataInput stream to read from
     *
     * @return a String value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static String readUTF(DataInput in)
            throws IOException
        {
        String s;

        if (in instanceof BufferInput)
            {
            s = ((BufferInput) in).readSafeUTF();
            }
        else if (in instanceof PofInputStream)
            {
            s = in.readUTF();
            }
        else
            {
            int cb = readInt(in);
            if (cb < 0)
                {
                return null;
                }
            else if (cb == 0)
                {
                return "";
                }

            // get the "UTF binary"
            // per JDK serialization filtering doc:
            //     The filter is not called ... for java.lang.String instances that are encoded concretely in the stream.
            byte[] ab;
            if (cb < CHUNK_THRESHOLD)
                {
                ab = new byte[cb];
                in.readFully(ab);
                }
            else
                {
                ab = readLargeByteArray(in, cb);
                }

            s = convertUTF(ab, 0, cb, new char[cb]);
            }

        return s;
        }

    /**
     * Convert binary UTF-8 encode data to a String. This method is a helper
     * to allow various I/O implementations to share a single, efficient
     * implementation.
     *
     * @param ab   an array of bytes containing UTF-8 encoded characters
     * @param of   the offset into the array of the UTF-8 data to decode
     * @param cb   the binary length in the array of the UTF-8 data to decode
     * @param ach  a temp char array large enough to convert the UTF into
     *
     * @return a String value
     *
     * @throws UTFDataFormatException  if the UTF data is corrupt
     */
    public static String convertUTF(byte[] ab, int of, int cb, char[] ach)
            throws UTFDataFormatException
        {
        // first run through the bytes determining if we have to
        // translate them at all (they might all be in the range 0-127)
        boolean fAscii = true;
        int     ofch   = 0;
        int     ofAsc  = of;
        int     ofEnd  = of + cb;

        // read 8 bytes at a time into a long to speed up comparison
        ByteBuffer buf = ByteBuffer.wrap(ab, of, cb);
        int cLongs = cb / 8;
        for (int i = 0; i < cLongs; i++)
            {
            long l = buf.getLong();
            if ((l & 0x8080808080808080L) == 0)
                {
                ofAsc += 8;
                }
            else
                {
                // it's not all "ascii" data
                fAscii = false;
                break;
                }
            }

        // compare remaining bytes one by one
        if (fAscii)
            {
            for (; ofAsc < ofEnd; ++ofAsc)
                {
                int n = ab[ofAsc];
                if (n < 0)
                    {
                    // it's not all "ascii" data
                    fAscii = false;
                    break;
                    }
                }
            }

        // the string contains non-ASCII characters; do full UTF-8 conversion
        if (!fAscii)
            {
            // copy initial ASCII characters directly
            if (ofAsc > of)
                {
                CharBuffer bufCh = CharBuffer.wrap(ach);
                StandardCharsets.ISO_8859_1.newDecoder().decode(buf.slice(of, ofAsc - of), bufCh, true);
                ofch = bufCh.position();
                }
            
            // process remaining characters
            for ( ; ofAsc < ofEnd; ++ofAsc)
                {
                int ch = ab[ofAsc] & 0xFF;
                switch ((ch & 0xF0) >>> 4)
                    {
                    case 0x0: case 0x1: case 0x2: case 0x3:
                    case 0x4: case 0x5: case 0x6: case 0x7:
                        // 1-byte format:  0xxx xxxx
                        ach[ofch++] = (char) ch;
                        break;

                    case 0xC: case 0xD:
                        {
                        // 2-byte format:  110x xxxx, 10xx xxxx
                        int ch2 = ab[++ofAsc] & 0xFF;
                        if ((ch2 & 0xC0) != 0x80)
                            {
                            throw new UTFDataFormatException();
                            }
                        ach[ofch++] = (char) (((ch & 0x1F) << 6) | ch2 & 0x3F);
                        break;
                        }

                    case 0xE:
                        {
                        // 3-byte format:  1110 xxxx, 10xx xxxx, 10xx xxxx
                        int ch2 = ab[++ofAsc] & 0xFF;
                        int ch3 = ab[++ofAsc] & 0xFF;
                        if ((ch2 & 0xC0) != 0x80 || (ch3 & 0xC0) != 0x80)
                            {
                            throw new UTFDataFormatException();
                            }
                        ach[ofch++] = (char) (((ch  & 0x0F) << 12) |
                                              ((ch2 & 0x3F) <<  6) |
                                              ((ch3 & 0x3F)      )  );
                        break;
                        }

                    case 0xF:
                        {
                        // 4-byte format:  1111 xxxx, 10xx xxxx, 10xx xxxx, 10xx xxxx (supplemental plane)
                        int ch2 = ab[++ofAsc] & 0xFF;
                        int ch3 = ab[++ofAsc] & 0xFF;
                        int ch4 = ab[++ofAsc] & 0xFF;
                        if ((ch2 & 0xC0) != 0x80 || (ch3 & 0xC0) != 0x80 || (ch4 & 0xC0) != 0x80)
                            {
                            throw new UTFDataFormatException();
                            }

                        int cp = (ch  & 0x07) << 18 |
                                 (ch2 & 0x3F) << 12 |
                                 (ch3 & 0x3F) <<  6 |
                                 (ch4 & 0x3F);

                        cp = cp - 0x10000;

                        char high = (char) (0xD800 + ((cp >> 10) & 0x3FF));
                        char low  = (char) (0xDC00 + (cp & 0x3FF));
                        ach[ofch++] = high;
                        ach[ofch++] = low;

                        break;
                        }

                    default:
                        throw new UTFDataFormatException(
                                "illegal leading UTF byte: " + ch);
                    }
                }
            }

        return fAscii   // all characters can be represented by a single byte, use Latin1
               ? new String(ab, of, cb, StandardCharsets.ISO_8859_1)
               : new String(ach, 0, ofch);
        }

    /**
     * Write a variable-length encoded UTF packed String. The major difference
     * between this implementation and DataOutput stream is that this is not
     * limited to 64KB String values.
     * <p>
     * Note: This format changed in Coherence 3.0; previously the leading
     * integer value was the number of characters, and currently it is the
     * number of bytes as per the java.io package implementations.
     *
     * @param out  a DataOutput stream to write to
     * @param s    a String value to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeUTF(DataOutput out, String s)
            throws IOException
        {
        if (out instanceof BufferOutput)
            {
            ((BufferOutput) out).writeSafeUTF(s);
            }
        else if (out instanceof PofOutputStream)
            {
            out.writeUTF(s);
            }
        else
            {
            // this is an inlined version of BufferOutput#writeSafeUTF()

            if (s == null)
                {
                writeInt(out, -1);
                }
            else
                {
                int cch = s.length();
                if (cch == 0)
                    {
                    writeInt(out, 0);
                    }
                else
                    {
                    // calculate the length (in bytes) of the resulting UTF
                    int cb = cch;
                    for (int ofch = 0; ofch < cch; ++ofch)
                        {
                        int ch = s.charAt(ofch);
                        if (ch <= 0x007F)
                            {
                            // all bytes in this range use the 1-byte format
                            // except for 0
                            if (ch == 0)
                                {
                                ++cb;
                                }
                            }
                        else
                            {
                            // either a 2-byte format or a 3-byte format (if
                            // over 0x07FF)
                            cb += (ch <= 0x07FF ? 1 : 2);
                            }
                        }

                    // write the UTF header (the length)
                    writeInt(out, cb);

                    // get a temp buffer to write the UTF binary into
                    byte[] ab = new byte[cb];

                    // write the UTF into the temp buffer
                    if (cb == cch)
                        {
                        // ask the string to convert itself to ascii bytes
                        s.getBytes(0, cch, ab, 0);
                        }
                    else
                        {
                        for (int ofch = 0, ofb = 0; ofch < cch; ++ofch)
                            {
                            int ch = s.charAt(ofch);
                            if (ch >= 0x0001 && ch <= 0x007F)
                                {
                                // 1-byte format:  0xxx xxxx
                                ab[ofb++] = (byte) ch;
                                }
                            else if (ch <= 0x07FF)
                                {
                                // 2-byte format:  110x xxxx, 10xx xxxx
                                ab[ofb++] = (byte) (0xC0 | ((ch >>> 6) & 0x1F));
                                ab[ofb++] = (byte) (0x80 | ((ch      ) & 0x3F));
                                }
                            else
                                {
                                // 3-byte format:  1110 xxxx, 10xx xxxx, 10xx xxxx
                                ab[ofb++] = (byte) (0xE0 | ((ch >>> 12) & 0x0F));
                                ab[ofb++] = (byte) (0x80 | ((ch >>>  6) & 0x3F));
                                ab[ofb++] = (byte) (0x80 | ((ch       ) & 0x3F));
                                }
                            }
                        }

                    // write the temp buffer to this WriteBuffer
                    out.write(ab, 0, cb);
                    }
                }
            }
        }

    /**
     * Read a variable-length encoded UTF packed String in the buffer.
     *
     * @param in  a BufferInput to read from
     *
     * @return a String value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static String readSafeUTF(BufferInput in)
            throws IOException
        {
        return in.readSafeUTF();
        }

    /**
     * Read a variable-length encoded UTF packed String. The major difference
     * between this implementation and DataInputStream is that this is not
     * limited to 64KB String values and allows null value.
     * <p>
     * Note: This format changed in Coherence 3.0; previously the leading
     * integer value was the number of characters, and currently it is the
     * number of bytes as per the java.io package implementations.
     *
     * @param in  a DataInput stream to read from
     *
     * @return a String value (could be null)
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static String readSafeUTF(DataInput in)
            throws IOException
        {
        return readUTF(in);
        }

    /**
     * Write a variable-length encoded UTF packed String to the buffer.
     *
     * @param out  a BufferOutput to write to
     * @param s    a String value to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeSafeUTF(BufferOutput out, String s)
            throws IOException
        {
        out.writeSafeUTF(s);
        }

    /**
     * Write a variable-length encoded UTF packed String. The major difference
     * between this implementation and DataOutput stream is that this is not
     * limited to 64KB String values and allows null value.
     * <p>
     * Note: This format changed in Coherence 3.0; previously the leading
     * integer value was the number of characters, and currently it is the
     * number of bytes as per the java.io package implementations.
     *
     * @param out  a DataOutput stream to write to
     * @param s    a String value to write (could be null)
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static void writeSafeUTF(DataOutput out, String s)
            throws IOException
        {
        writeUTF(out, s);
        }

    /**
     * Read a String array.
     *
     * @param in  a DataInput stream to read from
     *
     * @return a String array value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static String[] readStringArray(DataInput in)
            throws IOException
        {
        String[] as;

        if (in instanceof PofInputStream)
            {
            Object[] ao = (Object[]) ((PofInputStream) in).readObject();
            if (ao == null)
                {
                as = null;
                }
            else
                {
                int co = ao.length;
                as = new String[co];
                System.arraycopy(ao, 0, as, 0, co);
                }
            }
        else
            {
            int c = readInt(in);

            validateLoadArray(String[].class, c, in);
            as = c < CHUNK_THRESHOLD >> 3
                    ? readStringArray(in, c)
                    : readLargeStringArray(in, c);
            }
        return as;
        }

    /**
     * Write a String array.
     *
     * @param out  a DataOutput stream to write to
     * @param as   a String array to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeStringArray(DataOutput out, String[] as)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(as);
            }
        else
            {
            int c  = as.length;
            writeInt(out, c);

            for (int i = 0; i < c; ++i)
                {
                writeSafeUTF(out, as[i]);
                }
            }
        }

    /**
     * Read a BigInteger from a DataInput stream.
     *
     * @param in  a DataInput stream to read from
     *
     * @return a BigInteger value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static BigInteger readBigInteger(DataInput in)
            throws IOException
        {
        BigInteger n;

        if (in instanceof PofInputStream)
            {
            n = (BigInteger) ((PofInputStream) in).readObject();
            }
        else
            {
            n = new BigInteger(readByteArray(in));
            }

        return n;
        }

    /**
     * Write a BigInteger to a DataOutput stream.
     *
     * @param out     a DataOutput stream to write to
     * @param bigint  a BigInteger value to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeBigInteger(DataOutput out, BigInteger bigint)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(bigint);
            }
        else
            {
            writeByteArray(out, bigint.toByteArray());
            }
        }

    /**
     * Read a BigDecimal from a DataInput stream.
     *
     * @param in  a DataInput stream to read from
     *
     * @return a BigDecimal value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static BigDecimal readBigDecimal(DataInput in)
            throws IOException
        {
        BigDecimal dec;

        if (in instanceof PofInputStream)
            {
            dec = (BigDecimal) ((PofInputStream) in).readObject();
            }
        else
            {
            dec = new BigDecimal(readBigInteger(in), readInt(in));
            }

        return dec;
        }

    /**
     * Write a BigDecimal to a DataOutput stream.
     *
     * @param out  a DataOutput stream to write to
     * @param dec  a BigDecimal value to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeBigDecimal(DataOutput out, BigDecimal dec)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(dec);
            }
        else
            {
            writeBigInteger(out, dec.unscaledValue());
            writeInt(out, dec.scale());
            }
        }

    /**
     * Read a Date from a DataInput stream.
     *
     * @param in  a DataInput stream to read from
     *
     * @return a Date value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static Date readDate(DataInput in)
            throws IOException
        {
        Date date;

        if (in instanceof PofInputStream)
            {
            PofInputStream inPof   = (PofInputStream) in;
            RawDate        dateRaw = inPof.getPofReader().readRawDate(inPof.nextIndex());
            date = dateRaw == null ? null : dateRaw.toSqlDate();
            }
        else
            {
            date = new Date(readLong(in));
            }

        return date;
        }

    /**
     * Write a Date to a DataOutput stream.
     *
     * @param out   a DataOutput stream to write to
     * @param date  a Date value to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeDate(DataOutput out, Date date)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(date);
            }
        else
            {
            writeLong(out, date.getTime());
            }
        }

    /**
     * Read a Time from a DataInput stream.
     *
     * @param in  a DataInput stream to read from
     *
     * @return a Time value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static Time readTime(DataInput in)
            throws IOException
        {
        Time time;

        if (in instanceof PofInputStream)
            {
            PofInputStream inPof   = (PofInputStream) in;
            RawTime        timeRaw = inPof.getPofReader().readRawTime(inPof.nextIndex());
            time = timeRaw == null ? null : timeRaw.toSqlTime();
            }
        else
            {
            time = new Time(readLong(in));
            }

        return time;
        }

    /**
     * Write a Time to a DataOutput stream.
     *
     * @param out   a DataOutput stream to write to
     * @param time  a Time value to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeTime(DataOutput out, Time time)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            PofOutputStream outPof = (PofOutputStream) out;
            outPof.getPofWriter().writeTimeWithZone(outPof.nextIndex(), time);
            }
        else
            {
            writeLong(out, time.getTime());
            }
        }

    /**
     * Read a Timestamp from a DataInput stream.
     *
     * @param in  a DataInput stream to read from
     *
     * @return a Timestamp value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static Timestamp readTimestamp(DataInput in)
            throws IOException
        {
        Timestamp dt;

        if (in instanceof PofInputStream)
            {
            PofInputStream inPof = (PofInputStream) in;
            RawDateTime    dtRaw = inPof.getPofReader().readRawDateTime(inPof.nextIndex());
            dt = dtRaw == null ? null : dtRaw.toSqlTimestamp();
            }
        else
            {
            dt = new Timestamp(readLong(in));
            dt.setNanos(readInt(in));
            }

        return dt;
        }

    /**
     * Write a Timestamp to a DataOutput stream.
     *
     * @param out  a DataOutput stream to write to
     * @param dt   a Timestamp value to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeTimestamp(DataOutput out, Timestamp dt)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            PofOutputStream outPof = (PofOutputStream) out;
            outPof.getPofWriter().writeDateTimeWithZone(outPof.nextIndex(), dt);
            }
        else
            {
            writeLong(out, dt.getTime());
            writeInt(out, dt.getNanos());
            }
        }

    /**
     * Read an array of float numbers from a DataInput stream.
     *
     * @param in  a DataInput stream to read from
     *
     * @return an array of floats
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static float[] readFloatArray(DataInput in)
            throws IOException
        {
        float[] afl;

        if (in instanceof PofInputStream)
            {
            afl = (float[]) ((PofInputStream) in).readObject();
            }
        else
            {
            int cfl = in.readInt();

            validateLoadArray(float[].class, cfl, in);
            afl = cfl < CHUNK_THRESHOLD >> 2
                    ? readFloatArray(in, cfl)
                    : readLargeFloatArray(in, cfl);
            }

        return afl;
        }

    /**
     * Write an array of float numbers to a DataOutput stream.
     *
     * @param out  a DataOutput stream to write to
     * @param afl  an array of floats to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeFloatArray(DataOutput out, float[] afl)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(afl);
            }
        else
            {
            int cfl = afl.length;
            out.writeInt(cfl);

            if (cfl > 0)
                {
                byte[] ab = new byte[cfl << 2];
                for (int i = 0, of = 0; i < cfl; i++)
                    {
                    int iValue = Float.floatToIntBits(afl[i]);

                    ab[of++] = (byte)(iValue >>> 24);
                    ab[of++] = (byte)(iValue >>> 16);
                    ab[of++] = (byte)(iValue >>>  8);
                    ab[of++] = (byte)(iValue);
                    }
                out.write(ab);
                }
            }
        }

    /**
     * Read an array of double numbers from a DataInput stream.
     *
     * @param in  a DataInput stream to read from
     *
     * @return an array of doubles
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static double[] readDoubleArray(DataInput in)
            throws IOException
        {
        double[] adbl;

        if (in instanceof PofInputStream)
            {
            adbl = (double[]) ((PofInputStream) in).readObject();
            }
        else
            {
            int c = in.readInt();

            validateLoadArray(float[].class, c, in);
            adbl = c <= 0 ? new double[0] :
                        c < CHUNK_THRESHOLD >> 3
                            ? readDoubleArray(in, c)
                            : readLargeDoubleArray(in, c);
            }

        return adbl;
        }

    /**
     * Write an array of double numbers to a DataOutput stream.
     *
     * @param out  a DataOutput stream to write to
     * @param ad   an array of doubles to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeDoubleArray(DataOutput out, double[] ad)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(ad);
            }
        else
            {
            int cd = ad.length;
            out.writeInt(cd);

            if (cd > 0)
                {
                byte[] ab = new byte[cd << 3];
                for (int i = 0, of = 0; i < cd; i++)
                    {
                    long lValue = Double.doubleToLongBits(ad[i]);
                    int  iUpper = (int) (lValue >>> 32);
                    int  iLower = (int) lValue;

                    ab[of++] = (byte)(iUpper >>> 24);
                    ab[of++] = (byte)(iUpper >>> 16);
                    ab[of++] = (byte)(iUpper >>> 8);
                    ab[of++] = (byte)(iUpper);
                    ab[of++] = (byte)(iLower >>> 24);
                    ab[of++] = (byte)(iLower >>> 16);
                    ab[of++] = (byte)(iLower >>>  8);
                    ab[of++] = (byte)(iLower);
                    }
                out.write(ab);
                }
            }
        }

    /**
     * Read map content from a DataInput stream and update the specified map.
     * <p>
     * This method reads entries from the stream and "puts" them into the
     * specified map one-by-one using the "put" method.
     *
     * @param in      a DataInput stream to read from
     * @param map     a map to add the entries into
     * @param loader  the ClassLoader to use
     *
     * @return the number of read and inserted entries
     *
     * @throws IOException  if an I/O exception occurs
     *
     * @see #readMap(DataInput, Map, int, ClassLoader)
     */
    public static int readMap(DataInput in, Map map, ClassLoader loader)
            throws IOException
        {
        int cEntries;

        if (in instanceof PofInputStream)
            {
            PofInputStream inPof = (PofInputStream) in;
            inPof.getPofReader().readMap(inPof.nextIndex(), map);
            cEntries = map.size();
            }
        else
            {
            cEntries = in.readInt();
            for (int i = 0; i < cEntries; ++i)
                {
                Object oKey = readObject(in, loader);
                Object oVal = readObject(in, loader);

                map.put(oKey, oVal);
                }
            }

        return cEntries;
        }

    /**
     * Read map content from a DataInput stream and update the specified map
     * using the "putAll" method.
     * <p>
     * While the method {@link #readMap(DataInput, Map, ClassLoader)} reads
     * entries from the stream and "puts" them into the specified map
     * one-by-one, this method collects up to the "block" number of entries
     * into a temporary map and then updates the passed in map using the
     * "putAll" method.
     *
     * @param in      a DataInput stream to read from
     * @param map     a map to add the entries into
     * @param cBlock  the maximum number of entries to read at once
     * @param loader  the ClassLoader to use
     *
     * @return the number of read and inserted entries
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static int readMap(DataInput in, Map map, int cBlock, ClassLoader loader)
            throws IOException
        {
        int cEntries;

        if (in instanceof PofInputStream)
            {
            PofInputStream inPof = (PofInputStream) in;
            Map mapTemp = inPof.getPofReader().readMap(inPof.nextIndex(), (Map) null);
            if (mapTemp == null)
                {
                cEntries = 0;
                }
            else
                {
                cEntries = mapTemp.size();
                map.putAll(mapTemp);
                }
            }
        else
            {
            if (cBlock <= 0)
                {
                throw new IllegalArgumentException("Illegal block size: " + cBlock);
                }

            cEntries = in.readInt();
            Map mapTmp   = new HashMap(Math.min(cEntries, cBlock));
            int cTmp     = 0;
            for (int i = 0; i < cEntries; ++i)
                {
                Object oKey = readObject(in, loader);
                Object oVal = readObject(in, loader);

                mapTmp.put(oKey, oVal);
                if (++cTmp == cBlock)
                    {
                    map.putAll(mapTmp);
                    mapTmp.clear();
                    cTmp = 0;
                    }
                }

            if (cTmp > 0)
                {
                map.putAll(mapTmp);
                }
            }

        return cEntries;
        }

    /**
     * Write map content to a DataOutput stream.
     *
     * @param out  a DataOutput stream to write to
     * @param map  the map to write
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static void writeMap(DataOutput out, Map map)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(map);
            }
        else
            {
            int cEntries = map.size();
            int cCheck   = 0;

            out.writeInt(cEntries);
            try
                {
                for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); ++cCheck)
                    {
                    Map.Entry entry = (Map.Entry) iter.next();

                    writeObject(out, entry.getKey());
                    writeObject(out, entry.getValue());
                    }
                }
            catch (ConcurrentModificationException e) {}
            catch (NoSuchElementException e) {}

            if (cCheck != cEntries)
                {
                throw new IOException("Expected to write " + cEntries
                    + " objects but actually wrote " + cCheck);
                }
            }
        }

    /**
     * Read collection content from a DataInput stream and update the
     * specified collection.
     * <p>
     * This method reads elements from the stream and adds them into the
     * specified collection one-by-one using the "add" method.
     *
     * @param in          a DataInput stream to read from
     * @param collection  a collection to add the elements into
     * @param loader      the ClassLoader to use
     *
     * @return the number of read and inserted elements
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static int readCollection(DataInput in, Collection collection, ClassLoader loader)
            throws IOException
        {
        int cItems;

        if (in instanceof PofInputStream)
            {
            PofInputStream inPof = (PofInputStream) in;
            inPof.getPofReader().readCollection(inPof.nextIndex(), collection);
            cItems = collection.size();
            }
        else
            {
            cItems = in.readInt();
            for (int i = 0; i < cItems; ++i)
                {
                collection.add(readObject(in, loader));
                }
            }

        return cItems;
        }

    /**
     * Write collection content to a DataOutput stream.
     *
     * @param out         a DataOutput stream to write to
     * @param collection  the collection to write
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static void writeCollection(DataOutput out, Collection collection)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(collection);
            }
        else
            {
            int cItems = collection.size();
            int cCheck = 0;

            out.writeInt(cItems);
            try
                {
                for (Iterator iter = collection.iterator(); iter.hasNext(); ++cCheck)
                    {
                    writeObject(out, iter.next());
                    }
                }
            catch (ConcurrentModificationException e) {}
            catch (NoSuchElementException e) {}

            if (cCheck != cItems)
                {
                throw new IOException("Expected to write " + cItems
                    + " objects but actually wrote " + cCheck);
                }
            }
        }

    /**
     * Read an XmlSerializable object from a DataInput stream.
     *
     * @param in  a DataInput stream to read from
     *
     * @return an XmlSerializable value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static XmlSerializable readXmlSerializable(DataInput in)
            throws IOException
        {
        return readXmlSerializable(in, null);
        }

    /**
     * Read an XmlSerializable object from a DataInput stream.
     *
     * @param in      a DataInput stream to read from
     * @param loader  the ClassLoader to use
     *
     * @return an XmlSerializable value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static XmlSerializable readXmlSerializable(DataInput in, ClassLoader loader)
            throws IOException
        {
        XmlSerializable value;

        if (in instanceof PofInputStream)
            {
            value = (XmlSerializable) ((PofInputStream) in).readObject();
            }
        else
            {
            // instantiate the object
            String sClass = readUTF(in);
            try
                {
                value = (XmlSerializable) loadClass(sClass, loader, null).newInstance();
                }
            catch (Exception e)
                {
                throw new IOException(
                    "Class initialization failed: " + e +
                    "\n" + getStackTrace(e) +
                    "\nClass: " + sClass +
                    "\nClassLoader: " + loader +
                    "\nContextClassLoader: " + getContextClassLoader());
                }

            String     sXml = readUTF(in);

            // Bug 32341371 - Do not validate the XML to prevent XXE (XML eXternal Entity) injection
            XmlElement xml  = new SimpleParser(/* fValidate */ false).parseXml(sXml);
            value.fromXml(xml);
            }

        return value;
        }

    /**
     * Write an XmlSerializable object to a DataOutput stream.
     *
     * @param out  a DataOutput stream to write to
     * @param o    an XmlSerializable value to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeXmlSerializable(DataOutput out, XmlSerializable o)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(o);
            }
        else
            {
            StringWriter writerRaw = new StringWriter();
            PrintWriter  writerPrn = new PrintWriter(writerRaw);
            o.toXml().writeXml(writerPrn, false);
            writerPrn.close();

            writeUTF(out, o.getClass().getName());
            writeUTF(out, writerRaw.toString());
            }
        }

    /**
     * Read an ExternalizableLite object from a DataInput stream.
     *
     * @param in  a DataInput stream to read from
     *
     * @return an ExternalizableLite value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static ExternalizableLite readExternalizableLite(DataInput in)
            throws IOException
        {
        return readExternalizableLite(in, null);
        }

    /**
     * Read an ExternalizableLite object from a DataInput stream.
     * <p>
     * If the class of the object in the DataInput stream is an {@link ExternalizableLite} class
     * annotated with {@link ExternalizableType}, use the specified serializer's
     * {@link ExternalizableLiteSerializer#deserialize(DataInput) deserialize method} to
     * deserialize the object from the DataInput stream.
     *
     * @param in      a DataInput stream to read from
     * @param loader  the ClassLoader to use
     *
     * @return an ExternalizableLite value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static ExternalizableLite readExternalizableLite(DataInput in, ClassLoader loader)
            throws IOException
        {
        ExternalizableLite value = null;

        if (in instanceof PofInputStream)
            {
            value = (ExternalizableLite) ((PofInputStream) in).readObject();
            }
        else
            {
            Class<?>           clz  = null;
            ExternalizableType type = null;

            // instantiate the object
            String sClass = readUTF(in);

            WrapperDataInputStream inWrapper = in instanceof WrapperDataInputStream
                    ? (WrapperDataInputStream) in : null;
            try
                {
                clz = loadClass(sClass, loader,
                        inWrapper == null ? null : inWrapper.getClassLoader());

                validateLoadClass(clz, in);

                // check for class annotation for ExternalizableLiteSerializer
                type = clz.getAnnotation(ExternalizableType.class);
                if (type == null || type.serializer() == null)
                    {
                    value = (ExternalizableLite) clz.newInstance();
                    }
                else
                    {
                    // instance allocated by serializer deserialize
                    value = null;
                    }
                }
            catch (InstantiationException e)
                {
                throw new IOException(
                    "Unable to instantiate an instance of class '" + sClass +
                    "'; this is most likely due to a missing public " +
                    "no-args constructor: " + e +
                    "\n" + getStackTrace(e) +
                    "\nClass: " + sClass +
                    "\nClassLoader: " + loader +
                    "\nContextClassLoader: " + getContextClassLoader());
                }
            catch (Exception e)
                {
                throw new IOException(
                    "Class initialization failed: " + e +
                    "\n" + getStackTrace(e) +
                    "\nClass: " + sClass +
                    "\nClassLoader: " + loader +
                    "\nContextClassLoader: " + getContextClassLoader(), e);
                }

            if (loader != null)
                {
                if (inWrapper == null)
                    {
                    in = new WrapperDataInputStream(in, loader);
                    }
                else if (loader != inWrapper.getClassLoader())
                    {
                    inWrapper.setClassLoader(loader);
                    }
                }

            if (value != null)
                {
                value.readExternal(in);
                }
            else
                {
                try
                    {
                    value = (ExternalizableLite) type.serializer().newInstance().deserialize(in);
                    if (value != null && value.getClass() != clz)
                        {
                        // deserialize returned an instance that is not same as previously validated load class.
                        // validate the class of the value returned by deserializer against the ObjectInputFilter.
                        // same JEP 290 logic as applied by method #safeRealize(...).
                        validateLoadClass(value.getClass(), in);
                        }
                    }
                catch (Exception e)
                    {
                    throw new IOException(
                            "Class initialization failed: " + e +
                            "\n" + getStackTrace(e) +
                            "\nClass: " + sClass +
                            "\nExternalizableLiteSerializer class: " + type.serializer().getName() +
                            "\nClassLoader: " + loader +
                            "\nContextClassLoader: " + getContextClassLoader(), e);
                    }
                }
            if (value instanceof SerializerAware)
                {
                ((SerializerAware) value).setContextSerializer(ensureSerializer(loader));
                }
            }

        return value;
        }

    /**
     * Validate that the given class is permitted to be deserialized by
     * consulting any associated ObjectInputFilters.
     *
     * @param clz  the class to be validated
     * @param in   input context to use to validate if class is allowed to be loaded
     *
     * @throws InvalidClassException if ObjectInputFilter associated with
     *         <code>in</code> rejects class <code>clz</code>
     */
    protected static void validateLoadClass(Class clz, DataInput in)
            throws InvalidClassException
        {
        if (!checkObjectInputFilter(clz, in))
            {
            throw new InvalidClassException("Deserialization of class " + clz.getName() + " was rejected");
            }
        }

    /**
     * Validate that the given class and array length is permitted to be deserialized by
     * consulting any associated ObjectInputFilters.
     *
     * @param clz      the array type to be validated
     * @param cLength  the array length to be validated
     * @param in       input context to use to validate if class is allowed to be loaded
     *
     * @throws InvalidClassException if ObjectInputFilter associated with
     *         <code>in</code> rejects array length
     */
    public static void validateLoadArray(Class clz, int cLength, DataInput in)
            throws InvalidClassException
        {
        if (!checkObjectInputFilter(clz, cLength, in))
            {
            throw new InvalidClassException("Deserialization of class " + clz.getName() + " with array length " + cLength + " was rejected");
            }
        }

    /**
     * Write an ExternalizableLite object to a DataOutput stream.
     *
     * <p>
     * If the class of parameter <code>o</code> is annotated with {@link ExternalizableType},
     * use the specified serializer's
      {@link ExternalizableLiteSerializer#serialize(DataOutput, Object) serialize method} to
     * write <code>o</code> into the DataOutput stream.
     *
     * @param out  a DataOutput stream to write to
     * @param o    an ExternalizableLite value to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeExternalizableLite(DataOutput out, ExternalizableLite o)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(o);
            }
        else
            {
            Class              clz  = o.getClass();
            ExternalizableType type = (ExternalizableType) clz.getAnnotation(ExternalizableType.class);

            writeUTF(out, clz.getName());
            if (type == null || type.serializer() == null)
                {
                o.writeExternal(out);
                }
            else
                {
                try
                    {
                    type.serializer().newInstance().serialize(out, o);
                    }
                catch (InstantiationException e)
                    {
                    throw new IOException(e);
                    }
                catch (IllegalAccessException e)
                    {
                    throw new IOException(e);
                    }
                }
            }
        }

    /**
     * Read an XmlBean object from a DataInput stream.
     *
     * @param in      a DataInput stream to read from
     * @param loader  the ClassLoader to use
     *
     * @return an XmlBean value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static XmlBean readXmlBean(DataInput in, ClassLoader loader)
            throws IOException
        {
        XmlBean bean;

        if (in instanceof PofInputStream)
            {
            bean = (XmlBean) ((PofInputStream) in).readObject();
            }
        else if (USE_XMLBEAN_CLASS_CACHE)
            {
            int nBeanId = readInt(in);
            if (nBeanId < 0)
                {
                // this XmlBean is not serialization-optimized
                bean = (XmlBean) readExternalizableLite(in, loader);
                }
            else
                {
                try
                    {
                    Class clz = XMLBEAN_CLASS_CACHE.getClass(nBeanId, loader);
                    validateLoadClass(clz, in);
                    bean = (XmlBean) clz.newInstance();
                    }
                catch (Exception e)
                    {
                    throw new IOException(
                        "Class instantiation failed: " + e +
                        "\n" + getStackTrace(e) +
                        "\nXmlBean ID: " + nBeanId +
                        "\nClassLoader: " + loader +
                        "\nContextClassLoader: " + getContextClassLoader());
                    }
                bean.readExternal(in);
                }
            }
        else
            {
            bean = (XmlBean) readExternalizableLite(in, loader);
            }

        return bean;
        }

    /**
     * Write an XmlBean object to a DataOutput stream.
     *
     * @param out   a DataOutput stream to write to
     * @param bean  an XmlBean value to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value if passed
     */
    public static void writeXmlBean(DataOutput out, XmlBean bean)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(bean);
            }
        else if (USE_XMLBEAN_CLASS_CACHE)
            {
            int nBeanId = bean.getBeanInfo().getBeanId();
            writeInt(out, nBeanId);
            if (nBeanId < 0)
                {
                // this XmlBean is not serialization-optimized
                writeExternalizableLite(out, bean);
                }
            else
                {
                bean.writeExternal(out);
                }
            }
        else
            {
            writeExternalizableLite(out, bean);
            }
        }

    /**
     * Read an object from a DataInput stream.
     *
     * @param in  a DataInput stream to read from
     *
     * @return a value object
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static Object readSerializable(DataInput in)
            throws IOException
        {
        return readSerializable(in, null);
        }

    /**
     * Read an object from a DataInput stream.
     *
     * @param in      a DataInput stream to read from
     * @param loader  the ClassLoader to use
     *
     * @return a value object
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static Object readSerializable(DataInput in, ClassLoader loader)
            throws IOException
        {
        Object o;

        if (in instanceof PofInputStream)
            {
            o = ((PofInputStream) in).readObject();
            }
        else
            {
            // read the object; theoretically speaking only the following
            // exceptions are thrown:
            //
            //   ClassNotFoundException    Class of a serialized object
            //                             cannot be found
            //   InvalidClassException     Something is wrong with a class
            //                             used by serialization
            //   StreamCorruptedException  Control information in the stream
            //                             is inconsistent
            //   OptionalDataException     Primitive data was found in the
            //                             stream instead of objects
            //   IOException               Any of the usual Input/Output
            //                             related exceptions
            //
            // However, ClassCastException and IndexOutOfBoundsException
            // could be thrown as well, so to make the processing logic more
            // consistent we convert them all into a generic IOException.
            // (After all, it is the "read from stream" that fails.)
            ObjectInput streamObj = getObjectInput(in, loader);
            try
                {
                s_tloInEHDeserialize.set(true); // mark that we are in EH managed deserialization
                o = streamObj.readObject();
                if (o instanceof SerializerAware)
                    {
                    ((SerializerAware) o).setContextSerializer(ensureSerializer(loader));
                    }
                }
            catch (IOException e)
                {
                throw e;
                }
            catch (Exception e)
                {
                throw new IOException(
                    "readObject failed: " + e +
                    "\n" + getStackTrace(e) +
                    "\nClassLoader: " + loader);
                }
            finally
                {
                s_tloInEHDeserialize.remove();
                }
            }

        return o;
        }

    /**
     * Write an object to a DataOutput stream.
     *
     * @param out  a DataOutput stream to write to
     * @param o    a value object to write
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static void writeSerializable(DataOutput out, Object o)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(o);
            }
        else
            {
            ObjectOutput streamObj = getObjectOutput(out);
            streamObj.writeObject(o);
            streamObj.close();
            }
        }

    /**
     * Read an Object from the passed DataInput object.
     *
     * @param <T>  the class of the object read
     * @param in   the DataInput stream to read an object from
     *
     * @return the object read from the DataInput; may be null
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static <T> T readObject(DataInput in)
            throws IOException
        {
        return readObject(in, null);
        }

    /**
     * Read an Object from the passed DataInput object.
     *
     * @param <T>  the class of the object read
     * @param in      the DataInput stream to read an object from
     * @param loader  the ClassLoader to use
     *
     * @return the object read from the DataInput; may be null
     *
     * @throws IOException  if an I/O exception occurs
     */
    @SuppressWarnings("unchecked")
    public static <T> T readObject(DataInput in, ClassLoader loader)
            throws IOException
        {
        if (in instanceof PofInputStream)
            {
            return (T) ((PofInputStream) in).readObject();
            }

        Object o = readObjectInternal(in, in.readUnsignedByte(), loader);
        return (T) safeRealize(o, ensureSerializer(loader), in);
        }

    /**
     * {@link #realize(Object, Serializer) Realize} deserialized instance <code>o</code> for possible replacement.
     * If replacement occurs, the replacement's class is validated against {@link DataInput} <code>in</code>'s ObjectInputFilter.
     *
     * @param o           deserialized instance
     * @param serializer  the serializer
     * @param in          DataInput context
     * @param <T>         replacement type
     *
     * @return            either deserialized instance <code>o</code> or its {@link SerializationSupport#readResolve} replacement.
     *
     * @throws IOException if ObjectInputFilter associated with <code>in</code> rejects a replacements class
     */
    private static <T> T safeRealize(Object o, Serializer serializer, DataInput in)
            throws IOException
        {
        T oReplace = (T) realize(o, serializer);

        if (o != oReplace && oReplace != null && o.getClass() != oReplace.getClass())
            {
            // validate the replacement against the ObjectInputFilter
            if (oReplace.getClass().isArray())
                {
                validateLoadArray(oReplace.getClass(),
                                                       Array.getLength(oReplace), in);
                }
            else
                {
                validateLoadClass(oReplace.getClass(), in);
                }
            }
        return oReplace;
        }

    /**
     * Read an object of a known type from the specified DataInput.
     */
    private static Object readObjectInternal(DataInput in, int nType, ClassLoader loader)
            throws IOException
        {
        switch (nType)
            {
            default:
                throw new StreamCorruptedException("invalid type: " + nType);

            case FMT_UNKNOWN:
                // while exactly the same as FMT_OBJ_SER, we want to have a
                // distinct stack trace in a case of failure
                return readSerializable(in, loader);

            case FMT_NULL:
                return null;

            case FMT_INT:
                return readInt(in);

            case FMT_LONG:
                return readLong(in);

            case FMT_STRING:
                return readUTF(in);

            case FMT_DOUBLE:
                return in.readDouble();

            case FMT_INTEGER:
                return readBigInteger(in);

            case FMT_DECIMAL:
                return readBigDecimal(in);

            case FMT_BINARY:
                {
                Binary bin = new Binary();
                bin.readExternal(in);
                return bin;
                }

            case FMT_B_ARRAY:
                return readByteArray(in);

            case FMT_XML_SER:
                return readXmlSerializable(in, loader);

            case FMT_OBJ_EXT:
                return readExternalizableLite(in, loader);

            case FMT_OBJ_SER:
                return readSerializable(in, loader);

            case FMT_OPT:
                return in.readBoolean() ? Optional.of(readObject(in, loader)) : Optional.empty();

            case FMT_OPT_INT:
                return in.readBoolean() ? OptionalInt.of(readInt(in)) : OptionalInt.empty();

            case FMT_OPT_LONG:
                return in.readBoolean() ? OptionalLong.of(readLong(in)) : OptionalLong.empty();

            case FMT_OPT_DOUBLE:
                return in.readBoolean() ? OptionalDouble.of(in.readDouble()) : OptionalDouble.empty();

            case FMT_XML_BEAN:
                return readXmlBean(in, loader);

            case FMT_FLOAT:
                return in.readFloat();

            case FMT_SHORT:
                return in.readShort();

            case FMT_BYTE:
                return in.readByte();

            case FMT_BOOLEAN:
                return in.readBoolean() ? Boolean.TRUE : Boolean.FALSE;
            }
        }

    /**
     * Write the specified data to the passed DataOutput object.
     *
     * @param out  the DataOutput stream to write to
     * @param o    the data to write to the DataOutput; may be null
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static void writeObject(DataOutput out, Object o)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(o);
            }
        else
            {
            o = replace(o);

            int nType = getStreamFormat(o);
            out.writeByte(nType);

            switch (nType)
                {
                case FMT_UNKNOWN:
                    // while the same as FMT_OBJ_SER, we want to have a distinct
                    // stack trace in a case of failure
                    writeSerializable(out, o);
                    break;

                case FMT_NULL:
                    // no data to write
                    break;

                case FMT_INT:
                    writeInt(out, ((Integer) o).intValue());
                    break;

                case FMT_LONG:
                    writeLong(out, ((Long) o).longValue());
                    break;

                case FMT_STRING:
                    writeUTF(out, (String) o);
                    break;

                case FMT_DOUBLE:
                    out.writeDouble(((Double) o).doubleValue());
                    break;

                case FMT_INTEGER:
                    writeBigInteger(out, (BigInteger) o);
                    break;

                case FMT_DECIMAL:
                    writeBigDecimal(out, (BigDecimal) o);
                    break;

                case FMT_BINARY:
                    Binary.writeExternal(out, (ReadBuffer) o);
                    break;

                case FMT_B_ARRAY:
                    writeByteArray(out, (byte[]) o);
                    break;

                case FMT_XML_SER:
                    writeXmlSerializable(out, (XmlSerializable) o);
                    break;

                case FMT_OBJ_EXT:
                    writeExternalizableLite(out, (ExternalizableLite) o);
                    break;

                case FMT_OBJ_SER:
                    writeSerializable(out, o);
                    break;

                case FMT_XML_BEAN:
                    writeXmlBean(out, (XmlBean) o);
                    break;

                case FMT_OPT:
                    {
                    Optional opt      = (Optional) o;
                    boolean  fPresent = opt.isPresent();
                    out.writeBoolean(fPresent);
                    if (fPresent)
                        {
                        writeObject(out, opt.get());
                        }
                    }
                    break;

                case FMT_OPT_INT:
                    {
                    OptionalInt opt      = (OptionalInt) o;
                    boolean     fPresent = opt.isPresent();
                    out.writeBoolean(fPresent);
                    if (fPresent)
                        {
                        writeInt(out, opt.getAsInt());
                        }
                    }
                    break;

                case FMT_OPT_LONG:
                    {
                    OptionalLong opt      = (OptionalLong) o;
                    boolean      fPresent = opt.isPresent();
                    out.writeBoolean(fPresent);
                    if (fPresent)
                        {
                        writeLong(out, opt.getAsLong());
                        }
                    }
                    break;

                case FMT_OPT_DOUBLE:
                    {
                    OptionalDouble opt      = (OptionalDouble) o;
                    boolean        fPresent = opt.isPresent();
                    out.writeBoolean(fPresent);
                    if (fPresent)
                        {
                        out.writeDouble(opt.getAsDouble());
                        }
                    }
                    break;

                case FMT_FLOAT:
                    out.writeFloat(((Float) o).floatValue());
                    break;

                case FMT_SHORT:
                    out.writeShort(((Short) o).shortValue());
                    break;

                case FMT_BYTE:
                    out.writeByte(((Byte) o).byteValue());
                    break;

                case FMT_BOOLEAN:
                    out.writeBoolean(((Boolean) o).booleanValue());
                    break;

                default:
                    throw azzert();
                }
            }
        }

    /**
     * Serialize the passed object into a buffer created for that purpose.
     *
     * @param serializer  the serializer to use
     * @param o           the object to write
     * @param fBinary     pass true to prefer a buffer type that is optimized for
     *                    producing a Binary result
     *
     * @return the WriteBuffer that was created to hold the serialized form of
     *         the object
     *
     * @throws IOException  if an I/O exception occurs
     */
    private static WriteBuffer serializeInternal(Serializer serializer, Object o, boolean fBinary)
            throws IOException
        {
        return serializeInternal(serializer, o, fBinary, null);
        }

    /**
     * Serialize the passed object into a specified buffer.
     *
     * @param serializer  the serializer to use
     * @param o           the object to write
     * @param fBinary     pass true to prefer a buffer type that is optimized for
     *                    producing a Binary result
     * @param buf         the reusable WriteBuffer to serialize into
     *
     * @return the reusable WriteBuffer that was passed as a {@code buf} argument
     *         that the object was serialized into
     *
     * @throws IOException  if an I/O exception occurs
     */
    private static WriteBuffer serializeInternal(Serializer serializer, Object o, boolean fBinary, WriteBuffer buf)
            throws IOException
        {
        // estimate the size of the buffer
        boolean fDeco = false;
        int     nDeco = 0;
        int     cb    = 1;
        boolean fUser = false;
        Stats   stats = null;

        // start by unwrapping any int-decorated object
        if (o instanceof IntDecoratedObject)
            {
            // note: does not support recursive IDO
            IntDecoratedObject ido = (IntDecoratedObject) o;
            fDeco = true;
            nDeco = ido.getDecoration();
            cb   += 5;
            o     = ido.getValue();
            }

        o = replace(o);

        int nType = serializer instanceof DefaultSerializer ?
            getStreamFormat(o) : FMT_EXT;

        switch (nType)
            {
            case FMT_NULL:
                break;

            case FMT_BOOLEAN:
            case FMT_BYTE:
                cb += 1;
                break;

            case FMT_SHORT:
                cb += 2;
                break;

            case FMT_INT:
                cb += BufferOutput.MAX_PACKED_INT_SIZE;
                break;

            case FMT_LONG:
                cb += BufferOutput.MAX_PACKED_LONG_SIZE;
                break;

            case FMT_INTEGER:
                cb += ((BigInteger) o).bitLength() / 8 + 1;
                break;

            case FMT_FLOAT:
                cb += 4;
                break;

            case FMT_DOUBLE:
                cb += 8;
                break;

            case FMT_DECIMAL:
                cb += ((BigDecimal) o).unscaledValue().bitLength() / 8 + 2;
                break;

            case FMT_BINARY:
                cb += ((ReadBuffer) o).length() + 4;
                break;

            case FMT_B_ARRAY:
                cb += ((byte[]) o).length + 5;
                break;

            case FMT_STRING:
                // optimize assuming single byte characters
                cb += ((String) o).length() + 5;
                break;

            case FMT_EXT:
                azzert(serializer != null);
                // break through
            case FMT_UNKNOWN:
            case FMT_XML_SER:
            case FMT_OBJ_EXT:
            case FMT_OBJ_SER:
            case FMT_OPT:
            case FMT_OPT_INT:
            case FMT_OPT_LONG:
            case FMT_OPT_DOUBLE:
            case FMT_XML_BEAN:
                fUser = true;
                stats = findStats(o);
                if (stats == null)
                    {
                    cb = 64;
                    }
                break;

            default:
                throw azzert();
            }

        if (buf == null)
            {
            // presize a write buffer as efficiently as possible
            buf = stats == null
                      ? fBinary
                        ? new BinaryWriteBuffer(cb)
                        : new ByteArrayWriteBuffer(cb)
                      : stats.instantiateBuffer(fBinary);
            }

        // write out the object
        BufferOutput out = buf.getBufferOutput();
        if (fDeco)
            {
            out.writeByte(FMT_IDO);
            out.writePackedInt(nDeco);
            }

        out.writeByte(nType);

        // optimize for the most common code path
        if (nType == FMT_EXT)
            {
            serializer.serialize(out, o);
            }
        else
            {
            writeObjectInternal(out, nType, o);
            }

        // update stats for values of user types
        if (fUser)
            {
            updateStats(o, stats, buf.length());
            }

        return buf;
        }

    /**
     * Write an object of a known type to the specified BufferOutput. This method
     * is used only by the DefaultSerializer.
     * <p>
     * This method is very similar to writeObject, except that it is only used
     * internally to optimize a common code path and uses BufferOutput instead
     * of DataOutput.
     */
    private static void writeObjectInternal(BufferOutput out, int nType, Object o)
            throws IOException
        {
        switch (nType)
            {
            case FMT_NULL:
                // no data to write
                break;

            case FMT_INT:
                out.writePackedInt((Integer) o);
                break;

            case FMT_LONG:
                out.writePackedLong((Long) o);
                break;

            case FMT_STRING:
                out.writeSafeUTF((String) o);
                break;

            case FMT_DOUBLE:
                out.writeDouble((Double) o);
                break;

            case FMT_INTEGER:
                writeBigInteger(out, (BigInteger) o);
                break;

            case FMT_DECIMAL:
                writeBigDecimal(out, (BigDecimal) o);
                break;

            case FMT_BINARY:
                Binary.writeExternal(out, (ReadBuffer) o);
                break;

            case FMT_B_ARRAY:
                writeByteArray(out, (byte[]) o);
                break;

            case FMT_XML_SER:
                writeXmlSerializable(out, (XmlSerializable) o);
                break;

            case FMT_OBJ_EXT:
                writeExternalizableLite(out, (ExternalizableLite) o);
                break;

            case FMT_OBJ_SER:
                writeSerializable(out, o);
                break;

            case FMT_OPT:
                {
                Optional opt      = (Optional) o;
                boolean  fPresent = opt.isPresent();
                out.writeBoolean(fPresent);
                if (fPresent)
                    {
                    writeObject(out, opt.get());
                    }
                }
                break;

            case FMT_OPT_INT:
                {
                OptionalInt opt      = (OptionalInt) o;
                boolean     fPresent = opt.isPresent();
                out.writeBoolean(fPresent);
                if (fPresent)
                    {
                    writeInt(out, opt.getAsInt());
                    }
                }
                break;

            case FMT_OPT_LONG:
                {
                OptionalLong opt      = (OptionalLong) o;
                boolean      fPresent = opt.isPresent();
                out.writeBoolean(fPresent);
                if (fPresent)
                    {
                    writeLong(out, opt.getAsLong());
                    }
                }
                break;

            case FMT_OPT_DOUBLE:
                {
                OptionalDouble opt      = (OptionalDouble) o;
                boolean        fPresent = opt.isPresent();
                out.writeBoolean(fPresent);
                if (fPresent)
                    {
                    out.writeDouble(opt.getAsDouble());
                    }
                }
                break;

            case FMT_XML_BEAN:
                writeXmlBean(out, (XmlBean) o);
                break;

            case FMT_FLOAT:
                out.writeFloat((Float) o);
                break;

            case FMT_SHORT:
                out.writeShort((Short) o);
                break;

            case FMT_BYTE:
                out.writeByte((Byte) o);
                break;

            case FMT_BOOLEAN:
                out.writeBoolean((Boolean) o);
                break;

            case FMT_UNKNOWN:
                // while exactly the same as FMT_OBJ_SER, we want to have a
                // distinct stack trace in a case of failure
                writeSerializable(out, o);
                break;

            default:
                throw azzert();
            }
        }

    /**
     * Read an Object from the passed ReadBuffer using the specified Serializer.
     *
     * @param serializer  the Serializer to use
     * @param buf         the ReadBuffer object to read an object from
     *
     * @return the object read from the ReadBuffer; may be null
     *
     * @throws IOException  if an I/O exception occurs
     */
    private static <T> T deserializeInternal(Serializer serializer, ReadBuffer buf, Remote.Function<BufferInput, BufferInput> supplierBufferIn, Class<T> clazz)
            throws IOException
        {
        BufferInput in = buf.getBufferInput();

        int nType = in.readUnsignedByte();
        switch (nType)
            {
            case FMT_IDO:
                // read is not symmetrical to the write;
                // it returns the decorated value itself!
                readInt(in); // skip the decoration
                nType = in.readUnsignedByte();
                break;

            case FMT_BIN_DECO:
            case FMT_BIN_EXT_DECO:
                long nMask = nType == FMT_BIN_DECO ? in.readByte() : in.readPackedLong();
                if ((nMask & (1L << DECO_VALUE)) == 0L)
                    {
                    throw new EOFException("Decorated value is missing a value");
                    }

                // get a BufferInput that corresponds just to the portion of
                // the value within the decorated binary
                int cb = in.readPackedInt();
                in = buf.getReadBuffer(in.getOffset(), cb).getBufferInput();
                nType = in.readUnsignedByte();
                break;
            }

        if (supplierBufferIn != null)
            {
            in = supplierBufferIn.apply(in);
            }

        Object o = nType == FMT_EXT
                   ? serializer.deserialize(in, clazz)
                   : readObjectInternal(in, nType,
                                        ((ClassLoaderAware) serializer).getContextClassLoader());

        return safeRealize(o, serializer, in);
        }


    // ----- inner class: Stats ---------------------------------------------

    /**
     * Verify that the requested buffer size is smaller than the configured
     * maximum.
     *
     * @param cb size of the buffer being requested
     *
     * @throws BufferOverflowException if cb &gt; {@link #MAX_BUFFER }
     */
    public static void validateBufferSize(int cb)
        {
        // ensure that we don't allocate more than the configured maximum
        int cbMax = MAX_BUFFER;
        if (cbMax > 0 && cb > cbMax)
            {
            BufferOverflowException e = new BufferOverflowException();
            e.initCause(new IOException("serialization exceeded maximum size ("
                    + cb + " out of " + cbMax + ")"));
            throw e;
            }
        }

    /**
     * If statistics are being maintained for the class of the specified
     * Object value, then find and return those stats.
     *
     * @param o  the value to search for a Stats object for
     *
     * @return the Stats object for the specified Object value, or null
     */
    private static Stats findStats(Object o)
        {
        return s_astats[calculateStatsId(o)];
        }

    /**
     * If statistics are being maintained for the class of the specified
     * Object value, then find and return those stats.
     *
     * @param o      the object that has been written
     * @param stats  the statistics that track the serialized sizes of objects
     * @param cb     the size in bytes of the object as it was written
     */
    private static void updateStats(Object o, Stats stats, int cb)
        {
        if (stats == null)
            {
            s_astats[calculateStatsId(o)] = stats = new Stats();
            }
        stats.update(cb);
        }

    /**
     * Calculate a somewhat unique ID for the type of the passed Object.
     *
     * @param o  a user type value
     *
     * @return an ID that is hopefully unique across the set of user type
     *         classes in use within this JVM at this general point in time
     */
    private static int calculateStatsId(Object o)
        {
        if (o == null)
            {
            return 0;
            }
        int n = o.getClass().hashCode();
        return ((n >>> 1) + (n & 0x01)) % s_astats.length;
        }

    /**
     * Serialization statistics for a given user type.
     * Do not document.
     */
    protected static class Stats
            implements MultiBufferWriteBuffer.WriteBufferPool
        {
        // ----- statistics-related -----------------------------------------

        /**
         * Update the serialization statistics with the size (in bytes) of a
         * newly serialized object.
         *
         * @param cb  the number of bytes used to serialize
         */
        void update(int cb)
            {
            int  cbMax   = (int) (m_lStats >>> 32);
            long lAccum  = m_lAccum;
            int  cItems  = (int) (lAccum >>> 48);
            long cbTotal = lAccum & 0xFFFFFFFFFFFFL;

            if (cItems > 0)
                {
                boolean fResetStats = false;
                int     cbOldAvg    = (int) cbTotal / cItems;
                long    ldtNow      = 0;

                if (Math.abs(cbOldAvg - cb) > (cb / 2))
                    {
                    // reset the stats because cb differs by more than
                    // 50% from the current average
                    ldtNow = getSafeTimeMillis();
                    fResetStats = true;
                    }
                else if ((cItems & 0x3FF) == 0)
                    {
                    ldtNow = getSafeTimeMillis();
                    if (ldtNow > m_ldtCreated + EXPIRY_MILLIS ||  // stats expiry
                            (cItems & 0xFFFF) == 0)               // cItems overflow
                        {
                        // reset the stats periodically
                        fResetStats = true;
                        }
                    }

                if (fResetStats)
                    {
                    cbMax        = 0;
                    lAccum       = 0L;
                    cItems       = 0;
                    m_ldtCreated = ldtNow;
                    }
                }

            // accumulate the total bytes (uses lowest 48 out of 64 bits)
            cbTotal = (lAccum + cb) & 0xFFFFFFFFFFFFL;

            // recalculate the average
            int cbAvg = (int) (cbTotal / ++cItems);

            // check for a new max size
            if (cb > cbMax)
                {
                cbMax = cb;
                }

            // the item count and total bytes are stored in a "volatile long"
            // so that they are accessed (and modified) atomically
            m_lAccum = ((long) cItems << 48) | cbTotal;

            // the average and max are stored in a "volatile long" so that
            // they are subsequently accessed atomically
            m_lStats = ((long) cbMax << 32) | cbAvg;
            }

        /**
         * Instantiate a WriteBuffer to write a user type for which this
         * Stats object maintains serialization statistics.
         *
         * @param fBinary  true if the serialization should be optimized to
         *                 produce a Binary object
         *
         * @return a WriteBuffer to write the
         */
        WriteBuffer instantiateBuffer(boolean fBinary)
            {
            long lStats = m_lStats;

            int cbMax = (int) (lStats >>> 32);
            int cbAvg = (int) (lStats & 0x7fffffffL);

            return cbMax > MAX_ALLOC ||
                  (cbMax > 1024 && cbMax > cbAvg + (cbAvg >>> 3))
                ? new MultiBufferWriteBuffer(this)
                : fBinary
                      ? new BinaryWriteBuffer((cbMax + 0xF) & ~0xF)
                      : new ByteArrayWriteBuffer((cbMax + 0xF) & ~0xF);
            }

        // ----- WriteBufferPool interface ----------------------------------

        /**
         * Determine the largest amount of aggregate WriteBuffer capacity
         * that this factory can provide.
         *
         * @return the number of bytes that can be stored in the WriteBuffer
         *         objects that may be returned from this factory
         */
        public int getMaximumCapacity()
            {
            return Integer.MAX_VALUE;
            }

        /**
         * Allocate a WriteBuffer for use by the MultiBufferWriteBuffer. The
         * MultiBufferWriteBuffer calls this factory method when it exhausts
         * the storage capacity of previously allocated WriteBuffer objects.
         * <p>
         * Note that the returned WriteBuffer is expected to be empty, and
         * its capacity is expected to be identical to its maximum capacity,
         * i.e. it is not expected to resize itself, since the purpose of the
         * MultiBufferWriteBuffer is to act as a dynamically-sized
         * WriteBuffer.
         *
         * @param cbPreviousTotal  the total number of bytes of capacity of
         *        the WriteBuffer objects that the MultiBufferWriteBuffer has
         *        thus far consumed
         *
         * @return an empty WriteBuffer suitable for writing to
         */
        public WriteBuffer allocate(int cbPreviousTotal)
            {
            int cb;

            if (cbPreviousTotal <= 0)
                {
                // the smaller of: the biggest we've ever seen, or 1/8 more
                // than the average

                long lStats = m_lStats;

                int cbMax = (int) (lStats >>> 32);
                int cbAvg = (int) (lStats & 0x7fffffffL);
                int cbNew = cbAvg + (cbAvg >>> 3);

                cb = Math.min(MAX_ALLOC, Math.min(cbMax, cbNew <= 0 ? Integer.MAX_VALUE : cbNew));
                }
            else if (cbPreviousTotal <= 1024)
                {
                // grow 100% (and at least by the minimum allocation size)
                cb = Math.max(MIN_ALLOC, cbPreviousTotal);
                }
            else if (cbPreviousTotal <= 4096)
                {
                // grow 50%
                cb = cbPreviousTotal >>> 1;
                }
            else
                {
                // grow 25%
                cb = Math.min(MAX_ALLOC, cbPreviousTotal >>> 2);
                }

            validateBufferSize(cb + cbPreviousTotal);

            return new BinaryWriteBuffer(cb);
            }

        /**
         * Returns a WriteBuffer to the pool.
         *
         * @param buffer  the WriteBuffer that is no longer being used
         */
        public void release(WriteBuffer buffer)
            {
            }

        // ----- constants --------------------------------------------------

        /**
         * The smallest allocation for a MultiBufferWriteBuffer.
         */
        private static final int MIN_ALLOC = 128;     // 1/8 KB

        /**
         * The largest allocation for a MultiBufferWriteBuffer.
         */
        private static final int MAX_ALLOC = 1 << 20; // 1 MB

        /**
         * The expiry for statistics (in milliseconds).
         */
        private static final int EXPIRY_MILLIS = 10 * 60 * 1000; // 10 minutes

        // ----- data members -----------------------------------------------

        /**
         * <ul>
         * <li>high 2 bytes - Number of items that have been submitted for
         * statistics keeping.</li>
         * <li>low 6 bytes - Total number of bytes of all the items
         * submitted.</li>
         * </ul>
         */
        private volatile long m_lAccum;

        /**
         * <ul>
         * <li>highWord - Largest size in bytes of all the items
         * submitted.</li>
         * <li>lowWord  - The average size in bytes of all the items
         * submitted.</li>
         * </ul>
         */
        private volatile long m_lStats;

        /**
         * Time at which this Stats object was created.
         */
        private volatile long m_ldtCreated;
        }


    // ----- inner class: FormatAwareCompressor -----------------------------

    /**
     * Return a DeltaCompressor suitable for compressing/extracting binaries
     * produced by ExternalizableHelper using the specified serializer.  The
     * returned DeltaCompressor will use the specified compressor to
     * compress/extract binaries in the format produced directly by the specified
     * serializer.
     *
     * @param serializer  the serializer whose produced binary format is consumed
     *                    by the specified compressor
     * @param compressor  the compressor
     *
     * @return a DeltaCompressor
     */
    public static DeltaCompressor getDeltaCompressor(
            Serializer serializer, DeltaCompressor compressor)
        {
        return serializer instanceof DefaultSerializer
            ? compressor : new FormatAwareCompressor(compressor);
        }

    /**
     * A DeltaCompressor wrapper implementation that removes/replaces the
     * serialization format byte (FMT_EXT) before/after delegating to the
     * underlying compressor.
     */
    public static class FormatAwareCompressor
            implements DeltaCompressor
        {
        /**
         * Construct a FormatAwareCompressor.
         *
         * @param compressor  the underlying compressor
         */
        public FormatAwareCompressor(DeltaCompressor compressor)
            {
            m_compressor = compressor;
            }

        // ----- DeltaCompressor methods ------------------------------------

        /**
         * Compare an old value to a new value and generate a delta that represents
         * the changes that must be made to the old value in order to transform it
         * into the new value.  The generated delta must be a ReadBuffer of non-zero
         * length.
         * <p>
         * If the old value is null, the generated delta must be a "replace", meaning
         * that applying it to any value must produce the specified new value.
         *
         * @param bufOld  the old value
         * @param bufNew  the new value; must not be null
         *
         * @return the changes that must be made to the old value in order to
         *         transform it into the new value, or null to indicate no change
         */
        public ReadBuffer extractDelta(ReadBuffer bufOld, ReadBuffer bufNew)
            {
            // strip the FMT_EXT byte
            if (bufOld != null)
                {
                azzert(bufOld.byteAt(0) == FMT_EXT);
                bufOld = bufOld.getReadBuffer(1, bufOld.length() - 1);
                }

            azzert(bufNew.byteAt(0) == FMT_EXT);
            bufNew = bufNew.getReadBuffer(1, bufNew.length() - 1);

            return m_compressor.extractDelta(bufOld, bufNew);
            }

        /**
         * Apply a delta to an old value in order to create a new value.
         *
         * @param bufOld    the old value
         * @param bufDelta  the delta information returned from
         *                  {@link #extractDelta} to apply to the old value
         *
         * @return the new value
         */
        public ReadBuffer applyDelta(ReadBuffer bufOld, ReadBuffer bufDelta)
            {
            // strip the FMT_EXT byte from the old value
            if (bufOld != null && bufOld.length() > 0)
                {
                azzert(bufOld.byteAt(0) == FMT_EXT);
                bufOld = bufOld.getReadBuffer(1, bufOld.length() - 1);
                }

            ReadBuffer  bufNew = m_compressor.applyDelta(bufOld, bufDelta);
            WriteBuffer buffer = new BinaryWriteBuffer(bufNew.length() + 1);

            // replace the FMT_EXT byte
            try
                {
                BufferOutput out = buffer.getBufferOutput();
                out.writeByte(FMT_EXT);
                out.writeBuffer(bufNew);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }

            return buffer.toBinary();
            }

        // ----- Object methods ---------------------------------------------

        /**
         * Returns a string representation of the object.
         *
         * @return  a string representation of the object
         */
        public String toString()
            {
            return "FormatAwareCompressor {" + m_compressor + "}";
            }

        // ----- data members -----------------------------------------------

        /**
         * The wrapped DeltaCompressor.
         */
        protected DeltaCompressor m_compressor;
        }


    // ----- inner class: DecoratedMultiBufferReadBuffer --------------------

    /**
     * DecoratedMultiBufferReadBuffer is a MultiBufferWriteBuffer that
     * represents a binary "decorated" value, and allows a more
     * optimized {@link #getUndecorated(ReadBuffer)} operation.
     *
     * @see #decorate(ReadBuffer, int, ReadBuffer)
     * @see #getUndecorated(ReadBuffer)
     */
    public static class DecoratedMultiBufferReadBuffer
            extends MultiBufferReadBuffer
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a DecoratedMultiBufferReadBuffer for the specified value.
         *
         * @param bufValue  the undecorated value
         * @param abuf      the array of ReadBuffers from which to construct this
         *                  DecoratedMultiBufferReadBuffer
         */
        public DecoratedMultiBufferReadBuffer(ReadBuffer bufValue, ReadBuffer[] abuf)
            {
            super(abuf);

            m_bufValue = bufValue;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the undecorated value.
         *
         * @return the undecorated value
         */
        public ReadBuffer getUndecorated()
            {
            return m_bufValue;
            }

        // ----- data members -----------------------------------------------

        /**
         * The undecorated value.
         */
        protected final ReadBuffer m_bufValue;
        }


    // ----- ClassLoader helpers --------------------------------------------

    /**
     * Attempt to load the specified class using sequentionally all of the
     * specified loaders, the ContextClassLoader and the current class loader.
     *
     * @param sClass   the class name
     * @param loader1  first ClassLoader to try
     * @param loader2  second ClassLoader to try
     *
     * @return the Class for the specified name
     *
     * @throws ClassNotFoundException if all the attempts fail
     */
    public static Class loadClass(String sClass, ClassLoader loader1, ClassLoader loader2)
            throws ClassNotFoundException
        {
        for (int i = 1; i <= 3; i++)
            {
            ClassLoader loader;
            switch (i)
                {
                case 1:
                    loader = loader1;
                    break;

                case 2:
                    loader = loader2;
                    break;

                case 3:
                    loader = getContextClassLoader();
                    if (loader == loader1 || loader == loader2)
                        {
                        loader = null;
                        }
                    break;

                default:
                    throw new IllegalStateException();
                }

            try
                {
                if (loader != null)
                    {
                    return loader.loadClass(sClass);
                    }
                }
            catch (ClassNotFoundException ignore) {}
            }

        // nothing worked; try the current class loader as a last chance
        return ExternalizableHelper.class.getClassLoader().loadClass(sClass);
        }

    /**
     * Attempt to find a valid, resolvable URL for the specified resource,
     * first by using each of the specified ClassLoaders, then using the
     * ContextClassLoader.
     *
     * @param sName    the resource name
     * @param loader1  first ClassLoader to try
     * @param loader2  second ClassLoader to try
     *
     * @return the URL for the specified resource name, or null if the
     *         resource could not be found
     */
    public static URL loadResource(String sName, ClassLoader loader1, ClassLoader loader2)
        {
        for (int i = 1; i <= 3; i++)
            {
            ClassLoader loader;
            switch (i)
                {
                case 1:
                    loader = loader1;
                    break;

                case 2:
                    loader = loader2;
                    break;

                case 3:
                    loader = getContextClassLoader();
                    if (loader == loader1 || loader == loader2)
                        {
                        loader = null;
                        }
                    break;

                default:
                    throw new IllegalStateException();
                }

            if (loader != null)
                {
                URL url = loader.getResource(sName);
                if (url != null)
                    {
                    return url;
                    }
                }
            }

        return null;
        }


    // ----- stream wrappers ------------------------------------------------

    /**
     * Get an InputStream for the passed DataInput object.
     *
     * @param in  an Object implementing the DataInput interface
     *
     * @return an Object of type InputStream
     */
    public static InputStream getInputStream(final DataInput in)
        {
        return in instanceof InputStream
               ? (InputStream) in
               : new InputStream()
                   {
                   public int read()
                           throws IOException
                       {
                       try
                           {
                           return in.readUnsignedByte();
                           }
                       catch (EOFException e)
                           {
                           return -1;
                           }
                       }
                   };
        }

    /**
     * Get an OutputStream for the passed DataOutput object.
     *
     * @param out  an Object implementing the DataOutput interface
     *
     * @return an Object of type OutputStream
     */
    public static OutputStream getOutputStream(DataOutput out)
        {
        return out instanceof ObjectOutput
                ? new ShieldedObjectOutputStream((ObjectOutput) out)
                : new ShieldedDataOutputStream(out);
        }

    /**
     * Get a shielded OutputStream for the passed OutputStream object.
     *
     * @param out  an OutputStream
     *
     * @return an OutputStream that implements the Shielded interface
     */
    public static OutputStream getShieldedOutputStream(OutputStream out)
        {
        return out instanceof Shielded
                    ? out
             : out instanceof ObjectOutput
                    ? new ShieldedObjectOutputStream((ObjectOutput) out)
             : out instanceof DataOutput
                    ? new ShieldedDataOutputStream((DataOutput) out)
                    : new ShieldedOutputStream(out);
        }

    /**
     * Get an ObjectInput for the passed DataInput object.
     *
     * @param in      an Object implementing the DataInput interface
     * @param loader  the ClassLoader to use
     *
     * @return an Object of type ObjectInput
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static ObjectInput getObjectInput(DataInput in, ClassLoader loader)
            throws IOException
        {
        return s_streamfactory.getObjectInput(in, loader, false);
        }

    /**
     * Get a new ObjectInput for the passed DataInput, even if the passed
     * DataInput is an ObjectInput.
     *
     * @param in      an Object implementing the DataInput interface
     * @param loader  the ClassLoader to use
     *
     * @return an Object of type ObjectInput that is guaranteed not to be
     *         the same reference as <code>in</code>
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static ObjectInput getNewObjectInput(DataInput in, ClassLoader loader)
            throws IOException
        {
        return s_streamfactory.getObjectInput(in, loader, true);
        }

    /**
     * Get an ObjectOutput for the passed DataOutput object.
     *
     * @param out  an Object implementing the DataOutput interface
     *
     * @return an Object of type ObjectOutput
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static ObjectOutput getObjectOutput(DataOutput out)
            throws IOException
        {
        return s_streamfactory.getObjectOutput(out);
        }

    /**
     * Determine whether the passed DataOutput handles ClassLoader resolving.
     * Note that the ClassLoader resolving on the "write" side is necessary
     * only to make the stream format symetrical for the "read" side.
     *
     * @param out  an object implementing DataOutput
     *
     * @return true if the object implementing DataOutput also implements
     *         ObjectOutput and handles ClassLoader resolving
     */
    public static boolean isResolving(DataOutput out)
        {
        return out instanceof Resolving
                || out instanceof ShieldedObjectOutputStream
                && ((ShieldedObjectOutputStream) out).isResolving();
        }


    // ----- custom stream format support -----------------------------------

    /**
     * Return the ObjectStreamFactory used to convert DataInput/Output into
     * ObjectInput/Output streams.
     *
     * @return the currently used ObjectStreamFactory
     */
    public static ObjectStreamFactory getObjectStreamFactory()
        {
        return s_streamfactory;
        }

    /**
     * Specify an ObjectStreamFactory that should be used to convert
     * DataInput/Output into ObjectInput/Output streams.
     * <p>
     * <b>Warning:</b> This facility should be used with extreme care; failure
     * to set the ObjectStreamFactory identically on all cluster nodes may
     * render some of all clustered services completely inoperable,
     *
     * @param factory the ObjectStreamFactory to use
     */
    public static void setObjectStreamFactory(ObjectStreamFactory factory)
        {
        s_streamfactory = factory;
        }

    /**
     * Determine if the resource object can be serialized using the
     * DefaultSerializer.
     *
     * @param o  the resource object
     *
     * @return true iff the resource object can be serialized by the
     *         DefaultSerializer
     */
    public static boolean isSerializable(Object o)
        {
        return getStreamFormat(o) != FMT_UNKNOWN;
        }

    /**
     * Internal: Select an optimal stream format to use to store the passed
     * object in a stream (as used by the DefaultSerializer).
     *
     * @param o  an Object
     *
     * @return a stream format to use to store the object in a stream
     */
    public static int getStreamFormat(Object o)
        {
        return  o == null                       ? FMT_NULL
              : o instanceof String             ? FMT_STRING
              : o instanceof Number             ?
                (
                    o instanceof Integer            ? FMT_INT
                  : o instanceof Long               ? FMT_LONG
                  : o instanceof Double             ? FMT_DOUBLE
                  : o instanceof BigInteger         ? FMT_INTEGER
                  : o instanceof BigDecimal         ? FMT_DECIMAL
                  : o instanceof Float              ? FMT_FLOAT
                  : o instanceof Short              ? FMT_SHORT
                  : o instanceof Byte               ? FMT_BYTE
                  :                                   FMT_OBJ_SER
                )
              : o instanceof byte[]             ? FMT_B_ARRAY
              : o instanceof ReadBuffer         ? FMT_BINARY
              : o instanceof XmlBean            ? FMT_XML_BEAN
              : o instanceof IntDecoratedObject ? FMT_IDO
              : o instanceof ExternalizableLite ? FMT_OBJ_EXT
              : o instanceof Boolean            ? FMT_BOOLEAN
              : o instanceof Serializable       ? FMT_OBJ_SER
              : o instanceof Optional           ? FMT_OPT
              : o instanceof OptionalInt        ? FMT_OPT_INT
              : o instanceof OptionalLong       ? FMT_OPT_LONG
              : o instanceof OptionalDouble     ? FMT_OPT_DOUBLE
              : o instanceof XmlSerializable    ? FMT_XML_SER
              :                                   FMT_UNKNOWN;
        }

    /**
     * Determines whether or not the specified serializers are compatible.
     * In other words, this method returns true iff object serialized with the
     * first Serializer can be deserialized by the second and visa versa.
     *
     * @param serializerThis  the first Serializer
     * @param serializerThat  the second Serializer
     *
     * @return true iff the two Serializers are stream compatible
     */
    public static boolean isSerializerCompatible(
            Serializer serializerThis, Serializer serializerThat)
        {
        return serializerThis instanceof PofContext ?
                    serializerThat instanceof PofContext
             : serializerThis == null || serializerThat == null ?
                   serializerThis == serializerThat
             : serializerThis.getClass() == serializerThat.getClass();
        }

    /**
     * Log the message explaining the serializer incompatibility between the
     * specified cache and a service.
     *
     * @param cache      the NamedCache reference
     * @param sService   the service name
     * @param serializer the serializer used by the service
     */
    public static void reportIncompatibleSerializers(NamedCache cache,
            String sService, Serializer serializer)
        {
        Logger.warn("The serializer used by cache \"" + cache.getCacheName() + "\" ("
             + cache.getCacheService().getSerializer() + ") is incompatible with the"
             + " serializer configured for service \""
             + sService + "\" (" + serializer
             + "); therefore, cached keys and values will be"
             + " converted via serialization. This will result in"
             + " increased CPU and memory utilization. If possible,"
             + " consider reconfiguring either serializer.");
        }


    // ----- decorated Binary support ---------------------------------------

    /**
     * If the ReadBuffer is the result of serialization by ExternalizableHelper,
     * determine if the buffer contains decorations.
     * <p>
     * Note: This method can only be used against ReadBuffers that result
     * from serialization by ExternalizableHelper or buffers that are already
     * decorated.
     *
     * @param buf  the ReadBuffer to check
     *
     * @return true iff the ReadBuffer is decorated
     */
    public static boolean isDecorated(ReadBuffer buf)
        {
        if (buf != null && buf.length() > 1)
            {
            byte b = buf.byteAt(0);
            return (b == FMT_BIN_DECO || b == FMT_BIN_EXT_DECO);
            }
        return false;
        }

    /**
     * If the ReadBuffer is the result of serialization by ExternalizableHelper,
     * determine if the buffer contains the specified decoration.
     * <p>
     * Note: This method can only be used against ReadBuffers that result
     * from serialization by ExternalizableHelper or buffers that are already
     * decorated.
     *
     * @param buf  the ReadBuffer to check
     * @param nId  the identifier for the decoration to check
     *
     * @return true iff the ReadBuffer is decorated with the specified decoration
     */
    public static boolean isDecorated(ReadBuffer buf, int nId)
        {
        if (buf != null && buf.length() > 1)
            {
            byte b = buf.byteAt(0);
            if (b == FMT_BIN_DECO)
                {
                if (nId >= Byte.SIZE)
                    {
                    return false;
                    }
                long nBits = buf.byteAt(1) & 0xFF;
                long nMask = 1L << nId;
                return (nBits & nMask) != 0L;
                }
            else if (b == FMT_BIN_EXT_DECO)
                {
                BufferInput in = buf.getBufferInput();
                try
                    {
                    int  nFmt  = in.readUnsignedByte();
                    long nBits = in.readPackedLong();
                    long nMask = 1L << nId;
                    return (nBits & nMask) != 0L;
                    }
                catch (IOException e)
                    {
                    throw ensureRuntimeException(e);
                    }
                }
            }
        return false;
        }

    /**
     * Decorate the passed value with the specified decoration and return the
     * resulting Binary. The passed value may or may not already be decorated.
     * <p>
     * The decoration id must be in range defined by {@link #DECO_ID_MIN} and
     * {@link #DECO_ID_MAX}. A series of <tt>DECO_*</tt> constants are defined
     * for the pre-approved decorations, including:
     * <ul><li>{@link #DECO_VALUE} stores the original, undecorated value;</li>
     * <li>{@link #DECO_EXPIRY}, {@link #DECO_STORE}, {@link #DECO_TX} and
     * {@link #DECO_PUSHREP} are used by various facilities of Coherence;</li>
     * <li>{@link #DECO_TOPLINK} and {@link #DECO_WLS} are assigned to Oracle
     * products;</li>
     * <li>{@link #DECO_APP_1}, {@link #DECO_APP_2} and {@link #DECO_APP_3} are
     * made available for use by application developers;</li>
     * <li>{@link #DECO_CUSTOM} is another application-definable decoration,
     * but one that has also been used by Oracle frameworks and products in the
     * past, which means that a potential exists for collisions.</li></ul>
     * <p>
     * All other potential decoration id values are reserved. Product and
     * framework developers that require a new decoration id to be assigned to
     * them should contact the Coherence product development team.
     * <p>
     * Note: This method can only be used against Binary values that result
     * from serialization by ExternalizableHelper or Binary values that are
     * already decorated.
     *
     * @param bin            the Binary to decorate, which may already be
     *                       decorated
     * @param nId            the identifier for the decoration, one of the
     *                       <tt>DECO_*</tt> constants
     * @param binDecoration  the decoration to apply; a null value will remove
     *                       the specified decoration
     *
     * @return a decorated Binary containing the passed decoration
     */
    public static Binary decorate(Binary bin, int nId, Binary binDecoration)
        {
        // Note: "upcasts" are necessary in order to bind to the correct method
        return asBinary(decorate((ReadBuffer) bin, nId, (ReadBuffer) binDecoration));
        }

    /**
     * Decorate the passed value with the specified decoration and return the
     * resulting ReadBuffer. The passed value may or may not already be decorated.
     * <p>
     * The decoration id must be in range defined by {@link #DECO_ID_MIN} and
     * {@link #DECO_ID_MAX}. A series of <tt>DECO_*</tt> constants are defined
     * for the pre-approved decorations, including:
     * <ul><li>{@link #DECO_VALUE} stores the original, undecorated value;</li>
     * <li>{@link #DECO_EXPIRY}, {@link #DECO_STORE}, {@link #DECO_TX} and
     * {@link #DECO_PUSHREP} are used by various facilities of Coherence;</li>
     * <li>{@link #DECO_TOPLINK} and {@link #DECO_WLS} are assigned to Oracle
     * products;</li>
     * <li>{@link #DECO_APP_1}, {@link #DECO_APP_2} and {@link #DECO_APP_3} are
     * made available for use by application developers;</li>
     * <li>{@link #DECO_CUSTOM} is another application-definable decoration,
     * but one that has also been used by Oracle frameworks and products in the
     * past, which means that a potential exists for collisions.</li></ul>
     * <p>
     * All other potential decoration id values are reserved. Product and
     * framework developers that require a new decoration id to be assigned to
     * them should contact the Coherence product development team.
     * <p>
     * Note: This method can only be used against ReadBuffers that result from
     * serialization by ExternalizableHelper or buffers that are already
     * decorated.
     *
     * @param bufOrig  the ReadBuffer to decorate, which may already be decorated
     * @param nId      the identifier for the decoration, one of the
     *                 <tt>DECO_*</tt> constants
     * @param bufDeco  the decoration to apply; a null value will remove the
     *                 specified decoration
     *
     * @return a decorated ReadBuffer containing the passed decoration
     */
    public static ReadBuffer decorate(ReadBuffer bufOrig, int nId, ReadBuffer bufDeco)
        {
        // consider a null decoration to indicate that the decoration should
        // be removed
        if (bufDeco == null)
            {
            return undecorate(bufOrig, nId);
            }

        if (nId < DECO_ID_MIN || nId > DECO_ID_MAX)
            {
            throw new IndexOutOfBoundsException(
                    "decoration index is out of range: index=" + nId
                    + ", min=" + DECO_ID_MIN + ", max=" + DECO_ID_MAX);
            }

        boolean fDecorated = isDecorated(bufOrig);
        if (!fDecorated && nId == DECO_VALUE && !isDecorated(bufDeco))
            {
            // the "value" decoration can be returned as the result, as long
            // as there are no other decorations, except if the value is
            // itself decorated, in which case it must be stored as a "value"
            // decoration so that the corresponding call to getDecoration()
            // will return the correct value
            return bufDeco;
            }

        // this algorithm inserts/replaces/appends a decoration by determining
        // which portion (the "front") of the current decorated value needs to
        // be copied to the start of the new value, and which portion (the
        // "back") of the current decorated value needs to be copied to the
        // end of the new value:
        // 1) assuming the updated decoration will not be the first decoration
        //    in the resulting decorated binary, then the offset to copy from
        //    will be 2 for the 8-bit FMT_BIN_DECO (the first byte after the
        //    format identifier and the bit mask for the decoration
        //    identifiers) or somewhere between 2 and 11 for the packed-long-
        //    utilizing FMT_BIN_EXT_DECO
        // 2) if the new decoration is replacing an existing decoration, then
        //    the offset of the decoration to be replaced needs to be located;
        //    otherwise the offset of the following decoration needs to be
        //    located; either way, that will be the offset to copy up to (before
        //    the new decoration)
        // 3) then the new decoration will be appended
        // 4) then the first existing decoration past the new decoration will
        //    be appended, along with all trailing decorations
        // the algorithm is modified slightly if the current value is not
        // decorated
        long nBits   = (1L << nId);
        int  ofFront = -1;
        int  cbFront = 0;
        int  ofBack  = -1;
        int  cbBack  = 0;

        // there is a situation in which the front data to copy has not yet
        // been length-encoded since it is not yet a decorated binary
        boolean fEncodeFrontLength = false;

        // up to 8 bits can be encoded in the legacy format, otherwise the
        // "extended" format will be used
        boolean fExtended = false;

        if (bufOrig == null)
            {
            // there is no old value to decorate
            bufOrig = Binary.NO_BINARY;
            }
        else if (fDecorated)
            {
            try
                {
                BufferInput in = bufOrig.getBufferInput();

                // the first byte is the format, either "legacy" or "extended"
                fExtended = in.readByte() != FMT_BIN_DECO;

                // next comes the bit-mask of decoration id's
                long nPrevBits = fExtended
                        ? in.readPackedLong() : (long) in.readUnsignedByte();

                // the offset is now at the start of the first decoration
                ofFront = in.getOffset();

                // incorporate the previously existing decorations into the
                // bits we're collecting for the new decorated value
                nBits |= nPrevBits;

                // determine which decoration id's are the last to come before
                // and the first to come after the specified decoration id
                int nLastFront = indexOfMSB(nPrevBits & ((1L << nId) - 1L));
                int nFirstBack = indexOfLSB(nPrevBits & (-1L << (nId + 1)));

                for (int iCurrentId = 0; nPrevBits != 0; ++iCurrentId)
                    {
                    if ((nPrevBits & 0x01L) != 0)
                        {
                        if (iCurrentId == nFirstBack)
                            {
                            // we're at the start of the first decoration that
                            // follows the one that we're inserting/replacing
                            ofBack = in.getOffset();
                            cbBack = bufOrig.length() - ofBack;
                            }

                        // the format for each decoration is its packed length
                        // followed by the bytes of its binary value
                        in.skipBytes(in.readPackedInt());

                        if (iCurrentId == nLastFront)
                            {
                            // we're at the end of the last decoration
                            // that precedes the one that we're
                            // inserting/replacing/appending, i.e. at the
                            // point where we'll write the new decoration
                            cbFront = in.getOffset() - ofFront;
                            }
                        }

                    nPrevBits >>>= 1;
                    }
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        else
            {
            // the value to decorate is not yet decorated;
            // store the old value itself as the "value decoration"
            nBits  |= (1L << DECO_VALUE);
            ofFront = 0;
            cbFront = bufOrig.length();
            fEncodeFrontLength = true;
            }

        // determine if the new decorated binary will use the extended format
        fExtended |= nId >= Byte.SIZE;

        // figure out the total length of the decorated binary
        int cbNew = bufDeco.length();

        // testing has shown that for small binaries, avoiding the "copy" is a
        // performance loss.  For larger binaries, it is a win.
        if (cbFront + cbNew + cbBack > 128)
            {
            WriteBuffer  bufWrite = new ByteArrayWriteBuffer(1 + 10 + 5 + 5);
            BufferOutput out      = bufWrite.getBufferOutput();

            try
                {
                // write the format id and the decoration id's (as a bit mask)
                if (fExtended)
                    {
                    out.writeByte(FMT_BIN_EXT_DECO);
                    out.writePackedLong(nBits);
                    }
                else
                    {
                    out.writeByte(FMT_BIN_DECO);
                    out.writeByte((byte) nBits);
                    }

                if (fEncodeFrontLength)
                    {
                    // here, cbFront is the length of the value.
                    // Write the value length right after the mask
                    out.writePackedInt(cbFront);
                    }

                // portion of the temp buffer that precedes the existing decorations
                int cbHead = out.getOffset();

                // write the new decoration length
                out.writePackedInt(cbNew);

                // portion of the temp buffer that follows the existing decorations
                int cbTail = out.getOffset() - cbHead;

                ReadBuffer   bufRead = bufWrite.toBinary();
                ReadBuffer[] abuf    = new ReadBuffer[]
                    {bufRead.getReadBuffer(0, cbHead),
                     cbFront > 0 ? bufOrig.getReadBuffer(ofFront, cbFront) : Binary.NO_BINARY,
                     bufRead.getReadBuffer(cbHead, cbTail),
                     bufDeco,
                     cbBack > 0 ? bufOrig.getReadBuffer(ofBack, cbBack) : Binary.NO_BINARY};

                return fEncodeFrontLength  // fEncodeFrontLength implies bufOrig is undecorated
                    ? new DecoratedMultiBufferReadBuffer(bufOrig, abuf)
                    : new MultiBufferReadBuffer(abuf);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        else
            {
            int cbTotal = 1 + (fExtended ? calculatePackedLength(nBits) : 1)
                        + cbFront  + calculatePackedLength(cbNew) + cbNew + cbBack;
            if (fEncodeFrontLength)
                {
                cbTotal += calculatePackedLength(cbFront);
                }

            WriteBuffer  bufNew = new BinaryWriteBuffer(cbTotal, cbTotal);
            BufferOutput out    = bufNew.getBufferOutput();
            try
                {
                // write the format id and the decoration id's (as a bit mask)
                if (fExtended)
                    {
                    out.writeByte(FMT_BIN_EXT_DECO);
                    out.writePackedLong(nBits);
                    }
                else
                    {
                    out.writeByte(FMT_BIN_DECO);
                    out.writeByte((byte) nBits);
                    }

                // write any leading decorations
                if (fEncodeFrontLength)
                    {
                    // here, cbFront is the length of the value.
                    // Write the value length right after the mask
                    out.writePackedInt(cbFront);
                    }

                if (cbFront > 0)
                    {
                    // copy any bytes in front of the decoration
                    out.writeBuffer(bufOrig, ofFront, cbFront);
                    }

                // write the new decoration
                out.writePackedInt(cbNew);
                out.writeBuffer(bufDeco);

                // write any trailing decorations
                if (cbBack > 0)
                    {
                    out.writeBuffer(bufOrig, ofBack, cbBack);
                    }
                assert out.getOffset() == cbTotal;

                return bufNew.toBinary();
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
     * Decorate the passed Binary with the passed decorations. If the passed
     * Binary is decorated, use its decorations as the defaults and the passed
     * decorations as the overrides; if the passed Binary is not decorated,
     * then use it as the default for the {@link #DECO_VALUE "value"}
     * decoration.
     * <p>
     * Note: This method can only be used against Binary values that result
     * from serialization by ExternalizableHelper or Binary values that are
     * already decorated.
     *
     * @param bin              the Binary to decorate, which may already be
     *                         decorated; may be null
     * @param abinDecorations  the decorations to apply; each non-null element
     *                         is assumed to be a decoration to add whose
     *                         identifier is its index in the array; the array
     *                         contents will not be modified by this method
     *
     * @return a decorated Binary containing the passed decorations
     *
     * @deprecated as of Coherence 3.7.2
     */
    public static Binary decorate(Binary bin, Binary[] abinDecorations)
        {
        // Note: "upcasts" are necessary in order to bind the correct method
        return asBinary(decorate((ReadBuffer) bin, (ReadBuffer[]) abinDecorations));
        }

    /**
     * Decorate the passed Binary with the passed decorations. If the passed
     * Binary is decorated, use its decorations as the defaults and the passed
     * decorations as the overrides; if the passed Binary is not decorated,
     * then use it as the default for the {@link #DECO_VALUE "value"}
     * decoration.
     * <p>
     * Note: This method can only be used against Binary values that result
     * from serialization by ExternalizableHelper or Binary values that are
     * already decorated.
     *
     * @param buf              the Binary to decorate, which may already be
     *                         decorated; may be null
     * @param abufDeco  the decorations to apply; each non-null element
     *                         is assumed to be a decoration to add whose
     *                         identifier is its index in the array; the array
     *                         contents will not be modified by this method
     *
     * @return a decorated Binary containing the passed decorations
     */
    public static ReadBuffer decorate(ReadBuffer buf, ReadBuffer[] abufDeco)
        {
        // if a decorated Binary was passed in, use its decorations to fill in
        // any missing decorations in the passed decoration array; otherwise,
        // if the Binary passed in is NOT decorated, then use it to fill in
        // the "value" decoration in the passed decoration array
        if (isDecorated(buf))
            {
            // whatever decorations already exist in the passed-in binary
            // should be retained, unless the passed in decorations
            // override them
            ReadBuffer[] abufOrig = getDecorations(buf);
            ReadBuffer[] abufNew  = abufDeco;
            int          cbufOrig = abufOrig.length;
            int          cbufOver = abufNew.length;
            if (cbufOrig >= cbufOver)
                {
                abufDeco = abufOrig;
                for (int i = 0; i < cbufOver; ++i)
                    {
                    if (abufNew[i] != null)
                        {
                        abufDeco[i] = abufNew[i];
                        }
                    }
                }
            else
                {
                // avoid changing the contents of the passed array
                abufDeco = abufDeco.clone();

                for (int i = 0; i < cbufOrig; ++i)
                    {
                    if (abufDeco[i] == null)
                        {
                        abufDeco[i] = abufOrig[i];
                        }
                    }
                }
            }
        else if (buf != null && (abufDeco.length < 1 || abufDeco[0] == null))
            {
            // the passed-in binary is the value to be decorated, which
            // is encoded as the "zero-eth" decoration
            ReadBuffer[] abufOverride = abufDeco;
            int          cbufOverride = abufOverride.length;
            abufDeco = new ReadBuffer[Math.max(1, cbufOverride)];
            if (cbufOverride > 0)
                {
                System.arraycopy(abufOverride, 0, abufDeco, 0, cbufOverride);
                }
            abufDeco[0] = buf;
            }

        // at this point, all of the information that will be in the resulting
        // Binary is in the abufDeco array; the contents need to be
        // analyzed to determine what the result will look like
        int  cbTotal      = 1; // 1-byte serialization format identifier
        int  cDecorations = 0;
        int  nLastId      = -1;
        long nBits        = 0L;
        int  cBufDeco     = abufDeco.length;
        for (int i = 0; i < cBufDeco; ++i)
            {
            ReadBuffer bufDeco = abufDeco[i];
            if (bufDeco != null)
                {
                int cb = bufDeco.length();

                cbTotal      += calculatePackedLength(cb) + cb;
                cDecorations += 1;
                nLastId       = i;
                nBits        |= (1L << i);
                }
            }

        if (cDecorations == 0)
            {
            // there is nothing
            return null;
            }
        else if (cDecorations == 1 && nLastId == DECO_VALUE
                && !isDecorated(abufDeco[DECO_VALUE]))
            {
            // all there is is a value, which itself is undecorated, so return
            // it as an undecorated value
            return abufDeco[DECO_VALUE];
            }
        else if (nLastId > DECO_ID_MAX)
            {
            // there is too much
            throw new IndexOutOfBoundsException("decoration id out of bounds: "
                + nLastId);
            }

        // use the legacy binary decoration format for id's up to 7; otherwise
        // use the "extended" binary decoration format
        boolean fExtended = nLastId >= Byte.SIZE;
        cbTotal += fExtended ? calculatePackedLength(nBits) : 1;

        BinaryWriteBuffer bufNew = new BinaryWriteBuffer(cbTotal, cbTotal);
        BufferOutput      out    = bufNew.getBufferOutput();
        try
            {
            out.writeByte(fExtended ? FMT_BIN_EXT_DECO : FMT_BIN_DECO);
            if (fExtended)
                {
                out.writePackedLong(nBits);
                }
            else
                {
                out.write((byte) nBits);
                }

            for (int i = 0; i <= nLastId; ++i)
                {
                ReadBuffer bufDeco = abufDeco[i];
                if (bufDeco != null)
                    {
                    out.writePackedInt(bufDeco.length());
                    out.writeBuffer(bufDeco);
                    }
                }
            assert out.getOffset() == cbTotal;
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }

        return bufNew.toBinary();
        }

    /**
     * Extract and return the specified decoration from the passed Binary.
     * <p>
     * Note: This method can only be used against Binary values that result
     * from serialization by ExternalizableHelper or Binary values that are
     * already decorated.
     *
     * @param bin  the Binary that may be decorated
     * @param nId  the identifier for the decoration
     *
     * @return the Binary decoration, or null if the passed Binary is not
     *         decorated or if no decoration was found for the specified
     *         identifier
     *
     * @deprecated as of Coherence 3.7.2
     */
    public static Binary getDecoration(Binary bin, int nId)
        {
        // Note: "upcast" is necessary in order to bind the correct method
        return asBinary(getDecoration((ReadBuffer) bin, nId));
        }

    /**
     * Extract and return the specified decoration from the passed ReadBuffer.
     * <p>
     * Note: This method can only be used against ReadBuffers that result
     * from serialization by ExternalizableHelper or buffers that are already
     * decorated.
     *
     * @param buf  the ReadBuffer that may be decorated
     * @param nId  the identifier for the decoration
     *
     * @return the decoration, or null if the passed ReadBuffer is not decorated
     *         or if no decoration was found for the specified identifier
     */
    public static ReadBuffer getDecoration(ReadBuffer buf, int nId)
        {
        if (!isDecorated(buf) || nId < DECO_ID_MIN || nId > DECO_ID_MAX)
            {
            return nId == DECO_VALUE ? buf : null;
            }

        // first byte is the format identifier, followed by the 1-byte bit-
        // encoded list of decoration IDs (or a packed long for the extended
        // format), followed by the length of the first decoration's Binary,
        // followed by the first decoration's binary data, and so on
        BufferInput in = buf.getBufferInput();
        try
            {
            int  nFmt  = in.readUnsignedByte();
            long nBits = nFmt == FMT_BIN_DECO
                    ? (long) in.readUnsignedByte() : in.readPackedLong();
            long nMask = 1L << nId;
            if ((nBits & nMask) == 0L)
                {
                return null;
                }

            for (int i = 0; i < nId; ++i)
                {
                if ((nBits & 0x01L) != 0)
                    {
                    in.skipBytes(in.readPackedInt());
                    }
                nBits >>>= 1;
                }

            int cb = in.readPackedInt();
            int of = in.getOffset();
            return buf.getReadBuffer(of, cb);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
     * Return an array containing all of the decorations from the passed
     * ReadBuffer.
     * <p>
     * If the passed value is not decorated, then the result is a single-
     * element array containing the undecorated value, which is the
     * DECO_VALUE decoration.
     * <p>
     * Note: This method can only be used against ReadBuffers that result
     * from serialization by ExternalizableHelper or buffers that are
     * already decorated.
     *
     * @param buf  the ReadBuffer that may be decorated
     *
     * @return an array of all decorations on the passed ReadBuffer, indexed by
     *         the DECO_* constants
     */
    public static ReadBuffer[] getDecorations(ReadBuffer buf)
        {
        if (!isDecorated(buf))
            {
            // The value is translated into the DECO_VALUE decoration
            return new ReadBuffer[] {buf};
            }

        BufferInput in = buf.getBufferInput();
        try
            {
            // determine how many decorations there are (there has to be at
            // least one, or it wouldn't be "decorated")
            int  nFmt  = in.readUnsignedByte();
            long nBits = nFmt == FMT_BIN_DECO
                    ? in.readUnsignedByte() : in.readPackedLong();
            int  ofMSB = indexOfMSB(nBits);
            assert ofMSB >= DECO_ID_MIN && ofMSB <= DECO_ID_MAX;

            int          cbufDeco = ofMSB + 1;
            ReadBuffer[] abufDeco = new ReadBuffer[cbufDeco];

            for (int i = 0; i < cbufDeco; i++)
                {
                if ((nBits & 0x01L) != 0)
                    {
                    int cb = in.readPackedInt();
                    int of = in.getOffset();
                    abufDeco[i] = buf.getReadBuffer(of, cb);
                    in.skipBytes(cb);
                    }
                nBits >>>= 1;
                }

            return abufDeco;
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
     * Remove the specified decoration from the passed Binary. If the resulting
     * Binary has no decorations remaining, then return the undecorated Binary;
     * otherwise return the decorated Binary with the remaining decorations.
     * <p>
     * Note: This method can only be used against Binary values that result
     * from serialization by ExternalizableHelper or Binary values that are
     * already decorated.
     *
     * @param bin  the Binary to undecorate
     * @param nId  the identifier for the decoration to remove
     *
     * @return a Binary that may be decorated or undecorated
     *
     * @deprecated as of Coherence 3.7.2
     */
    public static Binary undecorate(Binary bin, int nId)
        {
        // Note: "upcast" is necessary in order to bind the correct method
        return asBinary(undecorate((ReadBuffer) bin, nId));
        }

    /**
     * Remove the specified decoration from the passed ReadBuffer and return the
     * resulting contents (which may be undecorated, or contain the remaining
     * decorations).
     * <p>
     * Note: This method can only be used against ReadBuffers that result from
     * serialization by ExternalizableHelper or buffers that are already decorated.
     *
     * @param buf  the ReadBuffer to undecorate
     * @param nId  the identifier for the decoration to remove
     *
     * @return a ReadBuffer that may or may not be decorated
     */
    public static ReadBuffer undecorate(ReadBuffer buf, int nId)
        {
        // verify that the binary is decorated and that the ID to remove is
        // even legitimate
        if (!isDecorated(buf, nId) || nId < DECO_ID_MIN || nId > DECO_ID_MAX)
            {
            return nId == DECO_VALUE && !isDecorated(buf) ? null : buf;
            }

        int  nFmt;
        long nBits;
        try
            {
            BufferInput in = buf.getBufferInput();
            nFmt  = in.readUnsignedByte();
            nBits = nFmt == FMT_BIN_DECO
                    ? (long) in.readUnsignedByte() : in.readPackedLong();
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }

        long nMask = 1L << nId;
        if ((nBits & nMask) == 0L)
            {
            return buf;
            }

        long nRemains = nBits & ~nMask;
        if (nRemains == 0L)
            {
            // nothing left
            return null;
            }
        else if (nRemains == (1L << DECO_VALUE))
            {
            // only the original value is left; if it's undecorated, just
            // return it, but if it's decorated, then returning it would
            // be incorrect, since those decorations intrinsic within the
            // "value" portion of the decorated binary would become the
            // decorations of the binary itself
            ReadBuffer bufValue = getUndecorated(buf);
            if (!isDecorated(bufValue))
                {
                return bufValue;
                }
            }

        // extract all the decorations, remove the specified one and
        // re-assemble the remaining ones
        ReadBuffer[] abufDeco = getDecorations(buf);
        abufDeco[nId] = null;
        return decorate(null, abufDeco);
        }

    /**
     * If the passed Binary is decorated, extract the original Binary value
     * that the decorations were added to, otherwise return the passed Binary
     * value.
     * <p>
     * Note: This method can only be used against Binary values that result
     * from serialization by ExternalizableHelper or Binary values that are
     * already decorated.
     *
     * @param bin  the Binary object
     *
     * @return the undecorated Binary value, or null if the Binary value is
     *         decorated but does not contain an original Binary value
     *
     * @deprecated as of Coherence 3.7.2
     */
    public static Binary getUndecorated(Binary bin)
        {
        return asBinary(getDecoration((ReadBuffer) bin, DECO_VALUE));
        }

    /**
     * If the passed ReadBuffer is decorated, extract the original contents
     * that the decorations were added to, otherwise return the passed Binary
     * value.
     * <p>
     * Note: This method can only be used against ReadBuffers that result from
     * serialization by ExternalizableHelper or buffers that are already decorated.
     *
     * @param buf  the ReadBuffer object
     *
     * @return the undecorated ReadBuffer, or null if the specified buffer is
     *         decorated but does not contain an actual value
     */
    public static ReadBuffer getUndecorated(ReadBuffer buf)
        {
        return buf instanceof DecoratedMultiBufferReadBuffer
            ? ((DecoratedMultiBufferReadBuffer) buf).getUndecorated()
            : getDecoration(buf, DECO_VALUE);
        }

    /**
     * Return a Binary representing the contents of the specified ReadBuffer, or
     * null if the buffer is null.
     *
     * @param buf  the read buffer
     *
     * @return the contents of the read buffer as a Binary object, or null
     */
    public static Binary asBinary(ReadBuffer buf)
        {
        return buf == null ? null : buf.toBinary();
        }


    // ----- SerializationSupport helpers -----------------------------------

    /**
     * Potentially replaces specified argument with a different object before
     * serialization.
     *
     * @param o  the object to replace, if necessary
     *
     * @return the replacement object
     *
     * @throws ObjectStreamException if an error occurs
     */
    public static Object replace(Object o)
            throws ObjectStreamException
        {
        // support either static or dynamic lambda
        o = Lambdas.ensureSerializable(o);

        if (o instanceof SerializationSupport)
            {
            o = ((SerializationSupport) o).writeReplace();
            }
        return o;
        }

    /**
     * Realizes object after deserialization by applying post-serialization rules.
     *
     * @param <T>         the class of realized object
     * @param o           the object to realize
     * @param serializer  the serializer that was used to deserialize the object
     *
     * @return fully realized object
     *
     * @throws ObjectStreamException  if an error occurs
     */
    public static <T> T realize(Object o, Serializer serializer)
            throws ObjectStreamException
        {
        if (o instanceof SerializerAware)
            {
            ((SerializerAware) o).setContextSerializer(serializer);
            }

        if (o instanceof SerializationSupport)
            {
            o = ((SerializationSupport) o).readResolve();
            if (o instanceof SerializerAware)
                {
                ((SerializerAware) o).setContextSerializer(serializer);
                }
            }
        return (T) o;
        }

    // ----- command line ---------------------------------------------------

    /**
     * Parse a hex string representing a serialized object.
     * <p>
     * Example:
     * <pre>
     *   java com.tangosol.util.ExternalizableHelper 0x0603486921
     * </pre>
     *
     * @param asArgs  the hex string as the command line arguments
     */
    public static void main(String[] asArgs)
        {
        if (asArgs.length == 0)
            {
            out("Usage:");
            out("java com.tangosol.util.ExternalizableHelper <hex string>");
            }
        else
            {
            Object o = fromByteArray(parseHex(asArgs[0]));
            if (o != null)
                {
                out("Class: " + o.getClass().getName());
                }
            out("Value: " + o);
            }
        }


    // ----- constants ------------------------------------------------------

    /**
     * Serialization format: Unknown value (alias to FMT_UNKNOWN).
     */
    public static final int FMT_NONE          = 255;

    /**
     * Serialization format: Unknown value.
     */
    public static final int FMT_UNKNOWN       = 255;

    /**
     * Serialization format: Null value.
     */
    public static final int FMT_NULL          =  0;

    /**
     * Serialization format: Integer value.
     */
    public static final int FMT_INT           =  1;

    /**
     * Serialization format: Long value.
     */
    public static final int FMT_LONG          =  2;

    /**
     * Serialization format: Double value.
     */
    public static final int FMT_DOUBLE        =  3;

    /**
     * Serialization format: BigInteger value.
     */
    public static final int FMT_INTEGER       =  4;

    /**
     * Serialization format: BigDecimal value.
     */
    public static final int FMT_DECIMAL       =  5;

    /**
     * Serialization format: String value.
     */
    public static final int FMT_STRING        =  6;

    /**
     * Serialization format: Binary value.
     */
    public static final int FMT_BINARY        =  7;

    /**
     * Serialization format: Byte array value.
     */
    public static final int FMT_B_ARRAY       =  8;

    /**
     * Serialization format: XmlSerializable value.
     */
    public static final int FMT_XML_SER      =  9;

    /**
     * Serialization format: ExternalizableLite value.
     */
    public static final int FMT_OBJ_EXT      = 10;

    /**
     * Serialization format: Serializable value.
     */
    public static final int FMT_OBJ_SER      = 11;

    /**
     * Serialization format: XmlBean value.
     */
    public static final int FMT_XML_BEAN     = 12;

    /**
     * Serialization format: Integer-decorated value.
     */
    public static final int FMT_IDO          = 13;

    /**
     * Serialization format: Float value.
     */
    public static final int FMT_FLOAT        = 14;

    /**
     * Serialization format: Short value.
     */
    public static final int FMT_SHORT        = 15;

    /**
     * Serialization format: Byte value.
     */
    public static final int FMT_BYTE         = 16;

    /**
     * Serialization format: Boolean value.
     */
    public static final int FMT_BOOLEAN      = 17;

    /**
     * Serialization format: Decorated Binary value.
     * <p>
     * Structure is:
     * <pre>
     * byte 0    : format identifier (18)
     * byte 1    : bit mask of decoration identifiers (see DECO_* constants)
     * byte 2    : packed int specifying the length of the first decoration
     * byte next : binary data
     * ...
     * </pre>
     * For each decoration, there is a packed int for its length, followed by
     * its binary data. The first decoration is the decorated value itself, if
     * present.
     * <p>
     * Note: FMT_IDO cannot be combined with FMT_BIN_DECO.
     */
    public static final int FMT_BIN_DECO = 18;

    /**
     * Serialization format: Extended-range Decorated Binary value.
     * <p>
     * Structure is:
     * <pre>
     * byte 0    : format identifier (19)
     * byte 1    : bit mask of decoration identifiers (see DECO_* constants),
     *             in the packed long format (1-10 bytes)
     * byte next : packed int specifying the length of the first decoration
     * byte next : binary data
     * ...
     * </pre>
     * For each decoration, there is a packed int for its length, followed by
     * its binary data. The first decoration is the decorated value itself, if
     * present.
     * <p>
     * Note: FMT_IDO cannot be combined with FMT_BIN_EXT_DECO.
     */
    public static final int FMT_BIN_EXT_DECO = 19;

    /**
     * Serialization format: A DefaultSerializer is NOT used.
     */
    public static final int FMT_EXT          = 21;

    /**
     * Serialization format: Optional value.
     */
    public static final int FMT_OPT          = 22;

    /**
     * Serialization format: OptionalInt value.
     */
    public static final int FMT_OPT_INT      = 23;

    /**
     * Serialization format: OptionalLong value.
     */
    public static final int FMT_OPT_LONG     = 24;

    /**
     * Serialization format: OptionalDouble value.
     */
    public static final int FMT_OPT_DOUBLE   = 25;

    /**
     * Decoration range: The minimum decoration identifier.
     */
    public static final int DECO_ID_MIN  = 0;

    /**
     * Decoration range: The maximum decoration identifier.
     */
    public static final int DECO_ID_MAX  = 63;

    /**
     * Decoration: The original value (before being decorated).
     */
    public static final int DECO_VALUE   = 0;

    /**
     * Decoration: The expiry for the value.
     */
    public static final int DECO_EXPIRY  = 1;

    /**
     * Decoration: The persistent state for the value.
     */
    public static final int DECO_STORE   = 2;

    /**
     * Decoration: Information managed on behalf of the {@link
     * com.tangosol.coherence.transaction.OptimisticNamedCache
     * OptimisticNamedCache} implementation.
     */
    public static final int DECO_TX      = 3;

    /**
     * Decoration: Information managed on behalf of Push Replication.
     */
    public static final int DECO_PUSHREP = 4;

    /**
     * Decoration: Suggests the backup is not in sync.
     */
    public static final int DECO_BACKUP  = 5;

    /**
     * Decoration: Reserved for future use by Coherence; do not use.
     */
    public static final int DECO_RSVD_1  = 6;

    /**
     * Decoration: A client specific value (opaque). The original intent of the
     * "custom" decoration was that it would be reserved for use by application
     * code, but previous to Coherence 3.7 it was also used for the
     * OptimisticNamedCache implementation, the TopLink Grid implementation and
     * the Coherence Incubator's "Push Replication" project. As of Coherence
     * 3.7, this attribute is once again made available to frameworks and
     * applications, although care must be taken to avoid using the versions
     * of any of the frameworks that utilized this decoration. Applications are
     * instead encouraged to use the new {@link #DECO_APP_1},
     * {@link #DECO_APP_2} and {@link #DECO_APP_3} decorations.
     */
    public static final int DECO_CUSTOM  = 7;

    /**
     * Decoration: Information managed on behalf of TopLink Grid.
     */
    public static final int DECO_TOPLINK = 8;

    /**
     * Decoration: Information managed on behalf of WebLogic.
     */
    public static final int DECO_WLS     = 9;

    /**
     * Decoration: Application-assignable decoration; this decoration will not
     * be used, modified or overwritten by Coherence or other Oracle products.
     */
    public static final int DECO_APP_1   = 10;

    /**
     * Decoration: Application-assignable decoration; this decoration will not
     * be used, modified or overwritten by Coherence or other Oracle products.
     */
    public static final int DECO_APP_2   = 11;

    /**
     * Decoration: Application-assignable decoration; this decoration will not
     * be used, modified or overwritten by Coherence or other Oracle products.
     */
    public static final int DECO_APP_3   = 12;

    /**
     * Decoration: Information managed on behalf of Memcached acceptor.
     */
    public static final int DECO_MEMCACHED = 13;

    /**
     * Decoration: Holds JCache specific meta-information for an entry.
     */
    public static final int DECO_JCACHE = 14;

    /**
     * Decoration: Indicates if an update or delete is considered
     * to be synthetic for JCache.
     * (this is not the same as a Coherence synthetic update)
     */
    public static final int DECO_JCACHE_SYNTHETIC = 15;

    /**
     * Decoration: Information about a queue element
     */
    public static final int DECO_QUEUE_METADATA = 16;

    /**
     * The maximum number of bytes the header of the binary-decorated value
     * may contain.
     */
    protected static final int MAX_DECO_HEADER_BYTES = 7;

    /**
     * Trints use 6 hexits (3 bytes), so the trint domain span is 0x01000000.
     */
    public static final int TRINT_DOMAIN_SPAN = 0x01000000;

    /**
     * Trints use 6 hexits (3 bytes), so the trint maximum is 0x00FFFFFF.
     */
    public static final int TRINT_MAX_VALUE = 0x00FFFFFF;

    /**
     * Trints use 6 hexits (3 bytes), so the trint maximum variance (from a
     * "current" value) is half the trint domain span, or 0x00800000.
     */
    public static final int TRINT_MAX_VARIANCE = 0x00800000;

    /**
     * An empty array of Binary objects.
     */
    public static final Binary[] EMPTY_BINARY_ARRAY = new Binary[0];

    /**
     * Maximum size of a packed long.
     */
    public static final int PACKED_LONG_SIZE = 10;

    /**
     * Maximum size of a packed int.
     */
    public static final int PACKED_INT_SIZE = 5;

    /**
     * Binary overhead.
     */
    public static final int BINARY_SIZE = 5;


    // ----- converters -----------------------------------------------------

    /**
     * A converter from Object to Binary format that uses the DefaultSerializer.
     *
     * @since Coherence 2.4
     */
    public static final Converter CONVERTER_TO_BINARY = new Converter()
        {
        public Object convert(Object o)
            {
            return toBinary(o, ensureSerializer(null));
            }
        };

    /**
     * A converter from Binary to Object format, which uses the DefaultSerializer.
     *
     * @since Coherence 2.4
     */
    public static final Converter CONVERTER_FROM_BINARY = new Converter()
        {
        public Object convert(Object o)
            {
            return o == null ? null :
                fromBinary((Binary) o, ensureSerializer(null));
            }
        };

    /**
     * A pass-through Binary converter that removes an IntDecoration if present.
     *
     * @since Coherence 3.4
     */
    public static final Converter CONVERTER_STRIP_INTDECO = new Converter()
        {
        public Object convert(Object o)
            {
            if (o != null)
                {
                Binary bin = (Binary) o;
                if (isIntDecorated(bin))
                    {
                    return removeIntDecoration(bin);
                    }
                }
            return o;
            }
        };


    // ----- stream wrappers ------------------------------------------------

    /**
     * Marker interface.
     */
    public interface Shielded
        {
        }

    /**
     * An InputStream that delegates all operations other than close to an
     * underlying InputStream.
     */
    public static class ShieldedInputStream
            extends FilterInputStream
            implements InputStreaming, Shielded
        {
        public ShieldedInputStream(InputStream in)
            {
            super(in);
            }

        public final void close()
            {
            }
        }

    /**
     * An OutputStream that delegates all operations other than flush and
     * close to an underlying OutputStream.
     */
    public static class ShieldedOutputStream
            extends WrapperOutputStream
            implements Shielded
        {
        public ShieldedOutputStream(OutputStream out)
            {
            super(out);
            }

        public final void flush()
            {
            }

        public final void close()
            {
            }
        }

    /**
     * An OutputStream that implements DataOutput that delegates all
     * operations other than flush and close to an underlying object that
     * implements DataOutput.
     */
    public static class ShieldedDataOutputStream
            extends WrapperDataOutputStream
            implements Shielded
        {
        public ShieldedDataOutputStream(DataOutput out)
            {
            super(out);
            }

        public final void flush()
            {
            }

        public final void close()
            {
            }
        }

    /**
     * An OutputStream that implements ObjectOutput that delegates all
     * operations other than flush and close to an underlying object that
     * implements ObjectOutput.
     */
    public static class ShieldedObjectOutputStream
            extends WrapperObjectOutputStream
            implements Shielded
        {
        public ShieldedObjectOutputStream(ObjectOutput out)
            {
            super(out);
            }

        public final void flush()
            {
            }

        public final void close()
            {
            }

        /**
         * Determine whether the underlying DataOutput is resolving.
         */
        boolean isResolving()
            {
            return ExternalizableHelper.isResolving(this.getObjectOutput());
            }
        }

    /**
     * Default ObjectStreamFactory implementation.
     */
    public static class DefaultObjectStreamFactory
            implements ObjectStreamFactory
        {
        /**
         * Obtain an ObjectInput based on the passed DataInput.
         *
         * @param  in         the DataInput to be wrapped
         * @param  loader     the ClassLoader to be used
         * @param  fForceNew  if true, a new ObjectInput must be returned; otherwise,
         *                    if the passed stream is already an ObjectInput, it's
         *                    allowed to be returned instead
         *
         * @return an ObjectInput that delegates to ("wraps") the passed DataInput
         *
         * @throws IOException  if an I/O exception occurs
         */
        public ObjectInput getObjectInput(DataInput in, ClassLoader loader, boolean fForceNew)
                throws IOException
            {
            // check if the passed DataInput supports the necessary contracts
            if (!fForceNew
              && in instanceof ObjectInput
              && (!FORCE_RESOLVING_STREAMS || in instanceof Resolving))
                {
                return (ObjectInput) in;
                }

            InputStream stream = getInputStream(in);
            loader = ensureClassLoader(loader == null && in instanceof WrapperDataInputStream
                         ? ((WrapperDataInputStream) in).getClassLoader()
                         : loader);
            return new ResolvingObjectInputStream(stream, RemotableSupport.get(loader));

            }

        /**
         * Obtain an ObjectOutput based on the passed DataOutput.
         *
         * @param  out  the DataOutput to be wrapped
         *
         * @return an ObjectOutput that delegates to ("wraps") the passed DataOutput
         *
         * @throws IOException  if an I/O exception occurs
         */
        public ObjectOutput getObjectOutput(DataOutput out)
                throws IOException
            {
            if (out instanceof ObjectOutput && (!FORCE_RESOLVING_STREAMS || isResolving(out)))
                {
                // the passed stream supports the necessary contracts, but we
                // have to prevent a nested close() from closing the stream that
                // we have been entrusted with
                return out instanceof Shielded
                        ? (ObjectOutput) out
                        : new ShieldedObjectOutputStream((ObjectOutput) out);
                }
            else
                {
                // block a nested close() from closing the underlying stream
                OutputStream stream = getOutputStream(out);

                return new ResolvingObjectOutputStream(stream);
                }
            }
        }


    // ----- Expiry-decoration helpers --------------------------------------

    /**
     * Return a ReadBuffer whose contents represent the specified buffer with
     * the specified expiry encoded as a <tt>DECO_EXPIRY</tt> decoration.  The
     * encoded expiry can be decoded via the {@link #decodeExpiry} method.
     *
     * @param buf        the buffer to encode
     * @param ldtExpiry  the expiry time, or {@link CacheMap#EXPIRY_DEFAULT} or
     *                   {@link CacheMap#EXPIRY_NEVER}
     *
     * @return an expiry-encoded ReadBuffer
     */
    public static ReadBuffer encodeExpiry(ReadBuffer buf, long ldtExpiry)
        {
        if (ldtExpiry == CacheMap.EXPIRY_DEFAULT)
            {
            return undecorate(buf, DECO_EXPIRY);
            }

        WriteBuffer bufWrite = new BinaryWriteBuffer(8, 8);
        try
            {
            BufferOutput out = bufWrite.getBufferOutput();
            out.writeLong(ldtExpiry < 0L ? CacheMap.EXPIRY_NEVER : ldtExpiry);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }

        return decorate(buf, DECO_EXPIRY, bufWrite.toBinary());
        }

    /**
     * Decode the expiry time from the specified ReadBuffer.
     *
     * @param buf  the buffer to decode
     *
     * @return the decoded expiry, or {@link CacheMap#EXPIRY_DEFAULT} if none exists
     */
    public static long decodeExpiry(ReadBuffer buf)
        {
        long ldtExpiry = CacheMap.EXPIRY_DEFAULT;

        if (!isDecorated(buf, DECO_EXPIRY))
            {
            return ldtExpiry;
            }

        ReadBuffer bufExpiry = getDecoration(buf, DECO_EXPIRY);

        if (bufExpiry != null)
            {
            try
                {
                ldtExpiry = bufExpiry.getBufferInput().readLong();
                }
            catch (Exception e) {}
            }

        return ldtExpiry;
        }


    // ----- int-Decorated values -------------------------------------------

    /**
     * Decorate the specified value with the specified integer decoration.
     *
     * @param oValue       the value to be decorated
     * @param nDecoration  the integer decoration
     *
     * @return  the decorated object
     */
    public static IntDecoratedObject decorate(Object oValue, int nDecoration)
        {
        return new IntDecoratedObject(oValue, nDecoration);
        }

    /**
     * Decorate the specified ReadBuffer with the specified integer decoration.
     *
     * @param bufValue     the ReadBuffer to be decorated
     * @param nDecoration  the integer decoration
     *
     * @return the decorated (with integer decoration) ReadBuffer
     */
    public static ReadBuffer decorateBinary(ReadBuffer bufValue, int nDecoration)
        {
        try
            {
            WriteBuffer  buf = new BinaryWriteBuffer(6 + bufValue.length());
            BufferOutput out = buf.getBufferOutput();

            assert nDecoration != HashEncoded.UNENCODED;

            out.writeByte(FMT_IDO);
            out.writePackedInt(nDecoration);
            out.writeBuffer(bufValue);

            return buf.toBinary();
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
     * Check whether or not the specified ReadBuffer is a representation of an
     * IntDecoratedObject.
     *
     * @param buf  the ReadBuffer
     *
     * @return true iff the buffer contains (starts with) a representation of an
     *         IntDecoratedObject
     *
     * @deprecated use {@link #isIntDecorated(ByteSequence)} instead
     */
    @Deprecated
    public static boolean isIntDecorated(ReadBuffer buf)
        {
        return isIntDecorated((ByteSequence) buf);
        }

    /**
     * Check whether or not the specified ByteSequence is a representation of an
     * IntDecoratedObject.
     *
     * @param buf  the ByteSequence
     *
     * @return true iff the buffer contains (starts with) a representation of an
     *         IntDecoratedObject
     */
    public static boolean isIntDecorated(ByteSequence buf)
        {
        try
            {
            return buf.byteAt(0) == FMT_IDO;
            }
        catch (IndexOutOfBoundsException e)
            {
            return false;
            }
        }

    /**
     * Extract a decoration value from the specified ReadBuffer that contains a
     * representation of an IntDecoratedObject.
     *
     * @param buf  the ReadBuffer
     *
     * @return the integer decoration value
     */
    public static int extractIntDecoration(ReadBuffer buf)
        {
        if (buf instanceof HashEncoded)
            {
            return ((HashEncoded) buf).getEncodedHash();
            }

        try
            {
            DataInput in = buf.getBufferInput();
            in.readUnsignedByte(); // skip the type
            return readInt(in);
            }
        catch (IOException e)
            {
            throw new IllegalArgumentException("invalid binary");
            }
        }

    /**
     * Remove a decoration value from the specified Binary that contains a
     * representation of an IntDecoratedObject.
     *
     * @param bin  the Binary object
     *
     * @return the undecorated Binary value
     *
     * @deprecated as of Coherence 3.7.2
     */
    public static Binary removeIntDecoration(Binary bin)
        {
        // Note: "upcast" is necessary in order to bind to the correct method
        return asBinary(removeIntDecoration((ReadBuffer) bin));
        }

    /**
     * Remove a decoration value from the specified ReadBuffer that contains
     * a representation of an IntDecoratedObject.
     *
     * @param buf the ReadBuffer
     *
     * @return the undecorated ReadBuffer
     */
    public static ReadBuffer removeIntDecoration(ReadBuffer buf)
        {
        try
            {
            BufferInput in = buf.getBufferInput();
            in.readUnsignedByte(); // skip the type
            readInt(in);           // skip the int decoration
            int of = in.getOffset();
            return buf.getReadBuffer(of, buf.length() - of);
            }
        catch (IOException e)
            {
            throw new IllegalArgumentException("invalid binary");
            }
        }

    /**
     * Read a char array.
     *
     * @param in  a DataInput stream to read from
     *
     * @return a char array value
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static char[] readCharArray(DataInput in)
            throws IOException
        {
        int cch = in.readInt();

        validateLoadArray(char[].class, cch, in);

        Utf8Reader reader = new Utf8Reader((InputStream) in);

        return cch < CHUNK_THRESHOLD >> 1
                ? readCharArray(reader, cch)
                : readLargeCharArray(reader, cch);
        }

    /**
     * Read an array of long numbers from a DataInput stream that use
     * fixed-length 8-byte Big Endian binary format.
     *
     * @param in  a DataInput stream to read from
     *
     * @return an array of longs
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static long[] readLongArray(DataInput in)
            throws IOException
        {
        int c = in.readInt();

        validateLoadArray(long[].class, c, in);
        return c <= 0 ? new long[0] :
                    c < CHUNK_THRESHOLD >> 3
                            ? readLongArray(in, c)
                            : readLargeLongArray(in, c);
        }

    /**
     * Write an array of long numbers to a DataOutput stream.
     *
     * @param out  a DataOutput stream to write to
     * @param al   an array of longs to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value is passed
     *
     * @since 24.09
     */
    public static void writeLongArray(DataOutput out, long[] al)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(al);
            }
        else
            {
            int c = al.length;
            out.writeInt(c);

            for (long l : al)
                {
                out.writeLong(l);
                }
            }
        }

    /**
     * Read an array of int numbers from a DataInput stream which
     * use fixed-length 4-byte Big Endian binary format.
     *
     * @param in  a DataInput stream to read from
     *
     * @return an array of ints
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static int[] readIntArray(DataInput in)
            throws IOException
        {
        int c = in.readInt();

        validateLoadArray(int[].class, c, in);
        return c <= 0 ?  new int[0] :
                    c < CHUNK_THRESHOLD >> 2
                        ? readIntArray(in, c)
                        : readLargeIntArray(in, c);
        }

    /**
     * Write an array of integer numbers to a DataOutput stream.
     *
     * @param out  a DataOutput stream to write to
     * @param ai   an array of ints to write
     *
     * @throws IOException           if an I/O exception occurs
     * @throws NullPointerException  if null value is passed
     *
     * @since 24.09
     */
    public static void writeIntArray(DataOutput out, int[] ai)
            throws IOException
        {
        if (out instanceof PofOutputStream)
            {
            ((PofOutputStream) out).writeObject(ai);
            }
        else
            {
            int c = ai.length;
            out.writeInt(c);

            for (int i : ai)
                {
                out.writeInt(i);
                }
            }
        }

    /**
     * Read an array of object from a DataInput stream.
     *
     * @param in  a DataInput stream to read from
     *
     * @return an array of object
     *
     * @throws IOException  if an I/O exception occurs
     */
    public static Object[] readObjectArray(DataInput in)
            throws IOException
        {
        int c = in.readInt();

        validateLoadArray(Object[].class, c, in);
        return c <= 0 ? new Object[0] :
                    c < CHUNK_THRESHOLD >> 4
                        ? readObjectArray(in, c)
                        : readLargeObjectArray(in, c);
        }

    /**
     * Return true if the provided class is allowed to be deserialized.
     *
     * @param clz  the class to be checked
     * @param ois  the ObjectInputStream
     *
     * @return true if the provided class is allowed to be deserialized
     */
    protected static boolean checkObjectInputFilter(Class<?> clz, ObjectInputStream ois)
        {
        return checkObjectInputFilter(clz, -1, (DataInput) ois);
        }

    /**
     * Return true if the provided class is allowed to be deserialized.
     *
     * @param clz  the class to be checked
     * @param in   input context containing ObjectInputFilter
     *
     * @return true if the provided class is allowed to be deserialized from <code>in</code>
     */
    protected static boolean checkObjectInputFilter(Class<?> clz, DataInput in)
        {
        return checkObjectInputFilter(clz, -1, in);
        }

    /**
     * Return true if the provided class is allowed to be deserialized.
     *
     * @param clz      the class to be checked
     * @param cLength  array length to be checked
     * @param in       input context containing ObjectInputFilter
     *
     * @return true if the provided class is allowed to be deserialized from <code>in</code>
     */
    protected static boolean checkObjectInputFilter(Class<?> clz,  int cLength, DataInput in)
        {
        Object oFilter = getObjectInputFilter(in);

        try
            {
            if (oFilter == null)
                {
                return true;
                }

            DynamicFilterInfo dynamic = s_tloHandler.get();
            dynamic.setClass(clz);
            dynamic.setArrayLength(cLength);

            Object oFilterInfo = dynamic.getFilterInfo();

            Enum status = (Enum) HANDLE_CHECKINPUT.invoke(oFilter, oFilterInfo);

            dynamic.setClass(null);
            dynamic.setArrayLength(-1);

            // TODO: we would like this to be programmatically enabled/disabled thus will
            //       introduce a mechanism to do so which will remove the necessity for a single JVM arg.
            if (SERIAL_FILTER_LOGGING)
                {
                // similar to ObjectInputStream serialfilter logging,
                // only log status ACCEPTED(1)/REJECTED(2) when logging enabled
                // and FINE logging, UNDECIDED(0) is logged at FINER
                Logger.log(() -> String.format("ExternalizableHelper checkInput %-9s %s, array length: %s",
                                               status, clz, cLength),
                           status.ordinal() > 0 ? Logger.FINE : Logger.FINER);
                }

            return !status.name().equals("REJECTED");
            }
        catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException e)
            {
            err("Unable to invoke checkInput on " + oFilter.getClass().getName() +
                " due to exception " + e.getClass().getName() + " : " + e.getMessage());
            }
        catch (Throwable t) {}

        return false;
        }

    /**
     * Return ObjectInputFilter associated with {@link DataInput}.
     *
     * @param in  DataInput that may or may not have a ObjectInputFilter associated with it
     *
     * @return ObjectInputFilter associated with {@link DataInput in} or null when one does not exist
     */
    protected static Object getObjectInputFilter(DataInput in)
        {
        try
            {
            while (in instanceof WrapperDataInputStream)
                {
                in = ((WrapperDataInputStream) in).getDataInput();
                }

            return in instanceof BufferInput
                       ? ((BufferInput) in).getObjectInputFilter()
                       : HANDLE_GET_FILTER != null && in instanceof ObjectInputStream
                           ? HANDLE_GET_FILTER.invoke((ObjectInputStream) in)
                           : null;
            }
        catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException e)
            {
            err("Unable to invoke method handle " + HANDLE_GET_FILTER + " on " + in.getClass().getName() +
                " due to exception " + e.getClass().getName() + ": " + e.getMessage());
            }
        catch (Throwable t) {}

        return null;
        }

    /**
     * Return the static JVM-wide serial filter or {@code null} if not configured.
     *
     * @return ObjectInputFilter as an Object to enable working with Java versions before 9 or
     *         null if no filter has been configured.
     */
    public static Object getConfigSerialFilter()
        {
        Object oFilter = m_oFilterSerial;

        if (oFilter != null || HANDLE_CONFIG_GET_FILTER == null)
            {
            return oFilter;
            }
        else
            {
            try
                {
                oFilter = HANDLE_CONFIG_GET_FILTER.invoke();
                if (oFilter != null)
                    {
                    synchronized (HANDLE_CONFIG_GET_FILTER)
                        {
                        if (m_oFilterSerial == null)
                            {
                            m_oFilterSerial = oFilter;
                            }
                        }
                    if (SERIAL_FILTER_LOGGING)
                        {
                        Logger.info("JVM wide ObjectInputFilter=" + oFilter);
                        }
                    }
                return oFilter;
                }
            catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException e)
                {
                err("Unable to invoke getSerialFilter on ObjectInputFilter$Config" +
                    " due to exception " + e.getClass().getName() + ": " + e.getMessage());
                }
            catch (Throwable t)
                {}
            }
        return null;
        }

    /**
     * Return the static JVM-wide serial filter factory.
     *
     * @return deserialization filter factory for Java version 17 and greater, null otherwise.
     */
    public static BinaryOperator getConfigSerialFilterFactory()
        {
        BinaryOperator factory = m_serialFilterFactory;

        if (HANDLE_CONFIG_GET_FILTER_FACTORY == null || factory != null)
            {
            return factory;
            }
        else
            {
            try
                {
                factory = (BinaryOperator) HANDLE_CONFIG_GET_FILTER_FACTORY.invoke();

                synchronized (HANDLE_CONFIG_GET_FILTER_FACTORY)
                    {
                    if (m_serialFilterFactory == null)
                        {
                        m_serialFilterFactory = factory;
                        }
                    }

                return factory;
                }
            catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | IllegalStateException e)
                {
                err("Unable to invoke getSerialFilterFactory on ObjectInputFilter$Config" +
                    " due to exception " + e.getClass().getName() + ": " + e.getMessage());
                }
            catch (Throwable t)
                {
                }
            }
        return null;
        }

    /**
     * Read an array of the specified number of int from a DataInput stream.
     *
     * @param in  a DataInput stream to read from
     * @param c   length to read
     *
     * @return an array of ints
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static int[] readIntArray(DataInput in, int c)
            throws IOException
        {
        int[] ai = new int[c];
        for (int i = 0; i < c; i++)
            {
            ai[i] = in.readInt();
            }

        return ai;
        }

    /**
     * Read an array of ints with length larger than {@link #CHUNK_THRESHOLD} {@literal >>} 2.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  length to read
     *
     * @return an array of ints
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static int[] readLargeIntArray(DataInput in, int cLength)
            throws IOException
        {
        int    cBatchMax = CHUNK_SIZE >> 2;
        int    cBatch    = cLength / cBatchMax + 1;
        int[]  aMerged   = null;
        int    cRead     = 0;
        int    cAllocate = cBatchMax;
        int[]  ai;

        for (int i = 0; i < cBatch && cRead < cLength; i++)
            {
            ai      = readIntArray(in, cAllocate);
            aMerged = mergeIntArray(aMerged, ai);
            cRead  += ai.length;

            cAllocate = Math.min(cLength - cRead, cBatchMax);
            }

        return aMerged;
        }

    /**
     * Read an array of the specified number of object from a DataInput stream.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  length to read
     *
     * @return an array of objects
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static Object[] readObjectArray(DataInput in, int cLength)
            throws IOException
        {
        Object[] ao = new Object[cLength];
        for (int i = 0; i < cLength; i++)
            {
            ao[i] = readObject(in);
            }

        return ao;
        }

    /**
     * Read an array of objects with length larger than {@link #CHUNK_THRESHOLD} {@literal >>} 4.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  length to read
     *
     * @return an array of objects
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static Object[] readLargeObjectArray(DataInput in, int cLength)
            throws IOException
        {
        int      cBatchMax = CHUNK_SIZE >> 4;
        int      cBatch    = cLength / cBatchMax + 1;
        Object[] aMerged   = null;
        int      cRead     = 0;
        int      cAllocate = cBatchMax;

        Object[] ao;
        for (int i = 0; i < cBatch && cRead < cLength; i++)
            {
            ao      = readObjectArray(in, cAllocate);
            aMerged = mergeArray(aMerged, ao);
            cRead  += ao.length;

            cAllocate = Math.min(cLength - cRead, cBatchMax);
            }

        return aMerged;
        }

    /**
     * Read an array of the specified number of longs from a DataInput stream.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  length to read
     *
     * @return an array of longs
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static long[] readLongArray(DataInput in, int cLength)
            throws IOException
        {
        long[] al = new long[cLength];
        for (int i = 0; i < cLength; i++)
            {
            al[i] = in.readLong();
            }

        return al;
        }

    /**
     * Read an array of longs with length larger than {@link #CHUNK_THRESHOLD} {@literal >>} 3.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  length to read
     *
     * @return an array of longs
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static long[] readLargeLongArray(DataInput in, int cLength)
            throws IOException
        {
        int    cBatchMax = CHUNK_SIZE >> 3;
        int    cBatch    = cLength / cBatchMax + 1;
        long[] aMerged   = null;
        int    cRead     = 0;
        int    cAllocate = cBatchMax;
        long[] al;
        for (int i = 0; i < cBatch && cRead < cLength; i++)
            {
            al      = readLongArray(in, cAllocate);
            aMerged = mergeLongArray(aMerged, al);
            cRead  += al.length;

            cAllocate = Math.min(cLength - cRead, cBatchMax);
            }

        return aMerged;
        }

    /**
     * Read an array of char for the specified length from the reader.
     *
     * @param reader   the Utf8Reader to read from
     * @param cLength  the length to read
     */
    protected static char[] readCharArray(Utf8Reader reader, int cLength)
            throws IOException
        {
        int of = 0;
        char[] ach = new char[cLength];
        while (of < cLength)
            {
            int cchBlock = reader.read(ach, of,cLength - of);
            if (cchBlock < 0)
                {
                throw new EOFException();
                }
            else
                {
                of += cchBlock;
                }
            }
        return ach;
        }

    /**
     * Read an array of char for the specified length from the reader.
     *
     * @param reader   the Utf8Reader to read from
     * @param cLength  the length to read
     */
    protected static char[] readLargeCharArray(Utf8Reader reader, int cLength)
            throws IOException
        {
        int    cBatchMax = CHUNK_SIZE >> 1;
        int    cBatch    = cLength / cBatchMax + 1;
        char[] aMerged   = null;
        int    cRead     = 0;
        int    cAllocate = cBatchMax;
        char[] ach;
        for (int i = 0; i < cBatch && cRead < cLength; i++)
            {
            ach     = readCharArray(reader, cAllocate);
            aMerged = mergeCharArray(aMerged, ach);
            cRead  += ach.length;

            cAllocate = Math.min(cLength - cRead, cBatchMax);
            }

        return aMerged;
        }

    /**
     * Read byte array with length larger than {@link #CHUNK_THRESHOLD}.
     *
     * @param in  a DataInput stream to read from
     * @param cb  number of bytes to read
     *
     * @return a read byte array value
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static byte[] readLargeByteArray(DataInput in, int cb)
            throws IOException
        {
        int    cbBatchMax = CHUNK_SIZE;
        int    cBatch     = cb / cbBatchMax + 1;

        try (MultiBufferWriteBuffer buf  = new MultiBufferWriteBuffer(new BufferManagerWriteBufferPool(BufferManagers.getHeapManager()), cbBatchMax))
            {
            InputStreaming input = new WrapperDataInputStream(in);
            int cbRead = 0;
            for (int i = 0; i < cBatch && cbRead < cb; i++)
                {
                int cbBatch = Math.min(cb - cbRead, cbBatchMax);
                buf.write(cbRead, input, cbBatch);
                cbRead += cbBatch;
                }
            return buf.toByteArray();
            }
        }

    /**
     * Read the specified number of booleans from a boolean array.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  the length to read
     *
     * @return a boolean array value
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static boolean[] readBooleanArray(DataInput in, int cLength)
            throws IOException
        {
        boolean[] af = new boolean[cLength];
        for (int of = 0, cb = (cLength + 7) / 8, i = 0; of < cb; ++of)
            {
            int nBits = in.readUnsignedByte();
            for (int nMask = 1; i < cLength && nMask <= 0xFF; nMask <<= 1)
                {
                af[i++] = (nBits & nMask) != 0;
                }
            }
        return af;
        }

    /**
     * Read a boolean array with length larger than {@link #CHUNK_THRESHOLD}.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  length to read
     *
     * @return the read boolean array
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static boolean[] readLargeBooleanArray(DataInput in, int cLength)
            throws IOException
        {
        int       cBatchMax = CHUNK_SIZE & ~0x7;
        int       cBatch    = cLength / cBatchMax + 1;
        int       cRead     = 0;
        int       cAllocate = cBatchMax;
        boolean[] aMerged   = null;
        boolean[] af;
        for (int i = 0; i < cBatch && cRead < cLength; i++)
            {
            af       = readBooleanArray(in, cAllocate);
            aMerged = mergeBooleanArray(aMerged, af);
            cRead   += af.length;

            cAllocate = Math.min(cLength - cRead, cBatchMax);
            }

        return aMerged;
        }

    /**
     * Read an array of the specified number of floats from a DataInput stream.
     *
     * @param in   a DataInput stream to read from
     * @param cfl  the length to read
     *
     * @return an array of floats
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static float[] readFloatArray(DataInput in, int cfl)
            throws IOException
        {
        byte[] ab = new byte[cfl << 2];
        in.readFully(ab);

        float[] afl = new float[cfl];
        for (int i = 0, of = 0; i < cfl; i++)
            {
            // Unfortunately we cannot win this battle:
            // the standard serialization goes native and does not
            // do any conversion at all:
            //
            // ival = ((bytes[srcpos + 0] & 0xFF) << 24) +
            //        ((bytes[srcpos + 1] & 0xFF) << 16) +
            //        ((bytes[srcpos + 2] & 0xFF) << 8) +
            //        ((bytes[srcpos + 3] & 0xFF) << 0);
            //  u.i = (long) ival;
            //  floats[dstpos] = (jfloat) u.f;

            int iValue =
                    ((ab[of++] & 0xff) << 24) +
                            ((ab[of++] & 0xff) << 16) +
                            ((ab[of++] & 0xff) <<  8) +
                            ((ab[of++] & 0xff));

            afl[i] = Float.intBitsToFloat(iValue);
            }

        return afl;
        }

    /**
     * Read a float array with length larger than {@link #CHUNK_THRESHOLD} {@literal >>} 2.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  length to read
     *
     * @return the read float array value
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static float[] readLargeFloatArray(DataInput in, int cLength)
            throws IOException
        {
        int      cBatchMax = CHUNK_SIZE >> 2;
        int      cBatch    = cLength / cBatchMax + 1;
        float[]  aflMerged = null;
        int      cRead     = 0;
        int      cAllocate = cBatchMax;
        float[]  afl;
        for (int i = 0; i < cBatch && cRead < cLength; i++)
            {
            afl       = readFloatArray(in, cAllocate);
            aflMerged = mergeFloatArray(aflMerged, afl);
            cRead    += afl.length;

            cAllocate = Math.min(cLength - cRead, cBatchMax);
            }

        return aflMerged;
        }

    /**
     * Read an array of the specified number of doubles from a DataInput stream.
     *
     * @param in    a DataInput stream to read from
     * @param cdfl  length to read
     *
     * @return an array of doubles
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static double[] readDoubleArray(DataInput in, int cdfl)
            throws IOException
        {
        byte[] ab = new byte[cdfl << 3];
        in.readFully(ab);
        double[] adfl = new double[cdfl];
        for (int i = 0, of = 0; i < cdfl; i++)
            {
            int iUpper =
                    ((ab[of++] & 0xff) << 24) +
                            ((ab[of++] & 0xff) << 16) +
                            ((ab[of++] & 0xff) << 8) +
                            ((ab[of++] & 0xff));
            int iLower =
                    ((ab[of++] & 0xff) << 24) +
                            ((ab[of++] & 0xff) << 16) +
                            ((ab[of++] & 0xff) << 8) +
                            ((ab[of++] & 0xff));

            adfl[i] = Double.longBitsToDouble(
                    (((long) iUpper) << 32) + (iLower & 0xFFFFFFFFL));
            }

        return adfl;
        }

    /**
     * Read a double array with length larger than {@link #CHUNK_THRESHOLD} {@literal >>} 3.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  the length to read
     *
     * @return an array of doubles
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static double[] readLargeDoubleArray(DataInput in, int cLength)
            throws IOException
        {
        int      cBatchMax  = CHUNK_SIZE >> 3;
        int      cBatch     = cLength / cBatchMax + 1;
        int      cAllocate  = cBatchMax;
        double[] adflMerged = null;
        int      cdflRead   = 0;
        double[] adfl;
        for (int i = 0; i < cBatch && cdflRead < cLength; i++)
            {
            adfl       = readDoubleArray(in, cAllocate);
            adflMerged = mergeDoubleArray(adflMerged, adfl);
            cdflRead  += adfl.length;

            cAllocate = Math.min(cLength - cdflRead, cBatchMax);
            }

        return adflMerged;
        }

    /**
     * Read array of string for the specified size.
     *
     * @param in  a DataInput stream to read from
     * @param c   length to read
     *
     * @return the read string array value
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static String[] readStringArray(DataInput in, int c)
            throws IOException
        {
        String[] as = new String[c];
        for (int i = 0; i < c; ++i)
            {
            as[i] = readSafeUTF(in);
            }
        return as;
        }

    /**
     * Read array of string with length larger than threshold {@link #CHUNK_THRESHOLD} {@literal >>} 3.
     *
     * @param in  a DataInput stream to read from
     * @param c   length to read
     *
     * @return the read string array value
     *
     * @throws IOException  if an I/O exception occurs
     */
    protected static String[] readLargeStringArray(DataInput in, int c)
            throws IOException
        {
        int      cBatchMax = CHUNK_SIZE >> 3;
        int      cBatch    = c / cBatchMax + 1;
        int      cRead     = 0;
        int      cAllocate = cBatchMax;
        String[] asMerged  = null;
        String[] as;
        for (int i = 0; i < cBatch && cRead < c; i++)
            {
            as       = readStringArray(in, cAllocate);
            asMerged = mergeArray(asMerged, as);
            cRead   += as.length;

            cAllocate = Math.min(c - cRead, cBatchMax);
            }

        return asMerged;
        }

    /**
     * Return class for the specified class name; null if not found.
     *
     * @param sClass  the class name
     *
     * @return the class for the specified class name
     */
    public static Class getClass(String sClass)
        {
        try
            {
            return Class.forName(sClass);
            }
        catch (ClassNotFoundException cnfe)
            {
            return null;
            }
        }

    /**
     * Integer decorated object.
     */
    protected static final class IntDecoratedObject
            extends    ExternalizableHelper
            implements Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct an IntDecorated object with the specified value and decoration.
         *
         * @param oValue       the value to decorate
         * @param nDecoration  the int decoration
         */
        public IntDecoratedObject(Object oValue, int nDecoration)
            {
            azzert(!(oValue instanceof IntDecoratedObject));

            m_oValue      = oValue;
            m_nDecoration = nDecoration;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the underlying value.
         *
         * @return the underlying value
         */
        public Object getValue()
            {
            return m_oValue;
            }

        /**
         * Return the int decoration.
         *
         * @return the int decoration
         */
        public int getDecoration()
            {
            return m_nDecoration;
            }

        // ----- Serializable interface -------------------------------------

        /**
         * Psuedo-interface. Standard serialization is not allowed.
         */
        private void writeObject(ObjectOutputStream stream)
                throws IOException
            {
            throw new IOException("not allowed");
            }

        /**
         * Psuedo-interface. Standard de-serialization is not allowed.
         */
        private void readObject(ObjectInputStream stream)
                throws IOException, ClassNotFoundException
            {
            throw new IOException("not allowed");
            }

        // ----- data members -----------------------------------------------

        /**
         * The decorated (original) object value.
         */
        private Object m_oValue;

        /**
         * The decoration integer value.
         */
        private int    m_nDecoration;
        }


    // ----- XmlBean class caching ------------------------------------------

    /**
     * An interface for caching class reference by integer ID values.
     */
    public interface XmlBeanClassCache
        {
        /**
         * Initialize the XmlBeanClassCache.
         *
         * @param xml  the XML configuration for the cache
         */
        public void init(XmlElement xml);

        /**
         * Look up the class ID for the specified class.
         *
         * @param clz  the class to look up
         *
         * @return the ID if the class is known; otherwise -1
         */
        public int getClassId(Class clz);

        /**
         * Look up the class for the specified ID.
         *
         * @param nId     the class ID
         * @param loader  the ClassLoader for the class to load
         *
         * @return the class for that class ID
         */
        public Class getClass(int nId, ClassLoader loader);
        }

    /**
     * An implementation XmlBeanClassCache that uses a pre-defined list of
     * XmlBean implementations.
     */
    public static class SimpleXmlBeanClassCache
            extends Base
            implements XmlBeanClassCache
        {
        // ----- XmlBeanClassCache methods ----------------------------------

        /**
         * Initialize the XmlBeanClassCache.
         *
         * @param xml  the XML configuration for the cache
         */
        public void init(XmlElement xml)
            {
            // build list of classes
            List list = new ArrayList();
            for (Iterator iter = xml.getSafeElement("xmlbean-list")
                    .getElements("xmlbean-class"); iter.hasNext(); )
                {
                XmlElement xmlClassName = (XmlElement) iter.next();
                String     sClass       = xmlClassName.getString(null);
                list.add(sClass == null ? null : sClass.intern());
                }
            m_asBeans = (String[]) list.toArray(new String[list.size()]);

            // determine whether or not this implementation needs to be
            // ClassLoader-aware
            if (xml.getSafeElement("classloader-aware").getBoolean())
                {
                m_fAware = true;

                // note that the large load factor will guarantee that the
                // map will never resize
                m_mapBeanClasses = new WeakHashMap(101, 1000.0F);
                }
            }

        /**
         * Look up the class ID for the specified class.
         *
         * @param clz  the class to look up
         *
         * @return the ID if the class is known; otherwise -1
         */
        public int getClassId(Class clz)
            {
            String sName = clz.getName();
            String[] asBeans = m_asBeans;
            for (int i = 0, c = asBeans.length; i < c; ++i)
                {
                if (equals(asBeans[i], sName))
                    {
                    return i;
                    }
                }
            return -1;
            }

        /**
         * Look up the class for the specified ID.
         *
         * @param nId     the class ID
         * @param loader  the ClassLoader for the class to load
         *
         * @return the class for that class ID
         */
        public Class getClass(int nId, ClassLoader loader)
            {
            Class clz = null;

            if (m_fAware)
                {
                WeakReference[] aref = (WeakReference[]) m_mapBeanClasses.get(loader);
                if (aref == null)
                    {
                    aref = initClassLoader(loader);
                    }

                try
                    {
                    clz = (Class) aref[nId].get();
                    }
                catch (NullPointerException e)
                    {
                    }
                }
            else
                {
                Class[] aclz = m_aclzBeans;
                if (aclz == null)
                    {
                    m_aclzBeans = aclz = initClasses(loader);
                    }

                try
                    {
                    clz = aclz[nId];
                    }
                catch (IndexOutOfBoundsException e)
                    {
                    }
                }

            if (clz == null)
                {
                if (nId < 0)
                    {
                    throw new IndexOutOfBoundsException("Class ID=" + nId
                        + "; a negative XmlBean ID is used to indicate"
                        + " an \"unknown\" XmlBean class");
                    }
                else
                    {
                    throw new IndexOutOfBoundsException("Class ID=" + nId
                        + ", Max ID=" + (m_aclzBeans.length - 1));
                    }
                }

            return clz;
            }

        // ----- internal ---------------------------------------------------

        /**
         * Given the specified ClassLoader, make sure that the XmlBean
         * classes have all been looked up within that ClassLoader.
         *
         * @param loader  the ClassLoader to use to look up the XmlBean
         *                classes
         *
         * @return an array of WeakReferences to Classes, one for each
         *         specified XmlBean
         */
        private WeakReference[] initClassLoader(ClassLoader loader)
            {
            azzert(m_fAware);

            Class[]         aclz = initClasses(loader);
            int             c    = aclz.length;
            WeakReference[] aref = new WeakReference[c];
            for (int i = 0; i < c; ++i)
                {
                aref[i] = new WeakReference(aclz[i]);
                }

            Map map = m_mapBeanClasses;
            synchronized (map)
                {
                map.put(loader, aref);
                }

            return aref;
            }

        /**
         * Given the specified ClassLoader, load all the XmlBean classes
         * that have been specified to be serialization-optimized.
         *
         * @param loader  the ClassLoader to load for
         *
         * @return an array of classes, one for each specified XmlBean
         */
        private Class[] initClasses(ClassLoader loader)
            {
            String[] asBeans = m_asBeans;
            int      cBeans  = asBeans.length;
            Class[]  aclz    = new Class[cBeans];

            for (int i = 0; i < cBeans; ++i)
                {
                String sBean = asBeans[i];

                if (sBean != null && sBean.length() > 0)
                    {
                    try
                        {
                        aclz[i] = loadClass(sBean, loader, null);
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw ensureRuntimeException(e);
                        }
                    }
                }

            return aclz;
            }

        // ----- data members -----------------------------------------------

        /**
         * XmlBean class cache is ClassLoader aware or not.
         */
        private boolean m_fAware;

        /**
         * Array of class names.
         */
        private String[] m_asBeans;

        /**
         * Array of classes. This array is only used if the implementation is
         * ClassLoader un-aware.
         */
        private Class[] m_aclzBeans;

        /**
         * Map of arrays of WeakReferences to Classes, keyed by ClassLoader.
         */
        private WeakHashMap m_mapBeanClasses;
        }

    /**
     * DynamicFilterInfo is an InvocationHandler that has association with
     * a single proxy instance.
     */
    private static class DynamicFilterInfo
            implements InvocationHandler
        {
        // ----- DynamicFilterInfo methods ----------------------------------

        public DynamicFilterInfo() {};

        /**
         * Return the proxy instance.
         *
         * @return the proxy instance
         */
        public Object getFilterInfo()
            {
            Object oProxy = m_oProxy;
            if (oProxy == null)
                {
                oProxy = m_oProxy = Proxy.newProxyInstance(getContextClassLoader(),
                        new Class[]{s_clzFilterInfo}, this);
                }
            return oProxy;
            }

        /**
         * Set the class to be checked.
         *
         * @param clz  the class to check
         */
        public void setClass(Class clz)
            {
            m_clz = clz;
            }

        /**
         * Set the array length to be checked
         *
         * @param cLength  array length to check, -1 indicates not an array
         */
        public void setArrayLength(int cLength)
            {
            m_cArrayLength = cLength;
            }

        // ----- InvocationHandler interface --------------------------------

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
            if (method.getName().equals("serialClass"))
                {
                return m_clz;
                }
            else if (method.getName().equals("arrayLength"))
                {
                return (long) m_cArrayLength;
                }
            else if (method.getName().equals("depth"))
                {
                return 1L;
                }
            else if (method.getName().equals("references"))
                {
                return 0L;
                }
            else if (method.getName().equals("streamBytes"))
                {
                return 0L;
                }
            return null;
            }

        // ----- data members -----------------------------------------------

        /**
         * The class to be checked by ObjectInputFilter.
         */
        private Class m_clz;

        /**
         * The array length, -1 indicates not an array.
         */
        private int   m_cArrayLength = -1;

        /**
         * The dynamic proxy.
         */
        private Object m_oProxy;
        }


    // ----- configurable options -------------------------------------------

    /**
     * The name of the system property that can be used to override the location
     * of the ExternalizableHelper configuration file.
     * <p>
     * The value of this property must be the name of a resource that contains
     * an XML document with the structure defined in the
     * <tt>/com/tangosol/util/ExternalizableHelper.xml</tt> configuration
     * descriptor.
     */
    public static final String PROPERTY_CONFIG = "coherence.externalizable.config";

    /**
     * Option: Always use a ResolvingObjectInputStream, even if a regular
     * ObjectInputStream is available.
     */
    public static final boolean FORCE_RESOLVING_STREAMS;

    /**
     * Option: Use an XmlBean class cache.
     */
    public static final boolean USE_XMLBEAN_CLASS_CACHE;

    /**
     * Option: XmlBean class cache implementation.
     */
    public static final XmlBeanClassCache XMLBEAN_CLASS_CACHE;

    /**
     * The total buffer allocation limit; zero for no limit.
     */
    private static final int MAX_BUFFER;

    /**
     * Option: Use POF as the default serialization format.
     */
    public static final boolean USE_POF_STREAMS;

    /**
     * Option: Use either <code>static</code> or <code>dynamic</code> lambda serialization.
     */
    public static final String LAMBDA_SERIALIZATION;

    /**
     * Option: Configurable ObjectStreamFactory.
     */
    public static ObjectStreamFactory s_streamfactory;

    /**
     * MethodHandle for getfilter from ObjectInputStream.
     */
    private static final MethodHandle HANDLE_GET_FILTER;

    /**
     * MethodHandle for checkInput from ObjectInputFilter.
     */
    private static final MethodHandle HANDLE_CHECKINPUT;

    /**
     * MethodHandle for method getSerialFilter from ObjectInputFilter$Config.
     */
    private static final MethodHandle HANDLE_CONFIG_GET_FILTER;


    /**
     * MethodHandle for method getSerialFilterFactory from ObjectInputFilter$Config.
     */
    private static final MethodHandle HANDLE_CONFIG_GET_FILTER_FACTORY;

    /**
     * Filter info class.
     */
    private static Class<?> s_clzFilterInfo = null;

    /**
     * The DynamicFilterInfo ThreadLocal.
     */
    private final static ThreadLocal<DynamicFilterInfo> s_tloHandler =
            ThreadLocal.withInitial(DynamicFilterInfo::new);


    static
        {
        boolean             fResolve = true;
        boolean             fCache   = false;
        XmlBeanClassCache   cache    = null;
        int                 cbMax    = 0;
        boolean             fPof     = false;
        String              sLambda  = "";
        ObjectStreamFactory factory  = null;

        try
            {
            XmlDocument xml = AccessController.doPrivileged(new PrivilegedAction<XmlDocument>()
                {
                public XmlDocument run()
                    {
                    String sConfig = Config.getProperty(PROPERTY_CONFIG);
                    XmlDocument xml = null;

                    if (sConfig != null && sConfig.length() > 0)
                        {
                        URL url = Resources.findResource(sConfig, null);
                        Throwable e = null;

                        if (url != null)
                            {
                            try
                                {
                                xml = XmlHelper.loadXml(url.openStream());
                                }
                            catch (Throwable t) {e = t;}
                            }

                        if (xml == null)
                            {
                            err("Unable to load ExternalizableHelper configuration file \"" + sConfig + "\";");
                            if (e != null)
                                {
                                err(e);
                                }
                            err("Using default configuration.");
                            }
                        }

                    if (xml == null)
                        {
                        xml = XmlHelper.loadXml(ExternalizableHelper.class, "ISO-8859-1");
                        }

                    XmlHelper.replaceSystemProperties(xml, "system-property");
                    return xml;
                    }
                });


            final XmlElement xmlFactory = xml.getSafeElement("object-stream-factory");

            factory = AccessController.doPrivileged(new PrivilegedAction<ObjectStreamFactory>()
                {
                public ObjectStreamFactory run()
                    {
                    try
                        {
                        if (!XmlHelper.isInstanceConfigEmpty(xmlFactory))
                            {
                            return (ObjectStreamFactory) XmlHelper.createInstance(
                                xmlFactory, ExternalizableHelper.class.getClassLoader(), null);
                            }
                        }
                    catch (Exception e)
                        {
                        err("Unable to instantiate an ObjectStreamFactory \"" + xmlFactory + "\":");
                        err(e);
                        }
                    return new DefaultObjectStreamFactory();
                    }
                });

            fResolve = xml.getSafeElement("force-classloader-resolving").getBoolean(fResolve);
            fCache   = xml.getSafeElement("enable-xmlbean-class-cache").getBoolean(fCache);

            if (fCache)
                {
                XmlElement xmlCfg = xml.getElement("xmlbean-class-cache-config");
                if (xmlCfg != null)
                    {
                    String sImpl = xml.getSafeElement("cache-class").getString(null);
                    try
                        {
                        if (sImpl == null)
                            {
                            cache = new SimpleXmlBeanClassCache();
                            }
                        else
                            {
                            cache = (XmlBeanClassCache) Class.forName(sImpl).newInstance();
                            }
                        cache.init(xmlCfg);
                        }
                    catch (Throwable e)
                        {
                        fCache = false;
                        err("Unable to instantiate and configure class cache \""
                                + sImpl + "\":");
                        err(e);
                        }
                    }
                }

            String sMax = xml.getSafeElement("serialization-maxbuffer").getString("0");
            cbMax = (int) parseMemorySize(sMax, POWER_0);

            fPof = xml.getSafeElement("enable-pof-serialization").getBoolean(fPof);

            sLambda = xml.getSafeElement("lambdas-serialization").getString("");
            }
        catch (Throwable e) {}

        FORCE_RESOLVING_STREAMS  = fResolve;
        USE_XMLBEAN_CLASS_CACHE  = fCache;
        XMLBEAN_CLASS_CACHE      = cache;
        MAX_BUFFER               = cbMax;
        USE_POF_STREAMS          = fPof;
        LAMBDA_SERIALIZATION     = sLambda;
        s_streamfactory          = factory;

        //  initialize method handles for potential JEP-290 checks

        Class<?>     clzFilterInfo                = null;
        MethodHandle handleGetFilter              = null;
        MethodHandle handleCheckInput             = null;
        MethodHandle handleConfigGetSerialFilter  = null;
        MethodHandle handleConfigGetFilterFactory = null;
        Method       methodConfigGetSerialFilter  = null;
        Method       methodConfigGetFilterFactory = null;


        // find ObjectInputFilter class; depending on jdk version
        try
            {
            Class<?>              clzFilter       = null;
            Class<? extends Enum> clzFilterStatus = null;
            Class<?>              clzConfig       = null;
            String                sFilterMethod   = null;
            Method                methodGet       = null;

            if ((clzFilter = getClass("java.io.ObjectInputFilter")) != null)
                {
                clzFilterInfo       = Class.forName("java.io.ObjectInputFilter$FilterInfo");
                clzFilterStatus     = (Class<? extends Enum>) Class.forName("java.io.ObjectInputFilter$Status");
                sFilterMethod       = "getObjectInputFilter";
                clzConfig           = Class.forName("java.io.ObjectInputFilter$Config");
                }
            else if ((clzFilter = getClass("sun.misc.ObjectInputFilter")) != null)
                {
                clzFilterInfo   = Class.forName("sun.misc.ObjectInputFilter$FilterInfo");
                clzFilterStatus = (Class<? extends Enum>) Class.forName("sun.misc.ObjectInputFilter$Status");
                sFilterMethod   = "getInternalObjectInputFilter";
                clzConfig       = Class.forName("sun.misc.ObjectInputFilter$Config");
                }

            if (sFilterMethod != null)
                {
                Class clzObjectInputStream = ObjectInputStream.class;

                methodGet = clzObjectInputStream.getDeclaredMethod(sFilterMethod);
                methodGet.setAccessible(true);

                methodConfigGetSerialFilter = clzConfig.getDeclaredMethod("getSerialFilter");
                methodConfigGetSerialFilter.setAccessible(true);

                try
                    {
                    methodConfigGetFilterFactory = clzConfig.getDeclaredMethod("getSerialFilterFactory");
                    }
                catch (NoSuchMethodException e)
                    {
                    // ignore when not defined in java version less than 17
                    }
                catch (Throwable t)
                    {
                    Logger.warn("Failed to find method ObjectInputFilter$Config.getSerialFilterFactory() in java version "
                                + System.getProperty("java.version") +  " due to: " + t.getMessage());
                    }

                MethodType mtCheckInput = MethodType.methodType(clzFilterStatus, clzFilterInfo);

                MethodHandles.Lookup lookup = MethodHandles.lookup();

                handleGetFilter              = lookup.unreflect(methodGet);
                handleCheckInput             = lookup.findVirtual(clzFilter, "checkInput", mtCheckInput);
                handleConfigGetSerialFilter  = lookup.unreflect(methodConfigGetSerialFilter);
                handleConfigGetFilterFactory = methodConfigGetFilterFactory == null ? null : lookup.unreflect(methodConfigGetFilterFactory);
                }
            }
        catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException e)
            {
            Logger.warn("ObjectInputFilter will not be honored due to: "
                    + e.getMessage() + '\n' + Base.printStackTrace(e));
            }

        s_clzFilterInfo                  = clzFilterInfo;
        HANDLE_GET_FILTER                = handleGetFilter;
        HANDLE_CHECKINPUT                = handleCheckInput;
        HANDLE_CONFIG_GET_FILTER         = handleConfigGetSerialFilter;
        HANDLE_CONFIG_GET_FILTER_FACTORY = handleConfigGetFilterFactory;
        }


    // ----- data members ---------------------------------------------------

    /**
     * A threshold used to decide whether to perform deserialization using a chunking
     * algorithm; default is greater than 128MB.
     */
    public static final int CHUNK_THRESHOLD = 0x7FFFFFF;

    /**
     * When using the chunking algorithm each chunk is constrained to 64MB by default.
     */
    public static final int CHUNK_SIZE = 0x3FFFFFF;

    /**
     * An array of Stats objects, indexed by the modulo of a swizzled class
     * hashcode.
     */
    private static final Stats[] s_astats = new Stats[6451];

    /**
     * WeakHashMap of Serializers, keyed by ClassLoader.
     */
    private static final Map<ClassLoader, Serializer> s_mapSerializerByClassLoader
            = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Enable tracing of {@link ExternalizableHelper#checkObjectInputFilter(Class, DataInput) ExternalizableLite serial filtering}.
     * Log level must by FINE or higher to get detailed tracing.
     */
    private static final boolean SERIAL_FILTER_LOGGING = Config.getBoolean("coherence.serialfilter.logging", false);

    /**
     * Cache of JVM-wide serial filter.
     */
    private static Object m_oFilterSerial;

    /**
     * Cache of JVM-wide serial filter factory.
     */
    private static BinaryOperator m_serialFilterFactory;

    /**
     * Flag to indicate whether this thread is in ExternalizableHelper initiated deserialization.
     * See {@link ExternalizableHelper#readSerializable(DataInput, ClassLoader)}.
     *
     * @since 12.2.1.4.23
     */
    public static ThreadLocal<Boolean> s_tloInEHDeserialize = ThreadLocal.withInitial(() -> Boolean.FALSE);
    }
