/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import com.oracle.coherence.mp.config.CoherenceConfigSource;
import com.oracle.coherence.rag.ModelProvider;
import com.oracle.coherence.rag.util.CdiHelper;

import dev.langchain4j.model.embedding.EmbeddingModel;

import org.eclipse.microprofile.config.Config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EmbeddingModelSupplier} class.
 * <p/>
 * This test class validates the embedding model supplier functionality including
 * model creation, provider resolution, local ONNX fallback, and caching behavior.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmbeddingModelSupplier")
class EmbeddingModelSupplierTest
    {
    @Mock
    private Config mockConfig;

    @Mock
    private CoherenceConfigSource mockCoherenceConfig;

    @Mock
    private ModelProvider mockProvider;

    @Mock
    private EmbeddingModel mockEmbeddingModel;

    @Mock
    private LocalOnnxEmbeddingModel mockLocalOnnxEmbeddingModel;

    private EmbeddingModelSupplier supplier;

    @BeforeEach
    void setUp() throws Exception
        {
        supplier = new EmbeddingModelSupplier();
        
        // Inject mocks using reflection
        Field configField = AbstractModelSupplier.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(supplier, mockConfig);

        Field coherenceConfigField = AbstractModelSupplier.class.getDeclaredField("coherenceConfig");
        coherenceConfigField.setAccessible(true);
        coherenceConfigField.set(supplier, mockCoherenceConfig);
        }

    @Test
    @DisplayName("should return correct description")
    void shouldReturnCorrectDescription()
        {
        String description = supplier.description();
        assertThat(description, is("embedding"));
        }

    @Test
    @DisplayName("should return correct default model")
    void shouldReturnCorrectDefaultModel()
        {
        String defaultModel = supplier.defaultModel();
        assertThat(defaultModel, is("-/all-MiniLM-L6-v2"));
        }

    @Test
    @DisplayName("should return correct config property")
    void shouldReturnCorrectConfigProperty()
        {
        String configProperty = supplier.configProperty();
        assertThat(configProperty, is("model.embedding"));
        }

    @Test
    @DisplayName("should create local ONNX model when provider is dash")
    void shouldCreateLocalOnnxModelWhenProviderIsDash()
        {
        try (MockedStatic<LocalOnnxEmbeddingModel> mockedLocalOnnx = mockStatic(LocalOnnxEmbeddingModel.class))
            {
            mockedLocalOnnx.when(() -> LocalOnnxEmbeddingModel.createDefault(any(ModelName.class)))
                    .thenReturn(mockLocalOnnxEmbeddingModel);

            ModelName modelName = new ModelName("-/all-MiniLM-L6-v2");
            EmbeddingModel result = supplier.create(modelName);

            assertThat(result, is(sameInstance(mockLocalOnnxEmbeddingModel)));
            mockedLocalOnnx.verify(() -> LocalOnnxEmbeddingModel.createDefault(modelName));
            }
        }

    @Test
    @DisplayName("should use provider when available")
    void shouldUseProviderWhenAvailable()
        {
        try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
            {
            when(mockProvider.getEmbeddingModel("test-model")).thenReturn(mockEmbeddingModel);
            mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, "TestProvider"))
                    .thenReturn(mockProvider);

            ModelName modelName = new ModelName("TestProvider/test-model");
            EmbeddingModel result = supplier.create(modelName);

            assertThat(result, is(sameInstance(mockEmbeddingModel)));
            verify(mockProvider).getEmbeddingModel("test-model");
            }
        }

    @Test
    @DisplayName("should fallback to local ONNX when provider not found")
    void shouldFallbackToLocalOnnxWhenProviderNotFound()
        {
        try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class);
             MockedStatic<LocalOnnxEmbeddingModel> mockedLocalOnnx = mockStatic(LocalOnnxEmbeddingModel.class))
            {
            mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, "UnknownProvider"))
                    .thenReturn(null);
            mockedLocalOnnx.when(() -> LocalOnnxEmbeddingModel.create(any(ModelName.class)))
                    .thenReturn(mockLocalOnnxEmbeddingModel);

            ModelName modelName = new ModelName("UnknownProvider/test-model");
            EmbeddingModel result = supplier.create(modelName);

            assertThat(result, is(sameInstance(mockLocalOnnxEmbeddingModel)));
            mockedLocalOnnx.verify(() -> LocalOnnxEmbeddingModel.create(modelName));
            }
        }

    @Test
    @DisplayName("should cache embedding model instances")
    void shouldCacheEmbeddingModelInstances() throws Exception
        {
        when(mockConfig.getOptionalValue("model.embedding", String.class))
            .thenReturn(Optional.of("-/all-MiniLM-L6-v2"));

        // Initialize supplier
        Method initMethod = AbstractModelSupplier.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(supplier);

        try (MockedStatic<LocalOnnxEmbeddingModel> mockedLocalOnnx = mockStatic(LocalOnnxEmbeddingModel.class))
            {
            mockedLocalOnnx.when(() -> LocalOnnxEmbeddingModel.createDefault(any(ModelName.class)))
                    .thenReturn(mockLocalOnnxEmbeddingModel);

            // Get same model multiple times
            EmbeddingModel model1 = supplier.get("-/cached-model");
            EmbeddingModel model2 = supplier.get("-/cached-model");
            EmbeddingModel model3 = supplier.get("-/cached-model");

            assertThat(model1, is(sameInstance(model2)));
            assertThat(model2, is(sameInstance(model3)));

            // Verify LocalOnnxEmbeddingModel.createDefault was called only once
            mockedLocalOnnx.verify(() -> LocalOnnxEmbeddingModel.createDefault(any(ModelName.class)), times(1));
            }
        }

    @Test
    @DisplayName("should determine default model name from config")
    void shouldDetermineDefaultModelNameFromConfig()
        {
        when(mockConfig.getOptionalValue("model.embedding", String.class))
            .thenReturn(Optional.of("custom/embedding-model"));

        ModelName defaultModelName = supplier.defaultModelName();

        assertThat(defaultModelName.fullName(), is("custom/embedding-model"));
        }

    @Test
    @DisplayName("should fall back to default when config not available")
    void shouldFallBackToDefaultWhenConfigNotAvailable()
        {
        when(mockConfig.getOptionalValue("model.embedding", String.class))
            .thenReturn(Optional.empty());

        ModelName defaultModelName = supplier.defaultModelName();

        assertThat(defaultModelName.fullName(), is("-/all-MiniLM-L6-v2"));
        }

    @Test
    @DisplayName("should handle various provider formats")
    void shouldHandleVariousProviderFormats()
        {
        try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class);
             MockedStatic<LocalOnnxEmbeddingModel> mockedLocalOnnx = mockStatic(LocalOnnxEmbeddingModel.class))
            {
            mockedCdi.when(() -> CdiHelper.getNamedBean(eq(ModelProvider.class), any(String.class)))
                    .thenReturn(null);
            mockedLocalOnnx.when(() -> LocalOnnxEmbeddingModel.create(any(ModelName.class)))
                    .thenReturn(mockLocalOnnxEmbeddingModel);

            // Test various provider formats
            String[] testModels = {
                "openai/text-embedding-ada-002",
                "oci/cohere.embed-multilingual-v3.0",
                "sentence-transformers/all-mpnet-base-v2"
            };

            for (String modelString : testModels)
                {
                ModelName modelName = new ModelName(modelString);
                EmbeddingModel result = supplier.create(modelName);
                
                assertThat(result, is(sameInstance(mockLocalOnnxEmbeddingModel)));
                mockedLocalOnnx.verify(() -> LocalOnnxEmbeddingModel.create(modelName));
                }
            }
        }
    } 
