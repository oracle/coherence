/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.internal;

import com.oracle.coherence.rag.config.index.IndexConfig;
import com.oracle.coherence.rag.config.StoreConfig;
import com.oracle.coherence.rag.model.EmbeddingModelSupplier;
import com.oracle.coherence.rag.model.ModelName;

import org.eclipse.microprofile.config.Config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StoreConfigCacheLoader} class.
 * <p/>
 * This test class validates the store configuration cache loader functionality
 * including default configuration creation, dependency injection, and configuration properties.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StoreConfigCacheLoader")
class StoreConfigCacheLoaderTest
    {
    @Mock
    private EmbeddingModelSupplier mockEmbeddingModelSupplier;

    @Mock
    private Config mockConfig;

    @Mock
    private ModelName mockModelName;

    private StoreConfigCacheLoader loader;

    @BeforeEach
    void setUp() throws Exception
        {
        loader = new StoreConfigCacheLoader();
        
        // Inject mock dependencies using reflection
        Field embeddingSupplierField = StoreConfigCacheLoader.class.getDeclaredField("embeddingModelSupplier");
        embeddingSupplierField.setAccessible(true);
        embeddingSupplierField.set(loader, mockEmbeddingModelSupplier);
        
        Field configField = StoreConfigCacheLoader.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(loader, mockConfig);
        }

    @Test
    @DisplayName("should create default store configuration")
    void shouldCreateDefaultStoreConfiguration()
        {
        String storeName = "testStore";
        String modelName = "openai/text-embedding-ada-002";
        
        when(mockModelName.fullName()).thenReturn(modelName);
        when(mockEmbeddingModelSupplier.defaultModelName()).thenReturn(mockModelName);
        
        // Setup config defaults
        when(mockConfig.getOptionalValue("chunk.size", Integer.class))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue("chunk.overlap", Integer.class))
            .thenReturn(Optional.empty());

        StoreConfig result = loader.load(storeName);

        assertThat(result, is(notNullValue()));
        assertThat(result.getEmbeddingModel(), is(modelName));
        assertThat(result.isNormalizeEmbeddings(), is(false));
        assertThat(result.getIndex(), is(notNullValue()));
        assertThat(result.getIndex().type(), is("NONE"));
        assertThat(result.getChunkSize(), is(1250));
        assertThat(result.getChunkOverlap(), is(150));
        }

   @Test
    @DisplayName("should use configured chunk size")
    void shouldUseConfiguredChunkSize()
        {
        String storeName = "testStore";
        String modelName = "openai/text-embedding-ada-002";
        int chunkSize = 2000;
        
        when(mockModelName.fullName()).thenReturn(modelName);
        when(mockEmbeddingModelSupplier.defaultModelName()).thenReturn(mockModelName);
        
        when(mockConfig.getOptionalValue("chunk.size", Integer.class))
            .thenReturn(Optional.of(chunkSize));
        when(mockConfig.getOptionalValue("chunk.overlap", Integer.class))
            .thenReturn(Optional.empty());

        StoreConfig result = loader.load(storeName);

        assertThat(result.getChunkSize(), is(chunkSize));
        }

    @Test
    @DisplayName("should use configured chunk overlap")
    void shouldUseConfiguredChunkOverlap()
        {
        String storeName = "testStore";
        String modelName = "openai/text-embedding-ada-002";
        int chunkOverlap = 300;
        
        when(mockModelName.fullName()).thenReturn(modelName);
        when(mockEmbeddingModelSupplier.defaultModelName()).thenReturn(mockModelName);
        
        when(mockConfig.getOptionalValue("chunk.size", Integer.class))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue("chunk.overlap", Integer.class))
            .thenReturn(Optional.of(chunkOverlap));

        StoreConfig result = loader.load(storeName);

        assertThat(result.getChunkOverlap(), is(chunkOverlap));
        }

    @Test
    @DisplayName("should use all configured values")
    void shouldUseAllConfiguredValues()
        {
        String storeName = "productionStore";
        String modelName = "oci/cohere.embed-english-v3.0";
        int chunkSize = 1500;
        int chunkOverlap = 200;
        
        when(mockModelName.fullName()).thenReturn(modelName);
        when(mockEmbeddingModelSupplier.defaultModelName()).thenReturn(mockModelName);
        
        when(mockConfig.getOptionalValue("chunk.size", Integer.class))
            .thenReturn(Optional.of(chunkSize));
        when(mockConfig.getOptionalValue("chunk.overlap", Integer.class))
            .thenReturn(Optional.of(chunkOverlap));

        StoreConfig result = loader.load(storeName);

        assertThat(result.getEmbeddingModel(), is(modelName));
        assertThat(result.getChunkSize(), is(chunkSize));
        assertThat(result.getChunkOverlap(), is(chunkOverlap));
        assertThat(result.isNormalizeEmbeddings(), is(false));
        assertThat(result.getIndex().type(), is("NONE"));
        }

    @Test
    @DisplayName("should handle different store names")
    void shouldHandleDifferentStoreNames()
        {
        String[] storeNames = {"documents", "knowledge-base", "customer-docs", "product-catalog"};
        String modelName = "openai/text-embedding-3-small";
        
        when(mockModelName.fullName()).thenReturn(modelName);
        when(mockEmbeddingModelSupplier.defaultModelName()).thenReturn(mockModelName);
        
        when(mockConfig.getOptionalValue(eq("chunk.size"), eq(Integer.class)))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue(eq("chunk.overlap"), eq(Integer.class)))
            .thenReturn(Optional.empty());

        for (String storeName : storeNames)
            {
            StoreConfig result = loader.load(storeName);

            assertThat(result, is(notNullValue()));
            assertThat(result.getEmbeddingModel(), is(modelName));
            assertThat(result.getChunkSize(), is(1250));
            assertThat(result.getChunkOverlap(), is(150));
            }
        }

    @Test
    @DisplayName("should handle edge case values")
    void shouldHandleEdgeCaseValues()
        {
        String storeName = "edgeStore";
        String modelName = "local/sentence-transformers";
        int minChunkSize = 100;
        int zeroOverlap = 0;
        
        when(mockModelName.fullName()).thenReturn(modelName);
        when(mockEmbeddingModelSupplier.defaultModelName()).thenReturn(mockModelName);
        
        when(mockConfig.getOptionalValue("chunk.size", Integer.class))
            .thenReturn(Optional.of(minChunkSize));
        when(mockConfig.getOptionalValue("chunk.overlap", Integer.class))
            .thenReturn(Optional.of(zeroOverlap));

        StoreConfig result = loader.load(storeName);

        assertThat(result.getEmbeddingModel(), is(modelName));
        assertThat(result.getChunkSize(), is(minChunkSize));
        assertThat(result.getChunkOverlap(), is(zeroOverlap));
        }

    @Test
    @DisplayName("should create consistent configurations")
    void shouldCreateConsistentConfigurations()
        {
        String storeName = "consistencyTest";
        String modelName = "openai/text-embedding-ada-002";
        
        when(mockModelName.fullName()).thenReturn(modelName);
        when(mockEmbeddingModelSupplier.defaultModelName()).thenReturn(mockModelName);
        
        when(mockConfig.getOptionalValue(any(String.class), eq(Integer.class)))
            .thenReturn(Optional.empty());

        StoreConfig result1 = loader.load(storeName);
        StoreConfig result2 = loader.load(storeName);

        // Should create equivalent configurations
        assertThat(result1.getEmbeddingModel(), is(result2.getEmbeddingModel()));
        assertThat(result1.getChunkSize(), is(result2.getChunkSize()));
        assertThat(result1.getChunkOverlap(), is(result2.getChunkOverlap()));
        assertThat(result1.isNormalizeEmbeddings(), is(result2.isNormalizeEmbeddings()));
        }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("should handle complex model names")
    void shouldHandleComplexModelNames()
        {
        String storeName = "complexStore";
        String complexModelName = "oci/meta.llama-2-70b-chat/embedding-model-v1.0";
        
        when(mockModelName.fullName()).thenReturn(complexModelName);
        when(mockEmbeddingModelSupplier.defaultModelName()).thenReturn(mockModelName);
        
        when(mockConfig.getOptionalValue(any(String.class), any(Class.class)))
            .thenReturn(Optional.empty());

        StoreConfig result = loader.load(storeName);

        assertThat(result.getEmbeddingModel(), is(complexModelName));
        }

    @Test
    @DisplayName("should handle special characters in configuration values")
    void shouldHandleSpecialCharactersInConfigurationValues()
        {
        String storeName = "specialStore";
        String modelName = "custom/model-with-dashes_and_underscores.v2";

        when(mockModelName.fullName()).thenReturn(modelName);
        when(mockEmbeddingModelSupplier.defaultModelName()).thenReturn(mockModelName);
        
        when(mockConfig.getOptionalValue("chunk.size", Integer.class))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue("chunk.overlap", Integer.class))
            .thenReturn(Optional.empty());

        StoreConfig result = loader.load(storeName);

        assertThat(result.getEmbeddingModel(), is(modelName));
        }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("should verify IndexConfig defaults")
    void shouldVerifyIndexConfigDefaults()
        {
        String storeName = "indexTest";
        String modelName = "openai/text-embedding-ada-002";
        
        when(mockModelName.fullName()).thenReturn(modelName);
        when(mockEmbeddingModelSupplier.defaultModelName()).thenReturn(mockModelName);
        
        when(mockConfig.getOptionalValue(any(String.class), any(Class.class)))
            .thenReturn(Optional.empty());

        StoreConfig result = loader.load(storeName);

        IndexConfig<?> indexConfig = result.getIndex();
        assertThat(indexConfig, is(notNullValue()));
        assertThat(indexConfig.type(), is("NONE"));
        }
    } 
