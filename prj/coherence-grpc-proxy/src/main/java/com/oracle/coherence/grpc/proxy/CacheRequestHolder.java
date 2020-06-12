/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.Entry;
import com.oracle.coherence.grpc.EntryResult;
import com.oracle.coherence.grpc.OptionalValue;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;

import io.grpc.Status;

import java.util.Map;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * A class that holds a gRPC request, an associated {@link com.tangosol.net.NamedCache}
 * and {@link com.tangosol.util.Binary} converters.
 *
 * @param <Req>  the type of the request
 * @param <Res>  the result type
 *
 * @author Jonathan Knight  2019.11.21
 * @since 14.1.2
 */
public class CacheRequestHolder<Req, Res>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link CacheRequestHolder}.
     *
     * @param request        the cache request
     * @param cache          the pass-through {@link AsyncNamedCache} that the request executes against
     * @param cacheSupplier  the {@link Supplier} to use to obtain a non-pass-through cache
     * @param sFormat        the name of the serializer used to serialize the request payloads
     * @param serializer     the {@link Serializer} used by the request
     * @param executor       the executor for asynchronous processing
     */
    protected CacheRequestHolder(Req request,
                                 AsyncNamedCache<Binary, Binary> cache,
                                 Supplier<NamedCache<?, ?>> cacheSupplier,
                                 String sFormat,
                                 Serializer serializer,
                                 Executor executor)
        {
        this.f_request = request;
        this.f_asyncNamedCache = cache;
        this.f_cacheSupplier = cacheSupplier;
        this.f_sFormat = sFormat;
        this.f_serializer = serializer;
        this.f_executor = executor;
        }

    // ----- public methods -------------------------------------------------

    /**
     * Obtain the cache request.
     *
     * @return the cache request
     */
    protected Req getRequest()
        {
        return f_request;
        }

    /**
     * Obtain the holder's result value.
     *
     * @return the holder's result value
     */
    @SuppressWarnings("unchecked")
    protected Res getResult()
        {
        return (Res) m_result;
        }

    /**
     * Obtain the holder's deserialized result value, assuming that the
     * result value is a serialized {@link Binary}.
     *
     * @param <T>  the deserialized type
     *
     * @return the holder's deserialized result value
     */
    protected <T> T getDeserializedResult()
        {
        return ExternalizableHelper.fromBinary((Binary) m_result, getCacheSerializer());
        }

    /**
     * Obtain the value deserialized from the specified {@link Binary} using the cache's serializer.
     *
     * @param binary  the {@link Binary} of the serialized object
     * @param <T>     the deserialized type
     *
     * @return the deserialized value
     */
    protected <T> T fromCacheBinary(Binary binary)
        {
        return ExternalizableHelper.fromBinary(binary, getCacheSerializer());
        }

    /**
     * Obtain the deserialized {@link Binary} value using the cache's serializer.
     *
     * @param binary  the {@link Binary} of the serialized object
     * @param <T>     the deserialized type
     *
     * @return the deserialized {@link Binary} value using the cache's serializer
     */
    protected <T> T deserialize(Binary binary)
        {
        return ExternalizableHelper.fromBinary(binary, getCacheSerializer());
        }

    /**
     * Obtain the deserialized {@link ByteString} value using the request's serializer.
     *
     * @param bytes  the {@link ByteString} of the serialized object
     * @param <T>    the deserialized type
     *
     * @return the deserialized {@link ByteString} value using the request's serializer
     */
    protected <T> T deserializeRequest(ByteString bytes)
        {
        return ExternalizableHelper.fromBinary(BinaryHelper.toBinary(bytes), f_serializer);
        }

    /**
     * Set the holder's result value.
     *
     * @param t    the result value
     * @param <T>  the type of the result value
     *
     * @return this {@link CacheRequestHolder} cast to the new result type
     */
    @SuppressWarnings("unchecked")
    protected <T> CacheRequestHolder<Req, T> setResult(T t)
        {
        this.m_result = t;
        return (CacheRequestHolder<Req, T>) this;
        }

    /**
     * Return a {@link CompletionStage} that will complete with a value
     * of a {@link CacheRequestHolder} with a result value that is the
     * result of the completion of the specified {@link CompletionStage}.
     *
     * @param stage  that stage that will provide the value for the {@link CacheRequestHolder}
     * @param <T>    the type of the {@link CompletionStage} value
     *
     * @return a {@link CompletionStage} that completes with a {@link CacheRequestHolder}
     */
    protected <T> CompletionStage<CacheRequestHolder<Req, T>> runAsync(CompletionStage<T> stage)
        {
        return stage.handleAsync((r, err) ->
            {
            if (err == null)
                {
                return this.setResult(r);
                }
            throw Status.INTERNAL.withCause(err).withDescription(
                    err.getMessage()).asRuntimeException();
            }, f_executor);
        }

    /**
     * Obtain the cache name.
     *
     * @return the cache name
     */
    protected String getCacheName()
        {
        return f_asyncNamedCache.getNamedCache().getCacheName();
        }

    /**
     * Obtain the {@link AsyncNamedCache} that the request executes on.
     *
     * @return the {@link AsyncNamedCache} that the request executes on
     */
    protected AsyncNamedCache<Binary, Binary> getAsyncCache()
        {
        return f_asyncNamedCache;
        }

    /**
     * Obtain the {@link NamedCache} that the request executes on.
     *
     * @return the {@link NamedCache} that the request executes on
     */
    protected NamedCache<Binary, Binary> getCache()
        {
        return f_asyncNamedCache.getNamedCache();
        }

    /**
     * Obtain the {@link NamedCache} that the request executes on.
     *
     * @param <K>  the key type
     * @param <V>  the value type
     *
     * @return the {@link NamedCache} that the request executes on
     */
    @SuppressWarnings("unchecked")
    protected <K, V> NamedCache<K, V> getNonPassThruCache()
        {
        return (NamedCache<K, V>) f_cacheSupplier.get();
        }

    /**
     * Obtain the request's {@link Serializer}.
     *
     * @return the request's {@link Serializer}
     */
    protected Serializer getSerializer()
        {
        return f_serializer;
        }

    /**
     * Obtain the cache's {@link Serializer}.
     *
     * @return the cache's {@link Serializer}
     */
    protected Serializer getCacheSerializer()
        {
        return f_asyncNamedCache.getNamedCache().getCacheService().getSerializer();
        }

    /**
     * Convert a {@link Binary} in the cache's serialization format
     * to a {@link Binary} in the request's serialization format.
     *
     * @param binary  the {@link Binary} to convert
     *
     * @return a {@link Binary} in the request's serialization format
     */
    protected Binary convertUp(Binary binary)
        {
        if (binary == null)
            {
            return null;
            }
        return ensureConverterUp().convert(binary);
        }

    /**
     * Convert the {@link ByteString} data serialized in the request format
     * to a {@link Binary} key serialized in the cache's serialization format.
     *
     * @param bytes  the {@link ByteString} to convert
     *
     * @return a {@link Binary} key in the cache's serialization format
     */
    protected Binary convertKeyDown(ByteString bytes)
        {
        Binary binary = BinaryHelper.toBinaryKey(bytes);
        return convertKeyDown(binary);
        }


    /**
     * Convert the {@link Binary} data serialized in the request format
     * to a {@link Binary} key serialized in the cache's serialization format.
     *
     * @param binary  the {@link Binary} to convert
     *
     * @return a {@link Binary} key in the cache's serialization format
     */
    protected Binary convertKeyDown(Binary binary)
        {
        return ensureConverterKeyDown().convert(binary);
        }

    /**
     * Convert the {@link ByteString} data serialized in the request format
     * to a {@link Binary} serialized in the cache's serialization format.
     *
     * @param supplier  the supplier of the {@link ByteString} to convert
     *
     * @return a {@link Binary} in the cache's serialization format
     */
    protected Binary convertDown(Supplier<ByteString> supplier)
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
    protected Binary convertDown(ByteString bytes)
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
    protected Binary convertDown(Binary binary)
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
    protected BytesValue deserializeToBytesValue(Binary binary)
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
    protected BytesValue toBytesValue(Binary binary)
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
    protected ByteString toByteString(Binary binary)
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
    protected Entry toEntry(Binary binKey, Binary binValue)
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
    protected EntryResult toEntryResult(Map.Entry<Binary, Binary> entry)
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
    protected OptionalValue toOptionalValue(Binary binary)
        {
        if (binary != null)
            {
            Binary converted = convertUp(binary);
            return OptionalValue.newBuilder().setValue(BinaryHelper.toByteString(converted)).setPresent(true).build();
            }
        return OptionalValue.newBuilder().setPresent(false).build();
        }

    /**
     * Obtain the {@link Converter} used to convert between the request format keys
     * and the cache format keys; creating the {@link Converter} if required.
     *
     * @return the {@link Converter} used to convert between the request format
     *         keys and the cache format keys
     */
    @SuppressWarnings("unchecked")
    protected Converter<Binary, Binary> ensureConverterKeyDown()
        {
        if (m_converterKeyDown == null)
            {
            CacheService cacheService    = f_asyncNamedCache.getNamedCache().getCacheService();
            Serializer   serializerCache = cacheService.getSerializer();
            String       cacheFormat     = serializerCache.getName();

            if ((cacheFormat == null || cacheFormat.isEmpty()) && serializerCache instanceof DefaultSerializer)
                {
                cacheFormat = "java";
                }
            else if ((cacheFormat == null || cacheFormat.isEmpty())
                     && serializerCache instanceof ConfigurablePofContext)
                {
                cacheFormat = "pof";
                }

            if (f_sFormat == null || f_sFormat.trim().isEmpty() || f_sFormat.equals(cacheFormat))
                {
                m_converterKeyDown = BinaryHelper::toBinaryKey;
                }
            else
                {
                BackingMapManagerContext  context      = cacheService.getBackingMapManager().getContext();
                Converter<Object, Binary> converterKey = context.getKeyToInternalConverter();

                m_converterKeyDown = new DownConverter(f_serializer, converterKey);
                }
            }
        return m_converterKeyDown;
        }

    /**
     * Obtain the {@link Converter} used to convert between the request format
     * and the cache format; creating the {@link Converter} if required.
     *
     * @return the {@link Converter} used to convert between the request format
     *         and the cache format
     */
    @SuppressWarnings("unchecked")
    protected Converter<Binary, Binary> ensureConverterDown()
        {
        if (m_converterDown == null)
            {
            CacheService cacheService  = f_asyncNamedCache.getNamedCache().getCacheService();
            Serializer serializerCache = cacheService.getSerializer();
            String     cacheFormat     = serializerCache.getName();

            if ((cacheFormat == null || cacheFormat.isEmpty()) && serializerCache instanceof DefaultSerializer)
                {
                cacheFormat = "java";
                }
            else if ((cacheFormat == null || cacheFormat.isEmpty())
                     && serializerCache instanceof ConfigurablePofContext)
                {
                cacheFormat = "pof";
                }

            if (f_sFormat == null || f_sFormat.trim().isEmpty() || f_sFormat.equals(cacheFormat))
                {
                // pass-thru
                m_converterDown = b -> b;
                }
            else
                {
                BackingMapManagerContext  context        = cacheService.getBackingMapManager().getContext();
                Converter<Object, Binary> converterValue = context.getValueToInternalConverter();

                m_converterDown = new DownConverter(f_serializer, converterValue);
                }
            }
        return m_converterDown;
        }

    /**
     * Obtain the {@link Converter} used to convert between the cache format
     * and the request format; creating the {@link Converter} if required.
     *
     * @return the {@link Converter} used to convert between the cache format
     *         and the request format
     */
    protected Converter<Binary, Binary> ensureConverterUp()
        {
        if (m_converterUp == null)
            {
            CacheService cacheService    = f_asyncNamedCache.getNamedCache().getCacheService();
            Serializer   serializerCache = cacheService.getSerializer();
            String       cacheFormat     = serializerCache.getName();

            if (f_sFormat == null || f_sFormat.trim().isEmpty() || f_sFormat.equals(cacheFormat))
                {
                // pass-thru
                m_converterUp = b -> b;
                }
            else
                {
                m_converterUp = new UpConverter(serializerCache, f_serializer);
                }

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
            this.f_serializerFrom = serializerFrom;
            this.f_serializerTo   = serializerTo;
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
            this.f_serializer = serializer;
            this.f_converter = converter;
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

    // ----- data members ---------------------------------------------------

    /**
     * The request.
     */
    protected final Req f_request;

    /**
     * The {@link AsyncNamedCache} that the request executes against.
     */
    protected final AsyncNamedCache<Binary, Binary> f_asyncNamedCache;

    /**
     * The name of the serializer used to serialize the request payloads.
     */
    protected final String f_sFormat;

    /**
     * The {@link Executor} to use to hand off asynchronous tasks.
     */
    protected final Executor f_executor;

    /**
     * The {@link Supplier} to use to obtain a non-pass-through cache.
     */
    protected final Supplier<NamedCache<?, ?>> f_cacheSupplier;

    /**
     * The converter used to convert between a {@link Binary} serialized in the
     * request format to a {@link Binary} serialized in the cache format.
     */
    protected Converter<Binary, Binary> m_converterDown;

    /**
     * The converter used to convert between a {@link Binary} key serialized in the
     * request format to a {@link Binary} key serialized in the cache format.
     */
    protected Converter<Binary, Binary> m_converterKeyDown;

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
