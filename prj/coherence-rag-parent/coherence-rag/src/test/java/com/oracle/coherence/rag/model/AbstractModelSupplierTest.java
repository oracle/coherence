/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import com.oracle.coherence.mp.config.CoherenceConfigSource;

import org.eclipse.microprofile.config.Config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AbstractModelSupplier} class.
 * <p/>
 * This test class validates the base model supplier functionality including caching,
 * configuration management, and model creation.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractModelSupplier")
class AbstractModelSupplierTest
    {
    @Mock
    private Config mockConfig;

    @Mock
    private CoherenceConfigSource mockCoherenceConfig;

    private TestModelSupplier supplier;

    @BeforeEach
    void setUp() throws Exception
        {
        supplier = new TestModelSupplier();
        
        // Inject mocks using reflection
        Field configField = AbstractModelSupplier.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(supplier, mockConfig);

        Field coherenceConfigField = AbstractModelSupplier.class.getDeclaredField("coherenceConfig");
        coherenceConfigField.setAccessible(true);
        coherenceConfigField.set(supplier, mockCoherenceConfig);
        }

    @Nested
    @DisplayName("Initialization")
    class InitializationTests
        {
        @Test
        @DisplayName("should initialize with default model name")
        void shouldInitializeWithDefaultModelName() throws Exception
            {
            when(mockConfig.getOptionalValue("test.model", String.class))
                .thenReturn(Optional.of("TestProvider/test-model"));

            // Call init method via reflection
            Method initMethod = AbstractModelSupplier.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(supplier);

            // Verify default model name is set
            Field defaultModelNameField = AbstractModelSupplier.class.getDeclaredField("defaultModelName");
            defaultModelNameField.setAccessible(true);
            ModelName defaultModelName = (ModelName) defaultModelNameField.get(supplier);

            assertThat(defaultModelName.fullName(), is("TestProvider/test-model"));
            }

        @Test
        @DisplayName("should use fallback when no config provided")
        void shouldUseFallbackWhenNoConfigProvided() throws Exception
            {
            when(mockConfig.getOptionalValue("test.model", String.class))
                .thenReturn(Optional.empty());

            // Call init method via reflection
            Method initMethod = AbstractModelSupplier.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(supplier);

            // Verify fallback model name is used
            Field defaultModelNameField = AbstractModelSupplier.class.getDeclaredField("defaultModelName");
            defaultModelNameField.setAccessible(true);
            ModelName defaultModelName = (ModelName) defaultModelNameField.get(supplier);

            assertThat(defaultModelName.fullName(), is("DefaultProvider/default-model"));
            }
        }

    @Nested
    @DisplayName("Model Retrieval")
    class ModelRetrievalTests
        {
        @BeforeEach
        void setUp() throws Exception
            {
            when(mockConfig.getOptionalValue("test.model", String.class))
                .thenReturn(Optional.of("TestProvider/test-model"));

            // Initialize supplier
            Method initMethod = AbstractModelSupplier.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(supplier);
            }

        @Test
        @DisplayName("should return default model with get()")
        void shouldReturnDefaultModelWithGet()
            {
            String model = supplier.get();

            assertThat(model, is("TestProvider/test-model"));
            assertThat(supplier.getCreateCallCount(), is(1));
            }

        @Test
        @DisplayName("should return specific model with get(String)")
        void shouldReturnSpecificModelWithGetString()
            {
            String model = supplier.get("OtherProvider/other-model");

            assertThat(model, is("OtherProvider/other-model"));
            assertThat(supplier.getCreateCallCount(), is(1));
            }

        @Test
        @DisplayName("should return specific model with get(ModelName)")
        void shouldReturnSpecificModelWithGetModelName()
            {
            ModelName modelName = new ModelName("ThirdProvider/third-model");
            String model = supplier.get(modelName);

            assertThat(model, is("ThirdProvider/third-model"));
            assertThat(supplier.getCreateCallCount(), is(1));
            }

        @Test
        @DisplayName("should cache model instances")
        void shouldCacheModelInstances()
            {
            // Get same model multiple times
            String model1 = supplier.get("CachedProvider/cached-model");
            String model2 = supplier.get("CachedProvider/cached-model");
            String model3 = supplier.get("CachedProvider/cached-model");

            assertThat(model1, is(sameInstance(model2)));
            assertThat(model2, is(sameInstance(model3)));
            assertThat(supplier.getCreateCallCount(), is(1)); // Only created once
            }

        @Test
        @DisplayName("should create different instances for different models")
        void shouldCreateDifferentInstancesForDifferentModels()
            {
            String model1 = supplier.get("Provider1/model1");
            String model2 = supplier.get("Provider2/model2");

            assertThat(model1, is(not(sameInstance(model2))));
            assertThat(supplier.getCreateCallCount(), is(2));
            }

        @Test
        @DisplayName("should handle concurrent access to same model")
        void shouldHandleConcurrentAccessToSameModel() throws Exception
            {
            int numThreads = 10;
            String[] results = new String[numThreads];
            Thread[] threads = new Thread[numThreads];

            for (int i = 0; i < numThreads; i++)
                {
                final int index = i;
                threads[i] = new Thread(() -> results[index] = supplier.get("Concurrent/model"));
                }

            // Start all threads
            for (Thread thread : threads)
                {
                thread.start();
                }

            // Wait for all threads to complete
            for (Thread thread : threads)
                {
                thread.join();
                }

            // Verify all results are the same instance
            for (int i = 1; i < numThreads; i++)
                {
                assertThat(results[i], is(sameInstance(results[0])));
                }

            // Verify create was called only once
            assertThat(supplier.getCreateCallCount(), is(1));
            }
        }

    @Nested
    @DisplayName("Configuration Management")
    class ConfigurationManagementTests
        {
        @Test
        @DisplayName("should determine default model name from config")
        void shouldDetermineDefaultModelNameFromConfig()
            {
            when(mockConfig.getOptionalValue("test.model", String.class))
                .thenReturn(Optional.of("ConfigProvider/config-model"));

            ModelName defaultModelName = supplier.defaultModelName();

            assertThat(defaultModelName.fullName(), is("ConfigProvider/config-model"));
            }

        @Test
        @DisplayName("should use default when config not available")
        void shouldUseDefaultWhenConfigNotAvailable()
            {
            when(mockConfig.getOptionalValue("test.model", String.class))
                .thenReturn(Optional.empty());

            ModelName defaultModelName = supplier.defaultModelName();

            assertThat(defaultModelName.fullName(), is("DefaultProvider/default-model"));
            }

        @Test
        @DisplayName("should handle multiple config requests")
        void shouldHandleMultipleConfigRequests()
            {
            when(mockConfig.getOptionalValue("test.model", String.class))
                .thenReturn(Optional.of("Provider1/model1"))
                .thenReturn(Optional.of("Provider2/model2"));

            ModelName defaultModelName1 = supplier.defaultModelName();
            ModelName defaultModelName2 = supplier.defaultModelName();

            assertThat(defaultModelName1.fullName(), is("Provider1/model1"));
            assertThat(defaultModelName2.fullName(), is("Provider2/model2"));
            }
        }

    @Nested
    @DisplayName("Cache Management")
    class CacheManagementTests
        {
        @Test
        @DisplayName("should provide access to cache internals")
        void shouldProvideAccessToCacheInternals() throws Exception
            {
            // Initialize supplier
            when(mockConfig.getOptionalValue("test.model", String.class))
                .thenReturn(Optional.of("TestProvider/test-model"));

            Method initMethod = AbstractModelSupplier.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(supplier);

            // Create some cached entries
            supplier.get("Provider1/model1");
            supplier.get("Provider2/model2");
            
            // Verify cache has entries
            Field mapModelField = AbstractModelSupplier.class.getDeclaredField("mapModel");
            mapModelField.setAccessible(true);
            ConcurrentMap<?, ?> cache = (ConcurrentMap<?, ?>) mapModelField.get(supplier);
            
            assertThat(cache.size(), is(2));
            assertThat(cache.containsKey(new ModelName("Provider1/model1")), is(true));
            assertThat(cache.containsKey(new ModelName("Provider2/model2")), is(true));
            }

        @Test
        @DisplayName("should handle cache with mixed access patterns")
        void shouldHandleCacheWithMixedAccessPatterns()
            {
            // Test mixed access patterns
            String model1 = supplier.get("Provider/model1");
            String model2 = supplier.get(new ModelName("Provider/model2"));
            String model3 = supplier.get("Provider/model3");
            String model1Again = supplier.get("Provider/model1");

            assertThat(model1, is(sameInstance(model1Again)));
            assertThat(supplier.getCreateCallCount(), is(3)); // Three unique models
            }
        }

    @Nested
    @DisplayName("Abstract Method Implementation")
    class AbstractMethodImplementationTests
        {
        @Test
        @DisplayName("should provide correct description")
        void shouldProvideCorrectDescription()
            {
            assertThat(supplier.description(), is("test"));
            }

        @Test
        @DisplayName("should provide correct default model")
        void shouldProvideCorrectDefaultModel()
            {
            assertThat(supplier.defaultModel(), is("DefaultProvider/default-model"));
            }

        @Test
        @DisplayName("should provide correct config property")
        void shouldProvideCorrectConfigProperty()
            {
            assertThat(supplier.configProperty(), is("test.model"));
            }

        @Test
        @DisplayName("should create models correctly")
        void shouldCreateModelsCorrectly()
            {
            ModelName modelName = new ModelName("TestProvider/test-model");
            String model = supplier.create(modelName);

            assertThat(model, is("TestProvider/test-model"));
            }
        }

    /**
     * Test implementation of AbstractModelSupplier for testing purposes.
     */
    private static class TestModelSupplier extends AbstractModelSupplier<String>
        {
        private int createCallCount = 0;

        @Override
        protected String description()
            {
            return "test";
            }

        @Override
        protected String defaultModel()
            {
            return "DefaultProvider/default-model";
            }

        @Override
        protected String configProperty()
            {
            return "test.model";
            }

        @Override
        public String create(ModelName modelName)
            {
            createCallCount++;
            return modelName.fullName();
            }

        public int getCreateCallCount()
            {
            return createCallCount;
            }
        }
    }
