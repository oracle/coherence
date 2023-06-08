/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.UnsafeByteOperations;
import com.oracle.coherence.common.base.Classes;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.OptionalValue;
import com.oracle.coherence.grpc.SimpleDaemonPoolExecutor;

import com.tangosol.coherence.config.Config;

import com.tangosol.io.NamedSerializerFactory;
import com.tangosol.io.Serializer;

import com.tangosol.net.events.EventDispatcher;

import com.tangosol.net.grpc.GrpcDependencies;

import com.tangosol.util.ExternalizableHelper;
import io.grpc.Channel;

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
public abstract class BaseGrpcClient<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates an {@link AsyncNamedCacheClient} from the specified
     * {@link AsyncNamedCacheClient.Dependencies}.
     *
     * @param dependencies  the {@link AsyncNamedCacheClient.Dependencies} to configure this
     *                      {@link AsyncNamedCacheClient}.
     */
    public BaseGrpcClient(Dependencies dependencies)
        {
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
        }

    // ----- helper methods -------------------------------------------------

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
        return !m_fReleased && !m_fDestroyed;
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
    protected static String getDefaultSerializerFormat()
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
    protected BytesValue toBytesValue(Object obj)
        {
        return BytesValue.of(toByteString(obj));
        }

    /**
     * A utility method to serialize an Object to {@link ByteString}.
     *
     * @param obj  the object to convert to {@link ByteString}.
     *
     * @return the {@link ByteString} for the specified object
     */
    protected ByteString toByteString(Object obj)
        {
        return UnsafeByteOperations.unsafeWrap(ExternalizableHelper.toBinary(obj, f_serializer).toByteBuffer());
        }

    /**
     * A utility method to deserialize an {@link OptionalValue} to an Object.
     * <p>
     * If no value is present in the {@link OptionalValue} then the default
     * value is returned.
     *
     * @param optional      the {@link OptionalValue} to deserialize an Object.
     * @param defaultValue  the value to return if the {link @OptionalValue} does
     *                      not contain a value
     *
     * @return an object from the specified {@link BytesValue}
     */
    protected V valueFromOptionalValue(OptionalValue optional, V defaultValue)
        {
        if (optional.getPresent())
            {
            return fromByteString(optional.getValue());
            }
        return defaultValue;
        }

    /**
     * A utility method to deserialize a {@link BytesValue} to an Object.
     *
     * @param bv  the {@link BytesValue} to deserialize an Object.
     *
     * @return an object from the specified {@link BytesValue}
     */
    protected V valueFromBytesValue(BytesValue bv)
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
    protected <T> T fromBytesValue(BytesValue bytes)
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
    protected <T> T fromByteString(ByteString bytes)
        {
        return BinaryHelper.fromByteString(bytes, f_serializer);
        }

    /**
     * Create a failed {@link CompletableFuture}.
     *
     * @param t   the error to use
     * @param <T> the type of the future
     *
     * @return a failed {@link CompletableFuture}
     */
    protected  <T> CompletableFuture<T> failedFuture(Throwable t)
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
        }

    // ----- data members ---------------------------------------------------

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
    protected boolean m_fReleased;

    /**
     * A flag indicating whether this client has been destroyed.
     */
    protected boolean m_fDestroyed;

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
    }
