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
import com.tangosol.io.Serializer;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;

import io.grpc.Status;

import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A class that holds a gRPC request, an associated {@link com.tangosol.net.NamedCache}
 * and {@link com.tangosol.util.Binary} converters.
 *
 * @param <Req>  the type of the request
 * @param <Res>  the result type
 *
 * @author Jonathan Knight  2019.11.21
 * @since 20.06
 */
public class CacheRequestHolder<Req, Res>
        extends RequestHolder<Req, Res>
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
    public CacheRequestHolder(Req request,
                              AsyncNamedCache<Binary, Binary> cache,
                              Supplier<NamedCache<?, ?>> cacheSupplier,
                              String sFormat,
                              Serializer serializer,
                              Executor executor)
        {
        super(request, sFormat, serializer, cache.getNamedCache().getCacheService(), executor);
        f_asyncNamedCache = cache;
        f_cacheSupplier   = cacheSupplier;
        }

    // ----- public methods -------------------------------------------------

    /**
     * Obtain the cache name.
     *
     * @return the cache name
     */
    public String getCacheName()
        {
        return f_asyncNamedCache.getNamedCache().getCacheName();
        }

    /**
     * Obtain the {@link AsyncNamedCache} that the request executes on.
     *
     * @return the {@link AsyncNamedCache} that the request executes on
     */
    public AsyncNamedCache<Binary, Binary> getAsyncCache()
        {
        return f_asyncNamedCache;
        }

    /**
     * Obtain the {@link NamedCache} that the request executes on.
     *
     * @return the {@link NamedCache} that the request executes on
     */
    public NamedCache<Binary, Binary> getCache()
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
    public <K, V> NamedCache<K, V> getNonPassThruCache()
        {
        return (NamedCache<K, V>) f_cacheSupplier.get();
        }

    /**
     * Obtain the cache's {@link Serializer}.
     *
     * @return the cache's {@link Serializer}
     */
    public Serializer getCacheSerializer()
        {
        return getServiceSerializer();
        }

    /**
     * Convert the {@link ByteString} data serialized in the request format
     * to a {@link Binary} key serialized in the cache's serialization format.
     *
     * @param bytes  the {@link ByteString} to convert
     *
     * @return a {@link Binary} key in the cache's serialization format
     */
    public Binary convertKeyDown(ByteString bytes)
        {
        Binary  binary = BinaryHelper.toBinary(bytes);
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
    public Binary convertKeyDown(Binary binary)
        {
        return ensureConverterKeyDown().convert(binary);
        }

    /**
     * Obtain the {@link Converter} used to convert between the request format keys
     * and the cache format keys; creating the {@link Converter} if required.
     *
     * @return the {@link Converter} used to convert between the request format
     *         keys and the cache format keys
     */
    @SuppressWarnings("unchecked")
    public Converter<Binary, Binary> ensureConverterKeyDown()
        {
        if (m_converterKeyDown == null)
            {
            CacheService cacheService = f_asyncNamedCache.getNamedCache().getCacheService();
            String       cacheFormat  = getCacheFormat(cacheService);

            Converter<Binary, Binary> converter;
            if (f_sFormat == null || f_sFormat.trim().isEmpty() || f_sFormat.equals(cacheFormat))
                {
                if (cacheService instanceof PartitionedService)
                    {
                    converter = ((PartitionedService) cacheService).instantiateKeyToBinaryConverter(null, true);
                    }
                else
                    {
                    converter = bin -> bin;
                    }
                }
            else
                {
                BackingMapManagerContext  context      = cacheService.getBackingMapManager().getContext();
                Converter<Object, Binary> converterKey = context.getKeyToInternalConverter();

                converter = new DownConverter(f_serializer, converterKey);
                }

            m_converterKeyDown = new ErrorHandlingConverter<>(converter);
            }
        return m_converterKeyDown;
        }

    @Override
    @SuppressWarnings("unchecked")
    protected Converter<Object, Binary> createConverterDown()
        {
        return ((CacheService) f_service).getBackingMapManager().getContext().getValueToInternalConverter();
        }

    /**
     * Return a {@link CompletionStage} that will complete with a value
     * of a {@link RequestHolder} with a result value that is the
     * result of the completion of the specified {@link CompletionStage}.
     *
     * @param stage  that stage that will provide the value for the {@link RequestHolder}
     * @param <T>    the type of the {@link CompletionStage} value
     *
     * @return a {@link CompletionStage} that completes with a {@link RequestHolder}
     */
    public <T> CompletionStage<CacheRequestHolder<Req, T>> runAsync(CompletionStage<T> stage)
        {
        return stage.handleAsync((r, err) ->
            {
            if (err == null)
                {
                return (CacheRequestHolder<Req, T>) setResult(r);
                }
            throw Status.INTERNAL.withCause(err).withDescription(err.getMessage()).asRuntimeException();
            }, f_executor);
        }

    /**
     * Return a {@link Consumer} of binary {@link Map.Entry} instances that sends
     * the entries to the specified {@link StreamObserver}.
     *
     * @param observer  the {@link StreamObserver} to receive the entries
     *
     * @return a {@link Consumer} of binary {@link Map.Entry} instances that sends
     *         the entries to the {@link StreamObserver}
     */
    public Consumer<Map.Entry<? extends Binary, ? extends Binary>> entryConsumer(StreamObserver<Entry> observer)
        {
        return entry -> observer.onNext(toEntry(entry.getKey(), entry.getValue()));
        }


    /**
     * Return a {@link Consumer} of {@link Binary} instances that sends
     * the binary to the specified {@link StreamObserver}.
     *
     * @param observer  the {@link StreamObserver} to receive the binary values
     *
     * @return a {@link Consumer} of {@link Binary} instances that sends
     *         the binaries to the {@link StreamObserver}
     */
    public Consumer<Binary> binaryConsumer(StreamObserver<BytesValue> observer)
        {
        return binary -> observer.onNext(BinaryHelper.toBytesValue(convertUp(binary)));
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link AsyncNamedCache} that the request executes against.
     */
    protected final AsyncNamedCache<Binary, Binary> f_asyncNamedCache;

    /**
     * The {@link Supplier} to use to obtain a non-pass-through cache.
     */
    protected final Supplier<NamedCache<?, ?>> f_cacheSupplier;

    /**
     * The converter used to convert between a {@link Binary} key serialized in the
     * request format to a {@link Binary} key serialized in the cache format.
     */
    protected Converter<Binary, Binary> m_converterKeyDown;
    }
