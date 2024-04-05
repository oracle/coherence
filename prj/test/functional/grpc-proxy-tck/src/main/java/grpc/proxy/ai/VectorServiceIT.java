/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.ai;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;

import com.oracle.coherence.ai.Converters;
import com.oracle.coherence.ai.VectorStore;

import com.oracle.coherence.ai.config.VectorStoreSessionConfig;

import com.oracle.coherence.ai.grpc.AddRequest;
import com.oracle.coherence.ai.grpc.GetVectorRequest;
import com.oracle.coherence.ai.grpc.OptionalVector;
import com.oracle.coherence.ai.grpc.RawVector;
import com.oracle.coherence.ai.grpc.StoreId;
import com.oracle.coherence.ai.grpc.UploadRequest;
import com.oracle.coherence.ai.grpc.UploadStart;
import com.oracle.coherence.ai.grpc.Vector;
import com.oracle.coherence.ai.grpc.VectorStoreService;
import com.oracle.coherence.ai.grpc.Vectors;

import com.oracle.coherence.ai.internal.BinaryVector;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.GetRequest;
import com.oracle.coherence.grpc.OptionalValue;
import com.oracle.coherence.grpc.PutRequest;
import com.oracle.coherence.grpc.Requests;

import com.oracle.coherence.grpc.proxy.common.NamedCacheService;

import com.oracle.coherence.testing.CheckJDK;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.Serializer;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.net.grpc.GrpcAcceptorController;

import grpc.proxy.BaseGrpcIT;
import grpc.proxy.TestStreamObserver;
import grpc.proxy.TestVectorStoreServiceProvider;

import io.grpc.stub.StreamObserver;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;

