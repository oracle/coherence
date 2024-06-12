/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.UnsafeByteOperations;

import com.oracle.coherence.common.base.Classes;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.MaybeByteString;
import com.oracle.coherence.grpc.SimpleDaemonPoolExecutor;

import com.tangosol.coherence.config.Config;

import com.tangosol.io.NamedSerializerFactory;
import com.tangosol.io.Serializer;

import com.tangosol.net.cache.KeyAssociation;
import com.tangosol.net.events.EventDispatcher;

import com.tangosol.net.grpc.GrpcDependencies;

import com.tangosol.util.Binary;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.ExternalizableHelper;

import io.grpc.Channel;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A base class for gRPC clients.
 *
 * @param <V>  the type of value this client contains
 *
 * @author Jonathan Knight  2023.02.02
 * @since 23.03
 */
public abstract class BaseGrpcClient<V, ClientType extends ClientProtocol>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates an {@link AsyncNamedCacheClient} from the specified
     * {@link AsyncNamedCacheClient.Dependencies}.
     *
     * @param dependencies  the {@link AsyncNamedCacheClient.Dependencies} to configure this
     *                      {@link AsyncNamedCacheClient}.
     * @param client        the {@link NamedCacheClientChannel} to use to send requests
     */
    public BaseGrpcClient(Dependencies dependencies, ClientType client)
        {
        f_client       = Objects.requireNonNull(client);
        f_dependencies = dependencies;
        f_sName        = dependencies.getName();
        f_dispatcher   = dependencies.getEventDispatcher();
        f_sScopeName   = dependencies.getScopeName().orElse(GrpcDependencies.DEFAULT_SCOPE);
        f_executor     = dependencies.getExecutor().orElseGet(BaseGrpcClient::createDefaultExecutor);
        f_sFormat      = dependencies.getSerializerFormat()
                              .orElseGet(() -> dependencies.getSerializer()
                                   .map(Serializer::getName)
                                   .orElseGet(BaseGrpcClient::getDefaultSerializerFormat));
        f_serializer   = dependencies.getSerializer().orElseGet(() -> createSerializer(f_sFormat));
        f_fDeferKeyAssociationCheck = dependencies.isDeferKeyAssociationCheck();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the {@link ClientProtocol} being used.
     *
     * @return the {@link ClientProtocol} being used
     */
    public ClientType getClientProtocol()
        {
        return f_client;
        }

    /**
     * Return {@code true} if the cache has been released.
     *
     * @return {@code true} if the cache has been released
     */
    public boolean isReleased()
        {
        return m_fReleased;
        }

    /**
     * Return {@code true} if the cache has been destroyed.
     *
     * @return {@code true} if the cache has been destroyed
     */
    public boolean isDestroyed()
        {
        return m_fDestroyed;
        }

    /**
     * Helper method for cache active checks.
     *
     * @return {@code true} if the cache is still active
     */
    public boolean isActiveInternal()
        {
        return f_client.isActive() && !m_fReleased && !m_fDestroyed;
        }

    /**
     * Obtain the default {@link Serializer} to be used by the cache.
     *
     * @return  the default {@link Serializer} to be used by the cache
     */
    protected static Serializer createSerializer(String sFormat)
        {
        return NamedSerializerFactory.DEFAULT.getNamedSerializer(sFormat, Classes.getContextClassLoader());
        }

    /**
     * Return the default serialization format.
     * <p>
     * The default format will be Java serialization unless
     * the {@code coherence.pof.enabled} property is set
     * when it will be POF.
     *
     * @return the default serialization format
     */
    public static String getDefaultSerializerFormat()
        {
        return Config.getBoolean("coherence.pof.enabled") ? "pof" : "java";
        }

    /**
     * Obtain the default {@link Executor} to be used by the cache.
     *
     * @return  the default {@link Executor} to be used by the cache
     */
    protected static Executor createDefaultExecutor()
        {
        return Executors.newSingleThreadExecutor();
        }

    /**
     * A utility method to serialize an Object to {@link BytesValue}.
     *
     * @param obj  the object to convert to {@link BytesValue}.
     *
     * @return the {@link BytesValue} for the specified object
     */
    public BytesValue toBytesValue(Object obj)
        {
        return BytesValue.of(toByteString(obj));
        }

    /**
     * A utility method to serialize an Object to {@link ByteString}
     * or return {@code null} if the value is {@code null}.
     *
     * @param obj  the object to convert to {@link ByteString}.
     *
     * @return the {@link ByteString} for the specified object or
     *         {@code null} if the value is {@code null}
     */
    public ByteString toByteStringOrNull(Object obj)
        {
        return obj == null ? null : toByteString(obj);
        }

    /**
     * A utility method to serialize an Object to {@link ByteString}.
     *
     * @param obj  the object to convert to {@link ByteString}.
     *
     * @return the {@link ByteString} for the specified object
     */
    public ByteString toByteString(Object obj)
        {
        return UnsafeByteOperations.unsafeWrap(ExternalizableHelper.toBinary(obj, f_serializer).toByteBuffer());
        }

    /**
     * Convert the specified object to a binary key.
     *
     * @param obj  the object to convert
     *
     * @return the {@link ByteString} for the specified key object
     */
    public ByteString toKeyByteString(Object obj)
        {
        Binary bin = ExternalizableHelper.toBinary(obj, f_serializer);
        if (f_fDeferKeyAssociationCheck)
            {
            return UnsafeByteOperations.unsafeWrap(bin.toByteBuffer());
            }
        BinaryHelper.toKeyByteString(bin, obj, f_serializer);
        Binary binDeco = bin;

        if (obj instanceof KeyAssociation)
            {
            obj = ((KeyAssociation<?>) obj).getAssociatedKey();
            if (obj != null)
                {
                binDeco = ExternalizableHelper.toBinary(obj, f_serializer);
                }
            }

        return UnsafeByteOperations.unsafeWrap(ExternalizableHelper.decorateBinary(bin,
                binDeco.calculateNaturalPartition(0)).toByteBuffer());
        }

    /**
     * Convert a {@link Map} of {@link ByteString} keys and values
     * to a deserialized map.
     *
     * @param map     the map to convert
     * @param <KOut>  the type of the deserialized key
     * @param <VOut>  the type of the deserialized value
     *
     * @return  the deserialized map
     */
    <KOut, VOut> Map<KOut, VOut> toMap(Map<ByteString, ByteString> map)
        {
        return ConverterCollections.getMap(map, this::fromByteString, this::toByteString, this::fromByteString, this::toByteString);
        }


    /**
     * Convert a {@link Map.Entry} of {@link ByteString} keys and values
     * to a deserialized map.
     *
     * @param entry   the {@link Map.Entry} to convert
     * @param <KOut>  the type of the deserialized key
     * @param <VOut>  the type of the deserialized value
     *
     * @return  the deserialized map
     */
    <KOut, VOut> Map.Entry<KOut, VOut> toMapEntry(Map.Entry<ByteString, ByteString> entry)
        {
        return ConverterCollections.getEntry(entry, this::fromByteString, this::fromByteString, this::toByteString);
        }

    /**
     * A utility method to deserialize a {@link BytesValue} to an Object.
     *
     * @param bv  the {@link BytesValue} to deserialize an Object.
     *
     * @return an object from the specified {@link BytesValue}
     */
    public V valueFromBytesValue(BytesValue bv)
        {
        return fromBytesValue(bv);
        }

    /**
     * A utility method to deserialize a {@link BytesValue} to an Object.
     *
     * @param bytes  the {@link BytesValue} to deserialize an Object.
     *
     * @return an object from the specified {@link BytesValue}
     */
    public <T> T fromBytesValue(BytesValue bytes)
        {
        return BinaryHelper.fromBytesValue(bytes, f_serializer);
        }

    /**
     * A utility method to deserialize a {@link ByteString} to an Object.
     *
     * @param bytes  the {@link ByteString} to deserialize an Object.
     *
     * @return an object from the specified {@link ByteString}
     */
    public <T> T fromByteString(ByteString bytes)
        {
        return BinaryHelper.fromByteString(bytes, f_serializer);
        }


    /**
     * A utility method to deserialize a {@link MaybeByteString} to an Object.
     *
     * @param bytes         the {@link MaybeByteString} to deserialize an Object.
     * @param defaultValue  the value to return if the {@link MaybeByteString} is empty
     *
     * @return an object from the specified {@link MaybeByteString}
     */
    public <T> T fromByteString(MaybeByteString bytes, T defaultValue)
        {
        if (bytes.isPresent())
            {
            return BinaryHelper.fromByteString(bytes.value(), f_serializer);
            }
        return defaultValue;
        }

    /**
     * Create a failed {@link CompletableFuture}.
     *
     * @param t   the error to use
     * @param <T> the type of the future
     *
     * @return a failed {@link CompletableFuture}
     */
    public <T> CompletableFuture<T> failedFuture(Throwable t)
        {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(t);
        return future;
        }

    // ----- Dependencies ---------------------------------------------------

    /**
     * The dependencies used to create an {@link AsyncNamedCacheClient}.
     */
    public interface Dependencies
        {
        /**
         * Return the underlying resource name.
         *
         * @return  the underlying resource name
         */
        String getName();

        /**
         * Returns the gRPC {@link Channel}.
         * <p>
         * This method must not return {@code null}.
         *
         * @return the gRPC {@link Channel}
         */
        Channel getChannel();

        /**
         * Returns the scope named used to link requests
         * to a specific server side scope.
         *
         * @return the scope named used to for requests
         */
        Optional<String> getScopeName();

        /**
         * Return the {@link Serializer} to use to
         * serialize request and response payloads.
         *
         * @return the {@link Serializer} to use for
         *         request and response payloads
         */
        Optional<Serializer> getSerializer();

        /**
         * Return the name of the serialization format to be used
         * for requests and responses.
         *
         * @return the name of the serialization format to be used
         *         for requests and responses
         */
        Optional<String> getSerializerFormat();

        /**
         * Return the optional {@link Executor} to use.
         *
         * @return the optional {@link Executor} to use
         */
        Optional<Executor> getExecutor();

        /**
         * Returns the event dispatcher for the resource.
         *
         * @return the event dispatcher for the resource
         */
        EventDispatcher getEventDispatcher();

        /**
         * Obtain a default rpc deadline value.
         * <p/>
         * If set to a value less than or equal to zero, a value of 30 seconds is used.
         *
         * @return the default request timeout
         */
        default long getDeadline()
            {
            return GrpcDependencies.DEFAULT_DEADLINE_MILLIS;
            }

        /**
         * Return the flag to indicate if the KeyAssociation check is deferred.
         *
         * @return  the flag to indicate if the KeyAssociation check is deferred
         */
        default boolean isDeferKeyAssociationCheck()
            {
            return false;
            }
        }

    // ----- DefaultDependencies ----------------------------------------

    /**
     * The default dependencies implementation.
     */
    public static class DefaultDependencies
            implements Dependencies
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link AsyncNamedCacheClient.DefaultDependencies}.
         *
         * @param channel     the gRPC {@link Channel} to use
         * @param dispatcher  the {@link EventDispatcher} to use for lifecycle events
         */
        public DefaultDependencies(String sName, Channel channel, EventDispatcher dispatcher)
            {
            f_sName      = sName;
            f_channel    = channel;
            m_sScopeName = GrpcDependencies.DEFAULT_SCOPE;
            f_dispatcher = dispatcher;
            }

        // ----- Dependencies methods ---------------------------------------

        @Override
        public String getName()
            {
            return f_sName;
            }

        @Override
        public Channel getChannel()
            {
            return f_channel;
            }

        @Override
        public Optional<String> getScopeName()
            {
            return Optional.ofNullable(m_sScopeName);
            }

        @Override
        public Optional<Serializer> getSerializer()
            {
            return Optional.ofNullable(m_serializer);
            }

        @Override
        public Optional<String> getSerializerFormat()
            {
            return Optional.ofNullable(m_serializerFormat);
            }

        @Override
        public Optional<Executor> getExecutor()
            {
            return Optional.ofNullable(m_executor);
            }

        @Override
        public EventDispatcher getEventDispatcher()
            {
            return f_dispatcher;
            }

        @Override
        public long getDeadline()
            {
            if (m_cDeadlineMillis <= 0)
                {
                return Dependencies.super.getDeadline();
                }
            return m_cDeadlineMillis;
            }

        @Override
        public boolean isDeferKeyAssociationCheck()
            {
            return m_fDeferKeyAssociationCheck;
            }

        // ----- setters ----------------------------------------------------

        /**
         * Set the scope name.
         *
         * @param sScopeName  the scope name
         */
        public void setScope(String sScopeName)
            {
            m_sScopeName = GrpcDependencies.DEFAULT_SCOPE_ALIAS.equals(sScopeName)
                    ? GrpcDependencies.DEFAULT_SCOPE : sScopeName;
            }

        /**
         * Set the optional {@link Executor} to use instead
         * of a {@link SimpleDaemonPoolExecutor}.
         *
         * @param executor the optional {@link Executor} to use
         */
        public void setExecutor(Executor executor)
            {
            m_executor = executor;
            }

        /**
         * Set the serialization format to use for requests and
         * responses.
         * <p>
         * Calling this method will remove any {@link Serializer} set by calling
         * {@link #setSerializer(Serializer)} or {@link #setSerializer(Serializer, String)}.
         * <p>
         * A {@link Serializer} configuration with the specified name must  exist in the
         * Coherence Operational Configuration.
         *
         * @param sFormat  the serialization format and reference to a {@link Serializer}
         *                 configuration in the Coherence Operational Configuration
         */
        @SuppressWarnings("unused")
        public void setSerializerFormat(String sFormat)
            {
            m_serializerFormat = sFormat;
            m_serializer       = null;
            }

        /**
         * Set the {@link Serializer} to use for requests and response payloads.
         * <p>
         * The serialization format used will be obtained by calling the
         * {@link Serializer#getName()} method on the serializer.
         *
         * @param serializer  the {@link Serializer} to use to serialize request
         *                    and response payloads
         */
        public void setSerializer(Serializer serializer)
            {
            String sFormat = serializer == null ? null : serializer.getName();
            setSerializer(serializer, sFormat);
            }

        /**
         * Set the {@link Serializer} and serialization format name to use for
         * requests and response payloads.
         * <p>
         *
         * @param serializer  the {@link Serializer} to use to serialize request
         *                    and response payloads
         * @param sFormat     the serialization format name
         */
        public void setSerializer(Serializer serializer, String sFormat)
            {
            m_serializer       = serializer;
            m_serializerFormat = sFormat;
            }

        /**
         * Set the rpc deadline to use.
         *
         * @param cMillis  rpc deadline
         */
        public void setDeadline(long cMillis)
            {
            m_cDeadlineMillis = cMillis;
            }

        /**
         * Set the flag to indicate if the KeyAssociation check is deferred.
         *
         * @param f  the flag to indicate if the KeyAssociation check is deferred
         */
        public void setDeferKeyAssociationCheck(boolean f)
            {
            m_fDeferKeyAssociationCheck = f;
            }

        // ----- data members -----------------------------------------------

        /**
         * The underlying resource name.
         */
        private final String f_sName;

        /**
         * The gRPC {@link Channel} to use to connect to the server.
         */
        private final Channel f_channel;

        /**
         * The event dispatcher for the cache.
         */
        private final EventDispatcher f_dispatcher;

        /**
         * The server side scope name to use in requests.
         */
        private String m_sScopeName;

        /**
         * An optional {@link Executor} to use.
         */
        private Executor m_executor;

        /**
         * The {@link Serializer} to use for request and response payloads.
         */
        private Serializer m_serializer;

        /**
         * The name of the serialization format.
         */
        private String m_serializerFormat;

        /**
         * The request timeout.
         */
        private long m_cDeadlineMillis;

        /**
         * The flag to indicate if the KeyAssociation check is deferred.
         */
        private boolean m_fDeferKeyAssociationCheck;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The client to use to send requests.
     */
    protected final ClientType f_client;

    /**
     * The scope name to use for requests.
     */
    protected final String f_sScopeName;

    /**
     * The cache name.
     */
    protected final String f_sName;

    /**
     * The {@link Serializer} to use.
     */
    protected final Serializer f_serializer;

    /**
     * The serialization format.
     */
    protected final String f_sFormat;

    /**
     * A flag indicating whether this client has been released.
     */
    protected volatile boolean m_fReleased;

    /**
     * A flag indicating whether this client has been destroyed.
     */
    protected volatile boolean m_fDestroyed;

    /**
     * The {@link Executor} to use to dispatch events.
     */
    protected final Executor f_executor;

    /**
     * The event dispatcher for this cache.
     */
    protected final EventDispatcher f_dispatcher;

    /**
     * The service dependencies.
     */
    protected final Dependencies f_dependencies;

    /**
     * The flag to indicate if the KeyAssociation check is deferred.
     */
    private final boolean f_fDeferKeyAssociationCheck;
    }
