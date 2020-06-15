/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;

import com.oracle.coherence.common.base.Logger;
import java.io.IOException;

import java.lang.ref.WeakReference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Named;

/**
 * A {@link Serializer} implementation that multiplexes serialization/deserialization requests
 * across multiple {@link Serializer} implementations.
 * <p>
 * More specifically, for serialization operations, the list of available {@link Serializer}s are
 * iterated over.  The first serializer that is able to serialize the object without error, will be
 * the {@link Serializer} used for that type going forward.
 *
 * All serialized payloads will be prefixed with the following header to enable deserialization.
 *
 * <pre>
 *     +--------------+-------------------------+
 *     |  Length (4)  | Serializer Name (1...)  |
 *     +--------------+-------------------------+
 *     |  Serialized Payload (0...)             |
 *     +----------------------------------------+
 * </pre>
 *
 * Upon deserialization, the header will be read and the specified {@link Serializer} will be used
 * to deserialize the payload.  If the header is no present, an {@link IOException} will be thrown.
 *
 * @since 20.06
 */
@Named("Multiplexing")
public class MultiplexingSerializer
        implements Serializer, ClassLoaderAware
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new {@link Serializer} that will multiplex serialization operations
     * across the provided list of {@link Serializer}s.
     *
     * It's important to keep in mind that the iteration order when performing serialization/deserialization
     * operation is based on the order they are provided.  This may have bearing on ordering decisions.
     *
     * @throws IllegalArgumentException if no {@link Serializer}s are provided, or if the any of the provided
     *                                  {@link Serializer}s return {@code null} from {@link Serializer#getName()}
     */
    public MultiplexingSerializer(Serializer... serializers)
        {
        if (serializers == null || serializers.length == 0)
            {
            throw new IllegalArgumentException("At least one Serializer must be provided.");
            }

        f_serializers    = serializers.clone();
        f_idToSerializer = new HashMap<>(serializers.length);

        for (int i = 0, len = serializers.length; i < len; i++)
            {
            Serializer serializer = serializers[i];
            if (serializer.getName() == null)
                {
                String msg = "Serializer '%s' returned a null name";
                throw new IllegalArgumentException(String.format(msg, serializer.getClass().getName()));
                }
            f_idToSerializer.put(serializer.getName(), serializer);
            }
        }

    // ----- Serializer interface -------------------------------------------

    @Override
    public void serialize(WriteBuffer.BufferOutput out, Object o) throws IOException
        {
        doSerialization(out, o, f_serializers);
        }

    @Override
    public <T> T deserialize(ReadBuffer.BufferInput in, Class<? extends T> clazz) throws IOException
        {
        SerializationHeader serializationHeader = readSerializationHeader(in);

        if (serializationHeader == null)
            {
            throw new IOException("Unable to read or missing serialization header");
            }
        else
            {
            String     serializerName = serializationHeader.getSerializerName();
            Serializer serializer     = f_idToSerializer.get(serializerName);
            if (serializer == null)
                {
                throw new IOException(String.format("Unknown serializer '%s'", serializerName));
                }

            int cbPayload = serializationHeader.getLength();
            int nOrig     = in.getOffset();

            // It may be the case that there are multiple serialized objects in a given buffer.
            // It was found that some serializers will continue reading from the buffer after
            // deserializing an object.  So to deal with that, we create a subsequence of the larger
            // buffer that contains a single entity to deserialize.
            ReadBuffer buffSub = in.getBuffer().getReadBuffer(nOrig, cbPayload);
            try
                {
                return serializer.deserialize(buffSub.getBufferInput(), clazz);
                }
            finally
                {
                in.setOffset(nOrig + cbPayload);
                }
            }
        }

    // ----- ClassLoaderAware interface -------------------------------------

    @Override
    public ClassLoader getContextClassLoader()
        {
        WeakReference<ClassLoader> refLoader = m_refLoader;
        return refLoader == null ? null : refLoader.get();
        }

    @Override
    public void setContextClassLoader(ClassLoader loader)
        {
        assert m_refLoader == null;

        if (loader != null)
            {
            m_refLoader = new WeakReference<>(loader);

            for (int i = 0, len = f_serializers.length; i < len; i++)
                {
                Serializer s = f_serializers[i];
                if (s instanceof ClassLoaderAware)
                    {
                    ((ClassLoaderAware) s).setContextClassLoader(loader);
                    }
                }
            }
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return getName() + ", " + getClass().getName()
               + "{loader=" + getContextClassLoader()
               + ", {serializers=" + Arrays.toString(f_serializers) + '}';
        }

    // ----- helper methods -------------------------------------------------

    /**
     * See {@link Serializer#serialize(WriteBuffer.BufferOutput, Object)}
     *
     * @param out          the {@link WriteBuffer.BufferOutput} to write to
     * @param o            the object to serialize
     * @param serializers  the {@link Serializer}s to attempt the serialization operation against
     *
     * @throws IOException if an error occurs during serialization
     */
    protected void doSerialization(WriteBuffer.BufferOutput out,
                                   Object o,
                                   Serializer[] serializers) throws IOException
        {
        String     type       = o == null ? NULL_TYPE : o.getClass().getName();
        Serializer serializer = f_mapTypeToSerializer.get(type);
        int        nStart     = out.getOffset();

        if (serializer == null)
            {
            // Track the errors that each Serializer produces in the situation where ALL
            // serializers fail.  When this occurs, dump the stack for each error, otherwise
            // if one serializer passes, we don't pollute the log with noise.
            Map<Serializer, Exception> mapError = null;

            for (Serializer s : f_serializers)
                {
                try
                    {
                    doWrite(out, o, s);

                    if (f_mapTypeToSerializer.put(type, s) == null)
                        {
                        Logger.fine(() -> String.format("Using serializer '%s' for type '%s'", s.getName(), type));
                        }
                    return;
                    }
                catch (Exception e)
                    {
                    if (mapError == null)
                        {
                        mapError = new LinkedHashMap<>(f_serializers.length);
                        }

                    mapError.put(s, e);

                    out.setOffset(nStart);

                    Logger.fine(() ->
                        {
                        String rawMsg = "Unable to serialize type '%s' using serializer '%s'.  This may be expected.";
                        return String.format(rawMsg, type, s.getName());
                        });
                    }
                }

            logSerializationErrors(mapError, type);
            throw new IOException(String.format("No valid serializer available for type '%s'", type));
            }
        else
            {
            try
                {
                doWrite(out, o, serializer);
                }
            catch (IOException ioe)
                {
                Logger.fine(() -> "Unexpected exception raised using cached serializer.  Trying others...", ioe);
                f_mapTypeToSerializer.remove(type);
                out.setOffset(nStart);
                doSerialization(out, o, Arrays.stream(serializers).filter(s -> s != serializer).toArray(Serializer[]::new));
                }
            }
        }

    /**
     * Writes the serialization header and payload.
     *
     * @param out         the {@link WriteBuffer.BufferOutput} to write to
     * @param o           the object to serialize
     * @param serializer  the {@link Serializer} to use
     *
     * @throws IOException if an error occurs during serialization
     */
    protected void doWrite(WriteBuffer.BufferOutput out, Object o, Serializer serializer)
            throws IOException
        {
        int nStart   = out.getOffset();
        int nWritten = writeSerializationHeader(out, serializer.getName());

        serializer.serialize(out, o);

        int nAfterSer = out.getOffset();
        int nPayload  = nAfterSer - (nStart + nWritten);

        // now set the actual length; space previously reserved by writeSerializationHeader.
        out.setOffset(nStart);
        out.writeInt(nPayload);

        out.setOffset(nAfterSer);
        }

    /**
     * Given the provided mappings between a {@link Serializer} and the {@link Exception} it threw, log
     * each mapping for diagnostic purposes.
     *
     * @param serializerExceptionMap  a mapping between a {@link Serializer} instance and the {@link Exception}
     *                                it threw for any given serialization operation
     * @param typeName                The name of the type that produced the error.
     */
    protected void logSerializationErrors(Map<Serializer, Exception> serializerExceptionMap, String typeName)
        {
        assert serializerExceptionMap != null && !serializerExceptionMap.isEmpty();

        if (Logger.isEnabled(Logger.FINE))
            {
            String msg = "Unable to serialize/deserialize type '%s' using serializer '%s'.";
            for (Map.Entry<Serializer, Exception> entry : serializerExceptionMap.entrySet())
                {
                Logger.fine(String.format(msg, typeName, entry.getKey().getName()), entry.getValue());
                }
            }
        }

    /**
     * Write the multiplex serialization header.
     *
     * <pre>
     *     +--------------+-------------------------+
     *     |  Length (4)  | Serializer Name (1...)  |
     *     +--------------+-------------------------+
     *     |  Serialized Payload (0...)             |
     *     +----------------------------------------+
     * </pre>
     *
     * @param out             the stream to write to
     * @param serializerName  the name of the {@link Serializer}
     *
     * @throws IOException if an error occurs writing the header or payload
     *
     * @see Serializer#getName()
     */
    protected int writeSerializationHeader(WriteBuffer.BufferOutput out,
                                           String serializerName)
            throws IOException
        {
        int nOff = out.getOffset();
        out.setOffset(nOff + Integer.BYTES); // reserve space for the length
        out.writeUTF(serializerName);

        return out.getOffset() - nOff;
        }

    /**
     * Read the {@link SerializationHeader}.
     *
     * <pre>
     *     +--------------+-------------------------+
     *     |  Length (4)  | Serializer Name (1...)  |
     *     +--------------+-------------------------+
     *     |  Serialized Payload (0...)             |
     *     +----------------------------------------+
     * </pre>
     *
     * @param in  the stream to read from
     *
     * @return the SerializationHeader if present and a matching {@link Serializer} is found, otherwise
     *         returns {@code null}
     */
    protected SerializationHeader readSerializationHeader(ReadBuffer.BufferInput in)
        {
        int nOrig = in.getOffset();
        try
            {
            int cbPayload = in.readInt();
            if (cbPayload < 0)
                {
                in.setOffset(nOrig);
                return null;
                }

            String serializerName = in.readUTF();

            return new SerializationHeader(cbPayload, serializerName);
            }
        catch (IOException ioe)
            {
            in.setOffset(nOrig);
            }

        return null;
        }

    // ----- inner class: SerializationHeader -------------------------------

    /**
     * Simple wrapper class to hold decorated info added by the serialization process.
     */
    protected static final class SerializationHeader
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a new {@link SerializationHeader}.
         *
         * @param cbLength        the length of the payload associated with this header
         * @param serializerName  the {@link Serializer} that can handle the payload
         */
        private SerializationHeader(int cbLength, String serializerName)
            {
            f_nLength    = cbLength;
            f_serializerName = serializerName;
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Return the payload length.
         *
         * @return the payload length
         */
        private int getLength()
            {
            return f_nLength;
            }

        /**
         * Return the {@link Serializer} to handle the payload
         *
         * @return the {@link Serializer} to handle the payload
         */
        private String getSerializerName()
            {
            return f_serializerName;
            }


        // ----- data members -----------------------------------------------

        /**
         * The length, in bytes, of the serialized data.
         */
        protected final int f_nLength;

        /**
         * The name of the serializer that should be used to deserialize the payload.
         */
        protected final String f_serializerName;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The optional {@link ClassLoader}.
     */
    protected WeakReference<ClassLoader> m_refLoader;

    /**
     * The {@link Set} {@link Serializer}s to be used by this {@link Serializer}.
     */
    protected final Serializer[] f_serializers;

    /**
     * The mapping of types referenced by their String name and a working
     * {@link Serializer} for that type.
     */
    protected final ConcurrentMap<String, Serializer> f_mapTypeToSerializer = new ConcurrentHashMap<>(32);

    /**
     * The mapping of {@link Serializer}s keyed by their name.
     */
    protected final Map<String, Serializer> f_idToSerializer;

    /**
     * Symbolic reference for {@code null}.
     */
    protected static final String NULL_TYPE = "NULL";
    }
