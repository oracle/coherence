/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.grpc;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import com.oracle.coherence.ai.internal.BinaryVectorStore;
import com.oracle.coherence.ai.Converters;
import com.oracle.coherence.ai.Vector.Key;
import com.oracle.coherence.ai.Vector.KeySequence;
import com.oracle.coherence.ai.VectorStore;

import com.oracle.coherence.ai.config.VectorStoreSessionConfig;
import com.oracle.coherence.ai.internal.BinaryVector;
import com.oracle.coherence.ai.internal.ConverterBinaryVectorStore;
import com.oracle.coherence.ai.internal.PassThruVectorStore;

import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.SafeStreamObserver;

import com.oracle.coherence.grpc.proxy.common.BaseGrpcServiceImpl;
import com.oracle.coherence.grpc.proxy.common.ResponseHandlers;

import com.oracle.coherence.io.json.JsonObject;
import com.oracle.coherence.io.json.JsonSerializer;
import com.tangosol.application.ContainerContext;
import com.tangosol.application.Context;

import com.tangosol.io.ReadBuffer;
import com.tangosol.io.Serializer;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A synchronous vector store service that performs work on the
 * gRPC request thread.
 */
public class SyncVectorStoreService
        extends BaseGrpcServiceImpl
        implements VectorStoreService
    {
    /**
     * Create an {@link SyncVectorStoreService}.
     *
     * @param deps  the {@link VectorStoreService.Dependencies dependencies} for the service
     */
    public SyncVectorStoreService(VectorStoreService.Dependencies deps)
        {
        super(deps, MBEAN_NAME, POOL_NAME);
        }

    @Override
    public void clear(ClearRequest request, StreamObserver<Empty> observer)
        {
        StreamObserver<Empty> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        try
            {
            if (!request.hasStoreId())
                {
                throw Status.INVALID_ARGUMENT
                        .withDescription(INVALID_STORE_ID_MESSAGE)
                        .asRuntimeException();
                }
            BinaryVectorStore store = getStore(request.getStoreId());
            store.clear();
            ResponseHandlers.handleUnary(Empty.getDefaultInstance(), safeObserver);
            }
        catch (Throwable error)
            {
            ResponseHandlers.handleError(error, safeObserver);
            }
        }

    @Override
    public void destroy(DestroyRequest request, StreamObserver<Empty> observer)
        {
        StreamObserver<Empty> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        try
            {
            if (!request.hasStoreId())
                {
                throw Status.INVALID_ARGUMENT
                        .withDescription(INVALID_STORE_ID_MESSAGE)
                        .asRuntimeException();
                }
            BinaryVectorStore store = getStore(request.getStoreId());
            store.destroy();
            ResponseHandlers.handleUnary(Empty.getDefaultInstance(), safeObserver);
            }
        catch (Throwable error)
            {
            ResponseHandlers.handleError(error, safeObserver);
            }
        }

    @Override
    public void add(AddRequest request, StreamObserver<Empty> observer)
        {
        StreamObserver<Empty> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        try
            {
            if (!request.hasStoreId())
                {
                throw Status.INVALID_ARGUMENT
                        .withDescription(INVALID_STORE_ID_MESSAGE)
                        .asRuntimeException();
                }
            StoreId           storeId = request.getStoreId();
            BinaryVectorStore store   = getStore(storeId);
            Vectors           vectors = request.getVectors();
            addVectors(store, storeId.getFormat(), vectors);
            ResponseHandlers.handleUnary(Empty.getDefaultInstance(), observer);
            }
        catch (Throwable error)
            {
            ResponseHandlers.handleError(error, safeObserver);
            }
        }

    @Override
    public StreamObserver<UploadRequest> upload(StreamObserver<Empty> observer)
        {
        return new Loader(observer);
        }

    @Override
    public void get(GetVectorRequest request, StreamObserver<OptionalVector> observer)
        {
        StreamObserver<OptionalVector> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        try
            {
            if (!request.hasStoreId())
                {
                throw Status.INVALID_ARGUMENT
                        .withDescription(INVALID_STORE_ID_MESSAGE)
                        .asRuntimeException();
                }
            StoreId           storeId = request.getStoreId();
            String            sFormat = storeId.getFormat();
            BinaryVectorStore store   = getStore(storeId);
            Binary            binKey  = BinaryHelper.toBinary(request.getKey());

            OptionalVector result = store.getVector(binKey)
                    .map(bv -> toRawVector(request.getKey(), bv, sFormat))
                    .map(this::optionalVector)
                    .orElse(optionalVector());

            ResponseHandlers.handleUnary(result, observer);
            }
        catch (Throwable error)
            {
            ResponseHandlers.handleError(error, safeObserver);
            }
        }

    @Override
    public void query(SimilarityQuery request, StreamObserver<QueryResult> observer)
        {
        StreamObserver<QueryResult> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        try
            {
            if (!request.hasStoreId())
                {
                throw Status.INVALID_ARGUMENT
                        .withDescription(INVALID_STORE_ID_MESSAGE)
                        .asRuntimeException();
                }
            BinaryVectorStore store      = getStore(request.getStoreId());
            JsonObject        jsonParams = BinaryHelper.fromByteString(request.getParameters(), JSON);

            safeObserver.onCompleted();
            }
        catch (Throwable error)
            {
            ResponseHandlers.handleError(error, safeObserver);
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Add {@link Vectors} to a {@link BinaryVectorStore}.
     *
     * @param store    the store to add the vectors to
     * @param sFormat  the serialization format used for the metadata
     * @param vectors  the vectors to add
     */
    protected void addVectors(BinaryVectorStore store, String sFormat, Vectors vectors)
        {
        KeySequence<?> sequence = Key.newSequence();
        for (Vector vector : vectors.getVectorList())
            {
            addVector(store, sFormat, vector, sequence);
            }
        }

    /**
     * Add {@link Vectors} to a {@link BinaryVectorStore}.
     *
     * @param store     the store to add the vectors to
     * @param sFormat   the serialization format used for the metadata
     * @param vectors   the vectors to add
     * @param sequence  the {@link KeySequence} to use to generate keys
     */
    protected void addVector(BinaryVectorStore store, String sFormat, Vector vectors, KeySequence<?> sequence)
        {
        Binary binMetadata = vectors.hasMetadata() ? BinaryHelper.toBinary(vectors.getMetadata()) : null;
        Binary binKey      = vectors.hasKey() ? BinaryHelper.toBinary(vectors.getKey()) : null;

        ReadBuffer buffVector;
        if (vectors.hasRawVector())
            {
            RawVector rawVector = vectors.getRawVector();
            buffVector = BinaryHelper.toBinary(rawVector.getData());
            }
        else if (vectors.hasFloatVector())
            {
            float[] floats = getFloats(vectors.getFloatVector());
            buffVector = Converters.readBufferFromFloats(floats);
            }
        else if (vectors.hasDoubleVector())
            {
            double[] doubles = getDoubles(vectors.getDoubleVector());
            buffVector = Converters.readBufferFromDoubles(doubles);
            }
        else if (vectors.hasIntVector())
            {
            int[] ints = getInts(vectors.getIntVector());
            buffVector = Converters.readBufferFromInts(ints);
            }
        else if (vectors.hasLongVector())
            {
            long[] longs = getLongs(vectors.getLongVector());
            buffVector = Converters.readBufferFromLongs(longs);
            }
        else
            {
            throw Status.INVALID_ARGUMENT
                    .withDescription(ADD_MISSING_VECTOR)
                    .asRuntimeException();
            }

        BinaryVector binaryVector = new BinaryVector(binMetadata, sFormat, buffVector);
        if (binKey == null)
            {
            store.addBinaryVector(binaryVector, sequence);
            }
        else
            {
            store.addBinaryVector(binaryVector, binKey);
            }
        }

    /**
     * Obtain an {@link VectorStore}.
     *
     * @param storeId  the identifier for the store
     *
     * @return the {@link NamedCache} with the specified name
     */
    protected BinaryVectorStore getStore(StoreId storeId)
        {
        String sScope;
        if (storeId.hasScope())
            {
            sScope = storeId.getScope();
            }
        else
            {
            sScope = VectorStoreSessionConfig.SCOPE_NAME;
            }
        return getStore(sScope, storeId.getStore(), storeId.getFormat());
        }

    /**
     * Obtain an {@link VectorStore}.
     *
     * @param sScope   the scope name to use to obtain the CCF to get the cache from
     * @param sName    the name of the store
     * @param sFormat  the serialization format used by the request
     *
     * @return the {@link NamedCache} with the specified name
     */
    protected BinaryVectorStore getStore(String sScope, String sName, String sFormat)
        {
        if (sName == null || sName.trim().isEmpty())
            {
            throw Status.INVALID_ARGUMENT
                    .withDescription(INVALID_STORE_NAME_MESSAGE)
                    .asRuntimeException();
            }

        Context                  context          = f_dependencies.getContext().orElse(null);
        ContainerContext         containerContext = context == null ? null : context.getContainerContext();
        ConfigurableCacheFactory ccf              = getCCF(sScope);

        if (containerContext != null)
            {
            return containerContext.runInDomainPartitionContext(createCallable(ccf, sName, sFormat));
            }
        else
            {
            try
                {
                return createCallable(ccf, sName, sFormat).call();
                }
            catch (Exception e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }
        }

    /**
     * Create a {@link Callable} to obtain the {@link BinaryVectorStore}.
     * <p/>
     * A {@link Callable} is used because inside WLS we may need to execute this inside a container context.
     *
     * @param ccf      the {@link ConfigurableCacheFactory} to use
     * @param sName    the name of the vector store
     * @param sFormat  the serializer name
     *
     * @return a {@link Callable} that will return a {@link BinaryVectorStore}
     */
    @SuppressWarnings("unchecked")
    private Callable<BinaryVectorStore> createCallable(ConfigurableCacheFactory ccf, String sName, String sFormat)
        {
        return () ->
            {
            PassThruVectorStore store             = new PassThruVectorStore(ccf, sName);
            Service             service           = store.getService();
            Serializer          serializerStore   = service.getSerializer();
            String              sFormatStore      = serializerStore.getName();
            Serializer          serializerRequest = getSerializer(sFormat, sFormatStore, () -> serializerStore, service::getContextClassLoader);

            if (ExternalizableHelper.isSerializerCompatible(serializerStore, serializerRequest))
                {
                return store;
                }

            CacheService              cacheService              = (CacheService) service;
            BackingMapManagerContext  context                   = cacheService.getBackingMapManager().getContext();
            Converter<Binary, Object> converterToObj            = bin -> ExternalizableHelper.fromBinary(bin, serializerRequest);
            Converter<Object, Binary> converterToBin            = obj -> ExternalizableHelper.toBinary(obj, serializerRequest);
            Converter<Binary, Binary> convertKeyToInternal      = new BinaryConverter(converterToObj, context.getKeyToInternalConverter());
            Converter<Binary, Binary> convertValueToInternal    = new BinaryConverter(converterToObj, context.getValueToInternalConverter());
            Converter<Binary, Binary> convertKeyFromInternal    = new BinaryConverter(context.getKeyFromInternalConverter(), converterToBin);
            Converter<Binary, Binary> convertValueFromInternal  = new BinaryConverter(context.getValueFromInternalConverter(), converterToBin);
            Converter<Binary, Binary> converterFromBinaryVector = new BinaryVectorConverter(context.getValueFromInternalConverter(), converterToBin, sFormat);
            return new ConverterBinaryVectorStore(store, convertKeyToInternal, convertValueToInternal,
                    convertKeyFromInternal, convertValueFromInternal, converterFromBinaryVector);
            };
        }

    /**
     * Create a {@link RawVector}.
     *
     * @param key        the key of the vector
     * @param binVector  the {@link BinaryVector} containing the vector data and optional metadata
     *
     * @return a {@link RawVector} containing the key, vector data and optional metadata
     */
    protected Vector toRawVector(ByteString key, BinaryVector binVector, String sFormat)
        {
        Vector.Builder builder = Vector.newBuilder()
                .setKey(key);

        builder.setRawVector(RawVector.newBuilder()
                .setData(BinaryHelper.toByteString(binVector.getVector().toBinary()))
                .build());

        binVector.getMetadata()
                .flatMap(bin -> ensureConverter(binVector.getFormat(), sFormat)
                        .convert(bin.toBinary()))
                        .ifPresent(bin -> builder.setMetadata(BinaryHelper.toByteString(bin)));

        return builder.build();
        }

    protected OptionalVector optionalVector(Vector vector)
        {
        return OptionalVector.newBuilder().setVector(vector).setPresent(true).build();
        }

    protected OptionalVector optionalVector()
        {
        return OptionalVector.newBuilder().setPresent(false).build();
        }

    /**
     * Return an array of {@code float} from a {@link FloatVector}.
     *
     * @param vector  the {@link FloatVector} to obtain the array from
     *
     * @return the array of {@code float} from the {@link FloatVector}
     */
    protected float[] getFloats(FloatVector vector)
        {
        float[] an = new float[vector.getDataCount()];
        for (int i = 0; i < an.length; i++)
            {
            an[i] = vector.getData(i);
            }
        return an;
        }

    /**
     * Return an array of {@code double} from a {@link DoubleVector}.
     *
     * @param vector  the {@link DoubleVector} to obtain the array from
     *
     * @return the array of {@code double} from the {@link DoubleVector}
     */
    protected double[] getDoubles(DoubleVector vector)
        {
        double[] an = new double[vector.getDataCount()];
        for (int i = 0; i < an.length; i++)
            {
            an[i] = vector.getData(i);
            }
        return an;
        }

    /**
     * Return an array of {@code int} from a {@link IntVector}.
     *
     * @param vector  the {@link IntVector} to obtain the array from
     *
     * @return the array of {@code int} from the {@link IntVector}
     */
    protected int[] getInts(IntVector vector)
        {
        int[] an = new int[vector.getDataCount()];
        for (int i = 0; i < an.length; i++)
            {
            an[i] = vector.getData(i);
            }
        return an;
        }

    /**
     * Return an array of {@code long} from a {@link LongVector}.
     *
     * @param vector  the {@link LongVector} to obtain the array from
     *
     * @return the array of {@code long} from the {@link LongVector}
     */
    protected long[] getLongs(LongVector vector)
        {
        long[] an = new long[vector.getDataCount()];
        for (int i = 0; i < an.length; i++)
            {
            an[i] = vector.getData(i);
            }
        return an;
        }

    /**
     * Create a {@link Converter} that converts between a binary serialized with one serializer
     * into a binary serialized with another serializer.
     *
     * @param sFrom  the name of the serializer used to serialize the original value
     * @param sTo    the name of the serializer to use to serialize the returned binary
     *
     * @return a {@link Converter} that converts between a binary serialized with one serializer
     *         into a binary serialized with another serializer
     */
    protected Converter<Binary, Optional<Binary>> ensureConverter(String sFrom, String sTo)
        {
        String sKey = sFrom + "->" + sTo;
        return f_mapConverter.computeIfAbsent(sKey, s ->
            {
            if (sFrom.equals(sTo))
                {
                return bin -> bin == null || bin.length() == 0 ? Optional.empty() : Optional.of(bin);
                }
            Serializer serFrom = getSerializer(sFrom, Classes.getContextClassLoader());
            Serializer serTo   = getSerializer(sTo, Classes.getContextClassLoader());
            return bin ->
                {
                Object o = ExternalizableHelper.fromBinary(bin, serFrom);
                if (o == null)
                    {
                    return Optional.empty();
                    }
                return Optional.of(ExternalizableHelper.toBinary(o, serTo));
                };
            });
        }

    // ----- inner class: BinaryConverter -----------------------------------

    /**
     * A converter to convert a {@link Binary} serialized with one serializer,
     * into a {@link Binary} serialized with another serializer.
     */
    protected static class BinaryConverter
            implements Converter<Binary, Binary>
        {
        public BinaryConverter(Converter<Binary, Object> toObject, Converter<Object, Binary> toBinary)
            {
            f_toObject = toObject;
            f_toBinary = toBinary;
            }

        @Override
        public Binary convert(Binary value)
            {
            return f_toBinary.convert(f_toObject.convert(value));
            }

        private final Converter<Binary, Object> f_toObject;

        private final Converter<Object, Binary> f_toBinary;
        }

    // ----- inner class: BinaryMetadataConverter ---------------------------

    /**
     * A converter to convert a {@link BinaryVector} serialized with one serializer,
     * into a {@link BinaryVector} serialized with another serializer and convert
     * the metadata.
     */
    protected static class BinaryVectorConverter
            implements Converter<Binary, Binary>
        {
        public BinaryVectorConverter(Converter<Binary, Object> toObject, Converter<Object, Binary> toBinary, String sFormat)
            {
            f_toObject = toObject;
            f_toBinary = toBinary;
            f_sFormat  = sFormat;
            }

        @Override
        public Binary convert(Binary value)
            {
            BinaryVector vector      = (BinaryVector) f_toObject.convert(value);
            Object       oMetadata   = vector.getMetadata().map(b -> f_toObject.convert(b.toBinary())).orElse(Binary.NO_BINARY);
            Binary       binMetadata = f_toBinary.convert(oMetadata);
            return f_toBinary.convert(new BinaryVector(binMetadata, f_sFormat, vector.getVector()));
            }

        private final Converter<Binary, Object> f_toObject;

        private final Converter<Object, Binary> f_toBinary;

        private final String f_sFormat;
        }

    // ----- inner class: Loader --------------------------------------------

    /**
     * A {@link StreamObserver} used to receive {@link UploadRequest} messages.
     */
    protected class Loader
            implements StreamObserver<UploadRequest>
        {
        /**
         * Create a {@link Loader}.
         *
         * @param observer  the {@link StreamObserver} to receive responses
         */
        public Loader(StreamObserver<Empty> observer)
            {
            m_observer = SafeStreamObserver.ensureSafeObserver(observer);
            }

        @Override
        public void onNext(UploadRequest request)
            {
            try
                {
                if (request.hasStart())
                    {
                    initialise(request.getStart());
                    }
                else
                    {
                    if (m_store == null)
                        {
                        throw Status.FAILED_PRECONDITION
                                .withDescription(MISSING_UPLOAD_START)
                                .asRuntimeException();
                        }
                    if (request.hasVectors())
                        {
                        addVectors(m_store, m_sFormat, request.getVectors());
                        }
                    }
                }
            catch (Throwable error)
                {
                Logger.err(error);
                ResponseHandlers.handleError(error, m_observer);
                }
            }

        @Override
        public void onError(Throwable error)
            {
            Logger.err(error);
            }

        @Override
        public void onCompleted()
            {
            m_observer.onNext(Empty.getDefaultInstance());
            m_observer.onCompleted();
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Initialise this loader.
         *
         * @param start  the {@link UploadStart} message.
         */
        protected void initialise(UploadStart start)
            {
            StoreId storeId = start.getStoreId();
            m_store   = getStore(storeId);
            m_sFormat = storeId.getFormat();
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link StreamObserver} to receive responses.
         */
        private final StreamObserver<Empty> m_observer;

        /**
         * The vector store to upload vectors to.
         */
        private BinaryVectorStore m_store;

        /**
         * The serialization format used by the request.
         */
        private String m_sFormat;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The JSON serializer to use.
     */
    public static final JsonSerializer JSON = new JsonSerializer();

    /**
     * The pool name for this service.
     */
    public static final String POOL_NAME = "GrpcVectorStoreProxy";

    /**
     * The name to use for the management MBean.
     */
    public static final String MBEAN_NAME = "type=" + POOL_NAME;

    // ----- error messages -------------------------------------------------
    
    public static final String INVALID_STORE_ID_MESSAGE = "invalid request, store id cannot be null or empty";

    public static final String INVALID_STORE_NAME_MESSAGE = "invalid request, store name cannot be null or empty";

    public static final String MISSING_UPLOAD_START = "Received an upload message before an upload start message";

    public static final String ADD_MISSING_VECTOR = "Received a vector store message without a vector";

    // ----- data members ---------------------------------------------------

    /**
     * A map of converters between different formats.
     */
    private final Map<String, Converter<Binary, Optional<Binary>>> f_mapConverter = new ConcurrentHashMap<>();
    }