import java.util.List;
import java.util.Optional;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@BaseGrpcIT.WithNullScopeName
@SuppressWarnings("resource")
public class VectorServiceIT
        extends BaseGrpcIT
    {
    @BeforeAll
    static void checkJavaVersion()
        {
        Assumptions.assumeTrue(CheckJDK.computeVersion(System.getProperty("java.version")) >= 21,
                "Test skipped as the Java version is less than 21");
        }

    @BeforeEach
    void setupTestInfo(TestInfo info)
        {
        m_info = info;
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldAddVector() throws Exception
        {
        Optional<Session> optSession = Coherence.findSession(ensureScopeName(Coherence.DEFAULT_SCOPE));
        assertThat(optSession.isPresent(), is(true));

        Session                          session    = optSession.get();
        NamedCache<String, BinaryVector> cache      = session.getCache("test-vectors");
        Serializer                       serializer = cache.getService().getSerializer();
        String                           sFormat    = serializer.getName();
        ReadBuffer                       bufVector  = Converters.readBufferFromFloats(1.0f, 2.0f, 3.0f);
        BinaryVector                     vector     = new BinaryVector(null, sFormat, bufVector);
        ByteString                       key        = BinaryHelper.toByteString("key-1", serializer);

        TestStreamObserver<BytesValue> observerPut = new TestStreamObserver<>();
        NamedCacheService              service     = createCacheService();
        ByteString                     value       = BinaryHelper.toByteString(vector, serializer);
        PutRequest                     requestPut  = Requests.put(Coherence.DEFAULT_SCOPE, cache.getCacheName(), sFormat, key, value);

        service.put(requestPut, observerPut);

        assertThat(observerPut.await(1, TimeUnit.MINUTES), is(true));
        observerPut.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        TestStreamObserver<OptionalValue> observerGet = new TestStreamObserver<>();
        GetRequest                    requestGet  = Requests.get(Coherence.DEFAULT_SCOPE, cache.getCacheName(), sFormat, key);

        service.get(requestGet, observerGet);

        assertThat(observerGet.await(1, TimeUnit.MINUTES), is(true));
        observerGet.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        OptionalValue optionalValue = observerGet.valueAt(0);
        BinaryVector  vectorResult  = BinaryHelper.fromByteString(optionalValue.getValue(), serializer);
        assertThat(vectorResult, is(notNullValue()));
        assertThat(vectorResult.getVector(), is(bufVector));
        }


    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddRawVector(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        NamedCache<String, BinaryVector> cache  = ensureEmptyCache(sScope);
        String                           sName  = cache.getCacheName();
        ByteBuffer                       buffer = Converters.bufferFromFloats(1.0f, 2.0f, 3.0f);
        ByteString                       vector = ByteString.copyFrom(buffer);
        ByteString                       key    = toByteString("key-1", serializer);

        AddRequest request = createAddRequest(sScope, sName, serializerName,
                createRawVector(vector, key, null));

        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        VectorStoreService         service = ensureVectorService();

        service.add(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(cache.size(), is(1));
        BinaryVector binVector = cache.get("key-1");
        assertThat(binVector, is(notNullValue()));
        assertThat(binVector.getVector().toByteArray(), is(vector.toByteArray()));
        assertThat(binVector.getMetadata().isPresent(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddRawVectorWithMetadata(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        NamedCache<String, BinaryVector> cache    = ensureEmptyCache(sScope);
        String                           sName    = cache.getCacheName();
        ByteBuffer                       buffer   = Converters.bufferFromFloats(1.0f, 2.0f, 3.0f);
        ByteString                       vector   = ByteString.copyFrom(buffer);
        ByteString                       key      = toByteString("key-1", serializer);
        ByteString                       metadata = toByteString("meta-1", serializer);

        AddRequest request = createAddRequest(sScope, sName, serializerName,
                createRawVector(vector, key, metadata));

        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        VectorStoreService         service = ensureVectorService();

        service.add(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(cache.size(), is(1));
        BinaryVector binVector = cache.get("key-1");
        assertThat(binVector, is(notNullValue()));
        assertThat(binVector.getVector().toByteArray(), is(vector.toByteArray()));
        Optional<ReadBuffer> optional = binVector.getMetadata();
        assertThat(optional.isPresent(), is(true));
        assertThat(optional.get().toByteArray(), is(metadata.toByteArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldStreamRawVector(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        NamedCache<String, BinaryVector> cache  = ensureEmptyCache(sScope);
        String                           sName  = cache.getCacheName();
        ByteBuffer                       buffer = Converters.bufferFromFloats(1.0f, 2.0f, 3.0f);
        ByteString                       vector = ByteString.copyFrom(buffer);
        ByteString                       key    = toByteString("key-1", serializer);

        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        VectorStoreService        service = ensureVectorService();

        StreamObserver<UploadRequest> upload = service.upload(observer);

        upload.onNext(createUploadInit(sScope, sName, serializerName));
        upload.onNext(createUploadVector(createRawVector(vector, key, null)));
        upload.onCompleted();
        
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(cache.size(), is(1));
        BinaryVector binVector = cache.get("key-1");
        assertThat(binVector, is(notNullValue()));
        assertThat(binVector.getVector().toByteArray(), is(vector.toByteArray()));
        assertThat(binVector.getMetadata().isPresent(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldStreamRawVectorWithMetadata(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        NamedCache<String, BinaryVector> cache    = ensureEmptyCache(sScope);
        String                           sName    = cache.getCacheName();
        ByteBuffer                       buffer   = Converters.bufferFromFloats(1.0f, 2.0f, 3.0f);
        ByteString                       vector   = ByteString.copyFrom(buffer);
        ByteString                       key      = toByteString("key-1", serializer);
        ByteString                       metadata = toByteString("meta-1", serializer);

        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        VectorStoreService        service = ensureVectorService();

        StreamObserver<UploadRequest> upload = service.upload(observer);

        upload.onNext(createUploadInit(sScope, sName, serializerName));
        upload.onNext(createUploadVector(createRawVector(vector, key, metadata)));
        upload.onCompleted();

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(cache.size(), is(1));
        BinaryVector binVector = cache.get("key-1");
        assertThat(binVector, is(notNullValue()));
        assertThat(binVector.getVector().toByteArray(), is(vector.toByteArray()));

        Optional<ReadBuffer> optional = binVector.getMetadata();
        assertThat(optional.isPresent(), is(true));
        assertThat(optional.get().toByteArray(), is(metadata.toByteArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetExistingVectorWithMetadata(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String  sName     = m_info.getDisplayName();
        float[] floats    = new float[]{1.0f, 2.0f, 3.0f};
        String  sKey      = "one";
        String  sMetadata = "one-metadata";

        Optional<Session> optSession = Coherence.findSession(ensureScopeName(sScope));
        assertThat(optSession.isPresent(), is(true));

        // Add a vector - the metadata will be serialized with the serializer of the underlying service,
        // which may be different to that of the test client.
        // One of the points of the tests it that the client can get the metadata correctly
        VectorStore<float[], String, String> store = VectorStore.ofFloats(sName, optSession.get());
        store.add(sKey, floats, sMetadata);

        TestStreamObserver<OptionalVector> observer = new TestStreamObserver<>();
        VectorStoreService                 service  = ensureVectorService();

        GetVectorRequest request = GetVectorRequest.newBuilder()
                .setStoreId(createStoreId(sScope, sName, serializerName))
                .setKey(toByteString(sKey, serializer))
                .build();

        service.get(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        List<OptionalVector> list = observer.values();
        assertThat(list.size(), is(1));
        OptionalVector result = list.get(0);
        assertThat(result, is(notNullValue()));
        assertThat(result.getPresent(), is(true));

        Vector vector = result.getVector();
        assertThat(vector, is(notNullValue()));

        assertThat(vector.hasMetadata(), is(true));
        Object     oMetadata = BinaryHelper.fromByteString(vector.getMetadata(), serializer);
        assertThat(oMetadata, is(sMetadata));

        assertThat(vector.hasRawVector(), is(true));
        RawVector rawVector    = vector.getRawVector();
        float[]   floatsResult = Converters.floatsFromByteBuffer(rawVector.getData().asReadOnlyByteBuffer());
        assertThat(floatsResult, is(floats));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetExistingVectorWithoutMetadata(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String  sName  = m_info.getDisplayName();
        float[] floats = new float[]{1.0f, 2.0f, 3.0f};
        String  sKey   = "one";

        Optional<Session> optSession = Coherence.findSession(ensureScopeName(sScope));
        assertThat(optSession.isPresent(), is(true));

        VectorStore<float[], String, String> store = VectorStore.ofFloats(sName, optSession.get());
        store.add(sKey, floats);

        TestStreamObserver<OptionalVector> observer = new TestStreamObserver<>();
        VectorStoreService                 service  = ensureVectorService();

        GetVectorRequest request = GetVectorRequest.newBuilder()
                .setStoreId(createStoreId(sScope, sName, serializerName))
                .setKey(toByteString(sKey, serializer))
                .build();

        service.get(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        List<OptionalVector> list = observer.values();
        assertThat(list.size(), is(1));
        OptionalVector result = list.get(0);
        assertThat(result, is(notNullValue()));
        assertThat(result.getPresent(), is(true));

        Vector vector = result.getVector();
        assertThat(vector, is(notNullValue()));

        assertThat(vector.hasMetadata(), is(false));

        assertThat(vector.hasRawVector(), is(true));
        RawVector rawVector    = vector.getRawVector();
        float[]   floatsResult = Converters.floatsFromByteBuffer(rawVector.getData().asReadOnlyByteBuffer());
        assertThat(floatsResult, is(floats));
        }

    // ----- helper methods -------------------------------------------------

    protected StoreId createStoreId(String sScope, String sName, String serializerName)
        {
        StoreId.Builder storeIdBuilder = StoreId.newBuilder()
                .setFormat(serializerName)
                .setStore(sName);

        if (sScope != null)
            {
            storeIdBuilder.setScope(sScope);
            }

        return storeIdBuilder.build();
        }

    protected AddRequest createAddRequest(String sScope, String sName, String serializerName, Vector... vectors)
        {
        return AddRequest.newBuilder()
            .setStoreId(createStoreId(sScope, sName, serializerName))
            .setVectors(Vectors.newBuilder().addAllVector(List.of(vectors)).build())
            .build();
        }
    
    protected UploadRequest createUploadInit(String sScope, String sName, String serializerName)
        {
        return UploadRequest.newBuilder()
                .setStart(UploadStart.newBuilder()
                        .setStoreId(createStoreId(sScope, sName, serializerName))
                        .build())
                .build();
        }
    
    protected UploadRequest createUploadVector(Vector... vectors)
        {
        return UploadRequest.newBuilder()
                .setVectors(Vectors.newBuilder()
                        .addAllVector(List.of(vectors))
                        .build())
                .build();
        }
    
    protected Vector createRawVector(ByteString vector, ByteString key, ByteString metadata)
        {
        Vector.Builder builder = Vector.newBuilder()
                .setRawVector(RawVector.newBuilder().setData(vector).build());

        if (key != null)
            {
            builder.setKey(key);
            }

        if (metadata != null)
            {
            builder.setMetadata(metadata);
            }

        return builder.build();
        }

    protected VectorStoreService ensureVectorService()
        {
        VectorStoreService service = m_vectorService;
        if (m_vectorService == null)
            {
            GrpcAcceptorController                 controller = GrpcAcceptorController.discoverController();
            VectorStoreService.DefaultDependencies deps       = new VectorStoreService.DefaultDependencies(controller.getServerType());

            deps.setConfigurableCacheFactorySupplier(this::ensureCCF);
            Optional<TestVectorStoreServiceProvider> optional = TestVectorStoreServiceProvider.getProvider();
            assertThat("Cannot find a TestVectorStoreServiceProvider instance, are you running this test from the TCK module instead of from one of the specific Netty or Helidon test modules",
                    optional.isPresent(), is(true));
            service = m_vectorService = optional.get().getService(deps);
            }
        return service;
        }

    @Override
    protected String ensureScopeName(String sName)
        {
        return sName == null ? VectorStoreSessionConfig.SCOPE_NAME : sName;
        }

    protected <K> NamedCache<K, BinaryVector> ensureEmptyCache(String sScope)
        {
        return ensureEmptyCache(sScope, m_info.getDisplayName(), null);
        }
    
    // ----- data members ---------------------------------------------------

    protected VectorStoreService m_vectorService;
    
    protected TestInfo m_info;
    }
