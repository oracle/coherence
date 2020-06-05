/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc;

import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.Serializer;
import com.tangosol.io.WrapperBufferInput;

import io.grpc.MethodDescriptor;
import io.grpc.Status;

import io.grpc.internal.ReadableBuffer;
import io.grpc.internal.ReadableBuffers;

import io.helidon.grpc.core.MarshallerSupplier;

import java.io.DataInputStream;
import java.io.InputStream;

import javax.inject.Named;

/**
 * gRPC {@code Marshaller} implementation that delegates to the specified {@link
 * Serializer}.
 *
 * @param <T> the type to marshal
 *
 * @author Aleks Seovic  2017.09.19
 */
public class SerializerMarshaller<T>
        implements MethodDescriptor.Marshaller<T>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code SerializerMarshaller} instance.
     *
     * @param serializer the {@link Serializer} to use
     * @param clazz      the class to marshal
     */
    protected SerializerMarshaller(Serializer serializer, Class<? extends T> clazz)
        {
        this.f_serializer = serializer;
        this.f_clazz = clazz;
        }

    // ---- Marshaller interface --------------------------------------------

    @Override
    public InputStream stream(T value)
        {
        try
            {
            ByteArrayWriteBuffer bufOut = new ByteArrayWriteBuffer(512);
            f_serializer.serialize(bufOut.getBufferOutput(), value);

            ReadableBuffer bufIn = ReadableBuffers.wrap(bufOut.getRawByteArray(), 0, bufOut.length());
            return ReadableBuffers.openStream(bufIn, true);
            }
        catch (Throwable t)
            {
            Logger.err("Unexpected error during gRPC marshalling", t);
            throw Status.INTERNAL.withCause(t).asRuntimeException();
            }
        }

    @Override
    public T parse(InputStream stream)
        {
        try
            {
            return f_serializer.deserialize(new WrapperBufferInput(new DataInputStream(stream)), f_clazz);
            }
        catch (Throwable t)
            {
            Logger.err("Unexpected error during gRPC marshalling", t);
            throw Status.INTERNAL.withCause(t).asRuntimeException();
            }
        }

    // ---- inner class: Supplier -------------------------------------------

    /**
     * A {@link MarshallerSupplier} implementation that supplies instance of
     * a default {@link ExternalizableLite} serializer.
     */
    @Named("ext-lite")
    public static class Supplier
            implements MarshallerSupplier
        {
        public Supplier()
            {
            f_serializer = new DefaultSerializer(Classes.ensureClassLoader(null));
            }

        @Override
        public <T> MethodDescriptor.Marshaller<T> get(Class<T> clazz)
            {
            return new SerializerMarshaller<>(f_serializer, clazz);
            }

        private final Serializer f_serializer;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The {@link Serializer} to use.
     */
    private final Serializer f_serializer;

    /**
     * The class to marshal.
     */
    private final Class<? extends T> f_clazz;
    }
