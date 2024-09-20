/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.v0;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.messages.cache.v0.Entry;
import com.oracle.coherence.grpc.messages.cache.v0.EntryResult;
import com.oracle.coherence.grpc.messages.cache.v0.OptionalValue;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.Service;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;

import io.grpc.Status;

import java.util.Map;

import java.util.concurrent.Executor;

import java.util.function.Supplier;

/**
 * A class that holds a gRPC request and {@link Binary} converters.
 *
 * @param <Req>  the type of the request
 * @param <Res>  the result type
 *
 * @author Jonathan Knight  2023.02.03
 * @since 23.03
 */
public abstract class RequestHolder<Req, Res>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link RequestHolder}.
     *
     * @param request        the cache request
     * @param sFormat        the name of the serializer used to serialize the request payloads
     * @param serializer     the {@link Serializer} used by the request
     * @param executor       the executor for asynchronous processing
     */
    public RequestHolder(Req request, String sFormat, Serializer serializer, Service service, Executor executor)
        {
        f_request    = request;
        f_sFormat    = sFormat;
        f_serializer = serializer;
        f_service    = service;
        f_executor   = executor;
        }

    // ----- public methods -------------------------------------------------

    /**
     * Obtain the cache request.
     *
     * @return the cache request
     */
    public Req getRequest()
        {
        return f_request;
        }

    /**
     * Obtain the holder's result value.
     *
     * @return the holder's result value
     */
    @SuppressWarnings("unchecked")
    public Res getResult()
        {
        return (Res) m_result;
        }


    /**
     * Obtain the cache's {@link Serializer}.
     *
     * @return the cache's {@link Serializer}
     */
    public Serializer getServiceSerializer()
        {
        return f_service.getSerializer();
        }

    /**
     * Obtain the holder's deserialized result value, assuming that the
     * result value is a serialized {@link Binary}.
     *
     * @param <T>  the deserialized type
     *
     * @return the holder's deserialized result value
     */
    public <T> T getDeserializedResult()
        {
        return ExternalizableHelper.fromBinary((Binary) m_result, getServiceSerializer());
        }

    /**
     * Obtain the value deserialized from the specified {@link Binary} using the cache's serializer.
     *
     * @param binary  the {@link Binary} of the serialized object
     * @param <T>     the deserialized type
     *
     * @return the deserialized value
     */
    public <T> T fromBinary(Binary binary)
        {
        return ExternalizableHelper.fromBinary(binary, getServiceSerializer());
        }

    /**
     * Obtain the deserialized {@link Binary} value using the cache's serializer.
     *
     * @param binary  the {@link Binary} of the serialized object
     * @param <T>     the deserialized type
     *
     * @return the deserialized {@link Binary} value using the cache's serializer
     */
    public <T> T deserialize(Binary binary)
        {
        return ExternalizableHelper.fromBinary(binary, getServiceSerializer());
        }

    /**
     * Obtain the deserialized {@link ByteString} value using the request's serializer.
     *
     * @param bytes  the {@link ByteString} of the serialized object
     * @param <T>    the deserialized type
     *
     * @return the deserialized {@link ByteString} value using the request's serializer
     */
    public <T> T deserializeRequest(ByteString bytes)
        {
        return ExternalizableHelper.fromBinary(BinaryHelper.toBinary(bytes), f_serializer);
        }

    /**
     * Set the holder's result value.
     *
     * @param t    the result value
     * @param <T>  the type of the result value
     *
     * @return this {@link RequestHolder} cast to the new result type
     */
    @SuppressWarnings("unchecked")
    public <T> RequestHolder<Req, T> setResult(T t)
        {
        this.m_result = t;
        return (RequestHolder<Req, T>) this;
        }

    /**
     * Obtain the request's {@link Serializer}.
     *
     * @return the request's {@link Serializer}
     */
    public Serializer getSerializer()
        {
        return f_serializer;
        }

    /**
     * Convert a {@link Binary} in the cache's serialization format
     * to a {@link Binary} in the request's serialization format.
     *
     * @param binary  the {@link Binary} to convert
     *
     * @return a {@link Binary} in the request's serialization format
     */
    public Binary convertUp(Binary binary)
        {
        if (binary == null)
            {
            return null;
            }
        return ensureConverterUp().convert(binary);
        }

    /**
     * Convert the {@link ByteString} data serialized in the request format
     * to a {@link Binary} serialized in the cache's serialization format.
     *
     * @param supplier  the supplier of the {@link ByteString} to convert
     *
     * @return a {@link Binary} in the cache's serialization format
     */
    public Binary convertDown(Supplier<ByteString> supplier)
        {
        return convertDown(supplier.get());
        }

    /**
     * Convert the {@link ByteString} data serialized in the request format
     * to a {@link Binary} serialized in the cache's serialization format.
     *
     * @param bytes  the {@link ByteString} to convert
     *
     * @return a {@link Binary} in the cache's serialization format
     */
    public Binary convertDown(ByteString bytes)
        {
        Binary binary = BinaryHelper.toBinary(bytes);
        return convertDown(binary);
        }

    /**
     * Convert the {@link Binary} data serialized in the request format
     * to a {@link Binary} serialized in the cache's serialization format.
     *
     * @param binary  the {@link Binary} to convert
     *
     * @return a {@link Binary} in the cache's serialization format
     */
    public Binary convertDown(Binary binary)
        {
        return ensureConverterDown().convert(binary);
        }

    /**
     * Convert the {@link Binary} serialized in the cache's serialization format
     * to a {@link BytesValue} serialized in the request's serialization format.
     * <p>
     * The assumption is that the {@link Binary} deserializes to another {@link Binary}.
     *
     * @param binary  the {@link Binary} to convert
     *
     * @return a {@link BytesValue} in the request's serialization format
     */
    public BytesValue deserializeToBytesValue(Binary binary)
        {
        return toBytesValue(deserialize(binary));
        }

    /**
     * Convert the {@link Binary} serialized in the cache's serialization format
     * to a {@link BytesValue} serialized in the request's serialization format.
     *
     * @param binary  the {@link Binary} to convert
     *
     * @return a {@link BytesValue} in the request's serialization format
     */
    public BytesValue toBytesValue(Binary binary)
        {
        return BytesValue.of(toByteString(binary));
        }

    /**
     * Convert the {@link Binary} serialized in the cache's serialization format
     * to a {@link ByteString} serialized in the request's serialization format.
     *
     * @param binary  the {@link Binary} to convert
     *
     * @return a {@link ByteString} in the request's serialization format
     */
    public ByteString toByteString(Binary binary)
        {
        return BinaryHelper.toByteString(ensureConverterUp().apply(binary));
        }

    /**
     * Convert a {@link Map.Entry} of {@link Binary} key and value serialized in the cache's
     * serialization format into an {@link Entry} with a key and value serialized in the
     * request's serialization format.
     *
     * @param binKey    the {@link Binary} key
     * @param binValue  the {@link Binary} value
     *
     * @return a {@link Entry} in the request's serialization format
     */
    public Entry toEntry(Binary binKey, Binary binValue)
        {
        return Entry.newBuilder()
                .setKey(BinaryHelper.toByteString(ensureConverterUp().apply(binKey)))
                .setValue(BinaryHelper.toByteString(ensureConverterUp().apply(binValue)))
                .build();
        }

    /**
     * Convert a {@link Map.Entry} of {@link Binary} key and value serialized in the cache's
     * serialization format into an {@link EntryResult} with a key and value serialized in
     * the request's serialization format.
     *
     * @param entry  the {@link Binary} to convert
     *
     * @return a {@link EntryResult} in the request's serialization format
     */
    public EntryResult toEntryResult(Map.Entry<Binary, Binary> entry)
        {
        return EntryResult.newBuilder()
                .setKey(BinaryHelper.toByteString(ensureConverterUp().apply(entry.getKey())))
                .setValue(BinaryHelper.toByteString(ensureConverterUp().apply(entry.getValue())))
                .build();
        }

    /**
     * Convert a {@link Binary} serialized with the cache's serializer to an
     * {@link OptionalValue} containing an optional {@link Binary} serialized
     * with the request's serializer.
     *
     * @param binary  the optional {@link Binary} value
     *
     * @return a {@link OptionalValue} in the request's serialization format
     */
    public OptionalValue toOptionalValue(Binary binary)
        {
        if (binary != null)
            {
            Binary converted = convertUp(binary);
            return OptionalValue.newBuilder().setValue(BinaryHelper.toByteString(converted)).setPresent(true).build();
            }
        return OptionalValue.newBuilder().setPresent(false).build();
        }

    /**
     * Obtain the {@link Converter} used to convert between the request format
     * and the cache format; creating the {@link Converter} if required.
     *
     * @return the {@link Converter} used to convert between the request format
     *         and the cache format
     */
    public Converter<Binary, Binary> ensureConverterDown()
        {
        if (m_converterDown == null)
            {
            String sFormat = getCacheFormat(f_service);

            Converter<Binary, Binary> converter;
            if (f_sFormat == null || f_sFormat.trim().isEmpty() || f_sFormat.equals(sFormat))
                {
                // pass-thru
                converter = b -> b;
                }
            else
                {
                Converter<Object, Binary> converterValue = createConverterDown();
                converter = new DownConverter(f_serializer, converterValue);
                }

            m_converterDown = new ErrorHandlingConverter<>(converter);
            }
        return m_converterDown;
        }

    /**
     * Create the {@link Converter} to use to convert from
     * Object form to internal {@link Binary} form.
     *
     * @return the {@link Converter} to use to convert from
     *         Object form to internal {@link Binary} form
     */
    protected abstract Converter<Object, Binary> createConverterDown();

    /**
     * Returns the serializer format name for the specified {@link Service}'s serializer.
     *
     * @param service  the {@link Service} to obtain the serializer format from
     *
     * @return  the serializer format name for the specified {@link Service}'s serializer.
     */
    public static String getCacheFormat(Service service)
        {
        Serializer serializer  = service.getSerializer();
        String     cacheFormat = serializer.getName();

        if ((cacheFormat == null || cacheFormat.isEmpty()) && serializer instanceof DefaultSerializer)
            {
            cacheFormat = "java";
            }
        else if ((cacheFormat == null || cacheFormat.isEmpty())
                 && serializer instanceof ConfigurablePofContext)
            {
            cacheFormat = "pof";
            }
        return cacheFormat;
        }

    /**
     * Obtain the {@link Converter} used to convert between the cache format
     * and the request format; creating the {@link Converter} if required.
     *
     * @return the {@link Converter} used to convert between the cache format
     *         and the request format
     */
    public Converter<Binary, Binary> ensureConverterUp()
        {
        if (m_converterUp == null)
            {
            Serializer   serializer  = f_service.getSerializer();
            String       cacheFormat = serializer.getName();

            Converter<Binary, Binary> converter;
            if (f_sFormat == null || f_sFormat.trim().isEmpty() || f_sFormat.equals(cacheFormat))
                {
                // pass-thru
                converter = b -> b;
                }
            else                                                                                       
                {
                converter = new UpConverter(serializer, f_serializer);
                }

            m_converterUp = new ErrorHandlingConverter<>(converter);
            }
        return m_converterUp;
        }

    // ----- inner class: UpConverter ---------------------------------------

    /**
     * A {@link Converter} that converts from a {@link Binary} serialized
     * in one format to a {@link Binary} serialized in a different format.
     */
    protected static class UpConverter
            implements Converter<Binary, Binary>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct the {@link Converter} that converts from a {@link Binary} serialized
         * in one format to a {@link Binary} serialized in a different format.
         *
         * @param serializerFrom  the {@link Serializer} to convert from
         * @param serializerTo    the {@link Serializer} to serialize to
         */
        protected UpConverter(Serializer serializerFrom, Serializer serializerTo)
            {
            f_serializerFrom = serializerFrom;
            f_serializerTo   = serializerTo;
            }

        // ----- Converter interface ----------------------------------------

        @Override
        public Binary convert(Binary binary)
            {
            if (binary == null)
                {
                return null;
                }
            Object o = ExternalizableHelper.fromBinary(binary, f_serializerFrom);
            return ExternalizableHelper.toBinary(o, f_serializerTo);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Serializer} used to convert the {@link Binary} to object form.
         */
        protected final Serializer f_serializerFrom;

        /**
         * The {@link Serializer} used to convert the object to a {@link Binary}.
         */
        protected final Serializer f_serializerTo;
        }

    // ----- inner class: KeyDownConverter ----------------------------------

    /**
     * A {@link Converter} that converts from a {@link Binary} serialized
     * in one format to a {@link Binary} key serialized in a different format.
     */
    protected static class DownConverter
            implements Converter<Binary, Binary>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct the {@link Converter} that converts from a {@link Binary} serialized
         * in one format to a {@link Binary} key serialized in a different format.
         *
         * @param serializer  the {@link Serializer}
         * @param converter   the {@link Converter}
         */
        protected DownConverter(Serializer serializer, Converter<Object, Binary> converter)
            {
            f_serializer = serializer;
            f_converter = converter;
            }

        // ----- Converter interface ----------------------------------------

        @Override
        public Binary convert(Binary binary)
            {
            if (binary == null)
                {
                return null;
                }
            Object o = ExternalizableHelper.fromBinary(binary, f_serializer);
            return f_converter.convert(o);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Serializer} used to convert the original {@link Binary}
         * into object form.
         */
        protected final Serializer f_serializer;

        /**
         * The {@link Converter} used to convert the object into a {@link Binary}.
         */
        protected final Converter<Object, Binary> f_converter;
        }

    // ----- inner class: ErrorHandlingConverter ----------------------------

    protected static class ErrorHandlingConverter<F, T>
            implements Converter<F, T>
        {
        protected ErrorHandlingConverter(Converter<F, T> converter)
            {
            f_converter = converter;
            }

        @Override
        public T convert(F value)
            {
            try
                {
                return f_converter.convert(value);
                }
            catch (Throwable t)
                {
                throw Status.UNKNOWN
                        .withDescription("Caught an exception while serializing or deserializing: " + t.getMessage())
                        .withCause(t)
                        .asRuntimeException();
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The wrapped converter.
         */
        private final Converter<F, T> f_converter;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The request.
     */
    protected final Req f_request;

    /**
     * The name of the serializer used to serialize the request payloads.
     */
    protected final String f_sFormat;

    /**
     * The {@link Executor} to use to hand off asynchronous tasks.
     */
    protected final Executor f_executor;

    /**
     * The {@link Service} managing the resource.
     */
    protected final Service f_service;

    /**
     * The converter used to convert between a {@link Binary} serialized in the
     * request format to a {@link Binary} serialized in the cache format.
     */
    protected Converter<Binary, Binary> m_converterDown;

    /**
     * The converter used to convert between a {@link Binary} serialized in the
     * cache format to a {@link Binary} serialized in the request format.
     */
    protected Converter<Binary, Binary> m_converterUp;

    /**
     * The {@link Serializer} used by the request.
     */
    protected final Serializer f_serializer;

    /**
     * A result value.
     */
    protected Object m_result;
    }
