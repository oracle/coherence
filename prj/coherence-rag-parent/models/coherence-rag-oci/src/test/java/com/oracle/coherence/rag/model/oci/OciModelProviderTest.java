/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.oci;

import com.oracle.bmc.ConfigFileReader;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import io.helidon.config.ConfigException;

import org.eclipse.microprofile.config.Config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OciModelProvider} class.
 * <p/>
 * This test class validates the OCI model provider functionality including
 * configuration management, model creation for all supported types (embedding,
 * chat, streaming), and error handling scenarios.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OciModelProvider")
class OciModelProviderTest
    {
    @Mock
    private Config mockConfig;

    private OciModelProvider provider;

    @BeforeEach
    void setUp()
        {
        provider = new OciModelProvider(mockConfig);
        }

    @Test
    @DisplayName("should create embedding model with valid compartment ID")
    void shouldCreateEmbeddingModelWithValidCompartmentId()
        {
        String testCompartmentId = "ocid1.compartment.oc1..test";
        when(mockConfig.getOptionalValue("oci.base.url", String.class))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue("oci.compartment.id", String.class))
            .thenReturn(Optional.of(testCompartmentId));
        when(mockConfig.getOptionalValue("oci.config.file", String.class))
            .thenReturn(Optional.of("src/test/resources/test_config"));

        EmbeddingModel embeddingModel = provider.getEmbeddingModel("cohere.embed-english-v3.0");

        assertThat(embeddingModel, is(notNullValue()));
        assertThat(embeddingModel, is(instanceOf(OciGenAiEmbeddingModel.class)));
        }

    @Test
    @DisplayName("should create chat model with valid compartment ID")
    void shouldCreateChatModelWithValidCompartmentId()
        {
        String testCompartmentId = "ocid1.compartment.oc1..test";
        when(mockConfig.getOptionalValue("oci.base.url", String.class))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue("oci.compartment.id", String.class))
            .thenReturn(Optional.of(testCompartmentId));
        when(mockConfig.getOptionalValue("oci.config.file", String.class))
            .thenReturn(Optional.of("src/test/resources/test_config"));

        ChatModel chatModel = provider.getChatModel("cohere.command-r-08-2024");

        assertThat(chatModel, is(notNullValue()));
        assertThat(chatModel, is(instanceOf(OciGenAiChatModel.class)));
        }

    @Test
    @DisplayName("should create streaming chat model with valid compartment ID")
    void shouldCreateStreamingChatModelWithValidCompartmentId()
        {
        String testCompartmentId = "ocid1.compartment.oc1..test";
        when(mockConfig.getOptionalValue("oci.base.url", String.class))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue("oci.compartment.id", String.class))
            .thenReturn(Optional.of(testCompartmentId));
        when(mockConfig.getOptionalValue("oci.config.file", String.class))
            .thenReturn(Optional.of("src/test/resources/test_config"));

        StreamingChatModel streamingChatModel = provider.getStreamingChatModel("cohere.command-r-08-2024");

        assertThat(streamingChatModel, is(notNullValue()));
        assertThat(streamingChatModel, is(instanceOf(OciGenAiStreamingChatModel.class)));
        }

    @Test
    @DisplayName("should throw ConfigException when compartment ID is missing for embedding model")
    void shouldThrowConfigExceptionWhenCompartmentIdMissingForEmbeddingModel()
        {
        when(mockConfig.getOptionalValue("oci.compartment.id", String.class))
            .thenReturn(Optional.empty());

        ConfigException exception = assertThrows(ConfigException.class,
            () -> provider.getEmbeddingModel("cohere.embed-english-v3.0"));

        assertThat(exception.getMessage(), containsString("OCI compartment ID is not set"));
        assertThat(exception.getMessage(), containsString("oci.compartment.id"));
        }

    @Test
    @DisplayName("should throw ConfigException when compartment ID is missing for chat model")
    void shouldThrowConfigExceptionWhenCompartmentIdMissingForChatModel()
        {
        when(mockConfig.getOptionalValue("oci.compartment.id", String.class))
            .thenReturn(Optional.empty());

        ConfigException exception = assertThrows(ConfigException.class,
            () -> provider.getChatModel("cohere.command-r-08-2024"));

        assertThat(exception.getMessage(), containsString("OCI compartment ID is not set"));
        assertThat(exception.getMessage(), containsString("oci.compartment.id"));
        }

    @Test
    @DisplayName("should throw ConfigException when compartment ID is missing for streaming chat model")
    void shouldThrowConfigExceptionWhenCompartmentIdMissingForStreamingChatModel()
        {
        when(mockConfig.getOptionalValue("oci.compartment.id", String.class))
            .thenReturn(Optional.empty());

        ConfigException exception = assertThrows(ConfigException.class,
            () -> provider.getStreamingChatModel("cohere.command-r-08-2024"));

        assertThat(exception.getMessage(), containsString("OCI compartment ID is not set"));
        assertThat(exception.getMessage(), containsString("oci.compartment.id"));
        }

    @Test
    @DisplayName("should throw IllegalArgumentException for null and empty model names")
    void shouldThrowIllegalArgumentExceptionForNullAndEmptyModelNames()
        {
        // Test null model names
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class,
            () -> provider.getEmbeddingModel(null));
        assertThat(exception1.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
            () -> provider.getChatModel(null));
        assertThat(exception2.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class,
            () -> provider.getStreamingChatModel(null));
        assertThat(exception3.getMessage(), containsString("Model name cannot be null or empty"));

        // Test empty model names
        IllegalArgumentException exception4 = assertThrows(IllegalArgumentException.class,
            () -> provider.getEmbeddingModel(""));
        assertThat(exception4.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception5 = assertThrows(IllegalArgumentException.class,
            () -> provider.getChatModel(""));
        assertThat(exception5.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception6 = assertThrows(IllegalArgumentException.class,
            () -> provider.getStreamingChatModel(""));
        assertThat(exception6.getMessage(), containsString("Model name cannot be null or empty"));

        // Test whitespace-only model names
        IllegalArgumentException exception7 = assertThrows(IllegalArgumentException.class,
            () -> provider.getEmbeddingModel("   "));
        assertThat(exception7.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception8 = assertThrows(IllegalArgumentException.class,
            () -> provider.getChatModel("   "));
        assertThat(exception8.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception9 = assertThrows(IllegalArgumentException.class,
            () -> provider.getStreamingChatModel("   "));
        assertThat(exception9.getMessage(), containsString("Model name cannot be null or empty"));
        }

    @Test
    @DisplayName("should throw ConfigException for whitespace-only compartment ID")
    void shouldThrowConfigExceptionForWhitespaceOnlyApiKey()
        {
        when(mockConfig.getOptionalValue("oci.compartment.id", String.class))
            .thenReturn(Optional.of("   "));

        // Should throw ConfigException for whitespace-only compartment ID
        ConfigException exception1 = assertThrows(ConfigException.class,
            () -> provider.getChatModel("cohere.command-r-08-2024"));
        assertThat(exception1.getMessage(), containsString("OCI compartment ID is not set. Please set the config property 'oci.compartment.id'"));

        ConfigException exception2 = assertThrows(ConfigException.class,
            () -> provider.getStreamingChatModel("cohere.command-r-08-2024"));
        assertThat(exception2.getMessage(), containsString("OCI compartment ID is not set. Please set the config property 'oci.compartment.id'"));
        }

    @Test
    @DisplayName("should handle configuration access for all model types")
    void shouldHandleConfigurationAccessForAllModelTypes()
        {
        String testCompartmentId = "ocid1.compartment.oc1..test";
        when(mockConfig.getOptionalValue("oci.base.url", String.class))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue("oci.compartment.id", String.class))
            .thenReturn(Optional.of(testCompartmentId));
        when(mockConfig.getOptionalValue("oci.config.file", String.class))
            .thenReturn(Optional.of("src/test/resources/test_config"));

        // Create one of each model type
        provider.getEmbeddingModel("cohere.embed-english-v3.0");
        provider.getChatModel("cohere.command-r-08-2024");
        provider.getStreamingChatModel("cohere.command-r-08-2024");

        // Verify configuration was accessed for each model creation
        verify(mockConfig, times(3)).getOptionalValue("oci.compartment.id", String.class);
        verify(mockConfig, times(3)).getOptionalValue("oci.config.file", String.class);
        verify(mockConfig, times(3)).getOptionalValue("oci.config.profile", String.class);
        }
    }
