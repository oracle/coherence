/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.UnsafeByteOperations;

import com.oracle.coherence.common.base.Classes;

import com.tangosol.io.NamedSerializerFactory;
import com.tangosol.io.Serializer;

import com.tangosol.net.grpc.GrpcDependencies;

import com.tangosol.util.ExternalizableHelper;

/**
 * A base implementation of a {@link ClientProtocol}.
 *
 * @param <DepsType>  the type of the dependencies
 * @param <Con>       the type of the {@link GrpcConnection}
 */
public abstract class BaseClientChannel<DepsType extends BaseGrpcClient.Dependencies, Con extends GrpcConnection>
        implements ClientProtocol
    {
    /**
     * Create a {@link BaseClientChannel}.
     *
     * @param dependencies  the dependencies to use
     * @param connection    the gRPC connection to use
     */
    protected BaseClientChannel(DepsType dependencies, Con connection)
        {
        f_dependencies = dependencies;
        f_connection   = connection;
        f_sScopeName   = dependencies.getScopeName().orElse(GrpcDependencies.DEFAULT_SCOPE);
        f_sFormat      = dependencies.getSerializerFormat()
                                     .orElseGet(() -> dependencies.getSerializer()
                                         .map(Serializer::getName)
                                         .orElseGet(BaseGrpcClient::getDefaultSerializerFormat));
        f_serializer   = dependencies.getSerializer().orElseGet(() -> createSerializer(f_sFormat));
        }

    @Override
    public void close()
        {
        f_connection.close();
        }

    @Override
    public int getVersion()
        {
        return f_connection.getProtocolVersion();
        }

    @Override
    public boolean isActive()
        {
        return f_connection.isConnected();
        }

    @Override
    public GrpcConnection getConnection()
        {
        return f_connection;
        }

    public DepsType getDependencies()
        {
        return f_dependencies;
        }

    // ----- helper methods -------------------------------------------------


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
     * Obtain the default {@link Serializer} to be used by the cache.
     *
     * @return  the default {@link Serializer} to be used by the cache
     */
    protected static Serializer createSerializer(String sFormat)
        {
        return NamedSerializerFactory.DEFAULT.getNamedSerializer(sFormat, Classes.getContextClassLoader());
        }

    // ----- data members ---------------------------------------------------

    /**
     * A constant void type.
     */
    protected static final Void VOID = null;

    /**
     * The {@link GrpcConnection} to use to send messages.
     */
    protected final Con f_connection;

    /**
     * The client dependencies.
     */
    protected final DepsType f_dependencies;
    /**
     * The {@link Serializer} to use.
     */
    protected final Serializer f_serializer;

    /**
     * The serialization format.
     */
    protected final String f_sFormat;

    /**
     * The name of the scope to use to process requests on the proxy.
     */
    protected final String f_sScopeName;
    }
