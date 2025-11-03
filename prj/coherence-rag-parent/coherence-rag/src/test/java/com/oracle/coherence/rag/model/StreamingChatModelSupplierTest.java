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

import dev.langchain4j.model.chat.StreamingChatModel;

import io.helidon.config.ConfigException;

import org.eclipse.microprofile.config.Config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StreamingChatModelSupplier} class.
 * <p/>
 * This test class validates the chat model supplier functionality including
 * model creation, provider resolution, error handling, and caching behavior.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatModelSupplier")
class StreamingChatModelSupplierTest
    {
    @Mock
    private Config mockConfig;

    @Mock
    private CoherenceConfigSource mockCoherenceConfig;

    @Mock
    private ModelProvider mockProvider;

    @Mock
    private StreamingChatModel mockChatModel;

    private StreamingChatModelSupplier supplier;

    @BeforeEach
    void setUp() throws Exception
        {
        supplier = new StreamingChatModelSupplier();
        
        // Inject mocks using reflection
        Field configField = AbstractModelSupplier.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(supplier, mockConfig);

        Field coherenceConfigField = AbstractModelSupplier.class.getDeclaredField("coherenceConfig");
        coherenceConfigField.setAccessible(true);
        coherenceConfigField.set(supplier, mockCoherenceConfig);
        }

    @Nested
    @DisplayName("Configuration Properties")
    class ConfigurationPropertiesTests
        {
        @Test
        @DisplayName("should return correct description")
        void shouldReturnCorrectDescription()
            {
            String description = supplier.description();
            assertThat(description, is("chat"));
            }

        @Test
        @DisplayName("should return correct default model")
        void shouldReturnCorrectDefaultModel()
            {
            String defaultModel = supplier.defaultModel();
            assertThat(defaultModel, is("OpenAI/gpt-4o-mini"));
            }

        @Test
        @DisplayName("should return correct config property")
        void shouldReturnCorrectConfigProperty()
            {
            String configProperty = supplier.configProperty();
            assertThat(configProperty, is("model.chat"));
            }

        @Test
        @DisplayName("should use default chat model constant")
        void shouldUseDefaultChatModelConstant()
            {
            assertThat(supplier.defaultModel(), is(StreamingChatModelSupplier.DEFAULT_CHAT_MODEL));
            }
        }

    @Nested
    @DisplayName("Model Creation")
    class ModelCreationTests
        {
        @Test
        @DisplayName("should create chat model when provider is available")
        void shouldCreateChatModelWhenProviderIsAvailable()
            {
            try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
                {
                when(mockProvider.getStreamingChatModel("gpt-4o-mini")).thenReturn(mockChatModel);
                mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, "OpenAI"))
                        .thenReturn(mockProvider);

                ModelName modelName = new ModelName("OpenAI/gpt-4o-mini");
                StreamingChatModel result = supplier.create(modelName);

                assertThat(result, is(sameInstance(mockChatModel)));
                verify(mockProvider).getStreamingChatModel("gpt-4o-mini");
                }
            }

        @Test
        @DisplayName("should throw ConfigException when provider not found")
        void shouldThrowConfigExceptionWhenProviderNotFound()
            {
            try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
                {
                mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, "UnknownProvider"))
                        .thenReturn(null);

                ModelName modelName = new ModelName("UnknownProvider/test-model");
                
                ConfigException exception = assertThrows(ConfigException.class, 
                    () -> supplier.create(modelName));
                
                assertThat(exception.getMessage(), 
                    containsString("Chat model [UnknownProvider/test-model] is not supported"));
                }
            }

        @Test
        @DisplayName("should handle various provider formats")
        void shouldHandleVariousProviderFormats()
            {
            try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
                {
                when(mockProvider.getStreamingChatModel(any(String.class))).thenReturn(mockChatModel);
                mockedCdi.when(() -> CdiHelper.getNamedBean(eq(ModelProvider.class), any(String.class)))
                        .thenReturn(mockProvider);

                // Test various provider formats
                String[] testModels = {
                    "OpenAI/gpt-4o-mini",
                    "oci/meta.llama-3.1-70b-instruct",
                    "ollama/llama3.1:8b",
                    "deepseek/deepseek-chat"
                };

                for (String modelString : testModels)
                    {
                    ModelName modelName = new ModelName(modelString);
                    StreamingChatModel result = supplier.create(modelName);
                    
                    assertThat(result, is(sameInstance(mockChatModel)));
                    verify(mockProvider).getStreamingChatModel(modelName.name());
                    }
                }
            }

        @Test
        @DisplayName("should handle model names with special characters")
        void shouldHandleModelNamesWithSpecialCharacters()
            {
            try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
                {
                when(mockProvider.getStreamingChatModel("model:with-special_chars.v2")).thenReturn(mockChatModel);
                mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, "TestProvider"))
                        .thenReturn(mockProvider);

                ModelName modelName = new ModelName("TestProvider/model:with-special_chars.v2");
                StreamingChatModel result = supplier.create(modelName);

                assertThat(result, is(sameInstance(mockChatModel)));
                verify(mockProvider).getStreamingChatModel("model:with-special_chars.v2");
                }
            }
        }

    @Nested
    @DisplayName("Integration with AbstractModelSupplier")
    class IntegrationTests
        {
        @BeforeEach
        void setUp() throws Exception
            {
            when(mockConfig.getOptionalValue("model.chat", String.class))
                .thenReturn(Optional.of("OpenAI/gpt-4o-mini"));

            // Initialize supplier
            Method initMethod = AbstractModelSupplier.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(supplier);
            }

        @Test
        @DisplayName("should cache chat model instances")
        void shouldCacheChatModelInstances()
            {
            try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
                {
                when(mockProvider.getStreamingChatModel("cached-model")).thenReturn(mockChatModel);
                mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, "CachedProvider"))
                        .thenReturn(mockProvider);

                // Get same model multiple times
                StreamingChatModel model1 = supplier.get("CachedProvider/cached-model");
                StreamingChatModel model2 = supplier.get("CachedProvider/cached-model");
                StreamingChatModel model3 = supplier.get("CachedProvider/cached-model");

                assertThat(model1, is(sameInstance(model2)));
                assertThat(model2, is(sameInstance(model3)));

                // Verify provider was called only once
                verify(mockProvider, times(1)).getStreamingChatModel("cached-model");
                }
            }

        @Test
        @DisplayName("should support default model access")
        void shouldSupportDefaultModelAccess()
            {
            try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
                {
                when(mockProvider.getStreamingChatModel("gpt-4o-mini")).thenReturn(mockChatModel);
                mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, "OpenAI"))
                        .thenReturn(mockProvider);

                StreamingChatModel result = supplier.get();

                assertThat(result, is(sameInstance(mockChatModel)));
                verify(mockProvider).getStreamingChatModel("gpt-4o-mini");
                }
            }

        @Test
        @DisplayName("should support ModelName parameter")
        void shouldSupportModelNameParameter()
            {
            try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
                {
                when(mockProvider.getStreamingChatModel("specific-model")).thenReturn(mockChatModel);
                mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, "SpecificProvider"))
                        .thenReturn(mockProvider);

                ModelName modelName = new ModelName("SpecificProvider/specific-model");
                StreamingChatModel result = supplier.get(modelName);

                assertThat(result, is(sameInstance(mockChatModel)));
                verify(mockProvider).getStreamingChatModel("specific-model");
                }
            }

        @Test
        @DisplayName("should determine default model name from config")
        void shouldDetermineDefaultModelNameFromConfig()
            {
            when(mockConfig.getOptionalValue("model.chat", String.class))
                .thenReturn(Optional.of("custom/chat-model"));

            ModelName defaultModelName = supplier.defaultModelName();

            assertThat(defaultModelName.fullName(), is("custom/chat-model"));
            }

        @Test
        @DisplayName("should fall back to default when config not available")
        void shouldFallBackToDefaultWhenConfigNotAvailable()
            {
            when(mockConfig.getOptionalValue("model.chat", String.class))
                .thenReturn(Optional.empty());

            ModelName defaultModelName = supplier.defaultModelName();

            assertThat(defaultModelName.fullName(), is("OpenAI/gpt-4o-mini"));
            }
        }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests
        {
        @Test
        @DisplayName("should throw ConfigException with proper message format")
        void shouldThrowConfigExceptionWithProperMessageFormat()
            {
            try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
                {
                mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, "NonExistentProvider"))
                        .thenReturn(null);

                ModelName modelName = new ModelName("NonExistentProvider/some-model");
                
                ConfigException exception = assertThrows(ConfigException.class, 
                    () -> supplier.create(modelName));
                
                assertThat(exception.getMessage(), 
                    is("Chat model [NonExistentProvider/some-model] is not supported"));
                }
            }

        @Test
        @DisplayName("should handle null provider gracefully")
        void shouldHandleNullProviderGracefully()
            {
            try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
                {
                mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, ""))
                        .thenReturn(null);

                ModelName modelName = new ModelName("model-without-provider");
                
                ConfigException exception = assertThrows(ConfigException.class, 
                    () -> supplier.create(modelName));
                
                assertThat(exception.getMessage(), 
                    containsString("Chat model [model-without-provider] is not supported"));
                }
            }

        @Test
        @DisplayName("should handle empty provider in model name")
        void shouldHandleEmptyProviderInModelName()
            {
            try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
                {
                mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, ""))
                        .thenReturn(null);

                ModelName modelName = new ModelName("/model-name");
                
                ConfigException exception = assertThrows(ConfigException.class, 
                    () -> supplier.create(modelName));
                
                assertThat(exception.getMessage(), 
                    is("Chat model [/model-name] is not supported"));
                }
            }

        @Test
        @DisplayName("should handle provider exceptions gracefully")
        void shouldHandleProviderExceptionsGracefully()
            {
            try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
                {
                when(mockProvider.getStreamingChatModel("failing-model"))
                        .thenThrow(new RuntimeException("Provider error"));
                mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, "FailingProvider"))
                        .thenReturn(mockProvider);

                ModelName modelName = new ModelName("FailingProvider/failing-model");
                
                RuntimeException exception = assertThrows(RuntimeException.class, 
                    () -> supplier.create(modelName));
                
                assertThat(exception.getMessage(), is("Provider error"));
                }
            }
        }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests
        {
        @Test
        @DisplayName("should handle complex model names")
        void shouldHandleComplexModelNames()
            {
            try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
                {
                when(mockProvider.getStreamingChatModel("meta.llama-3.1-405b-instruct")).thenReturn(mockChatModel);
                mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, "oci"))
                        .thenReturn(mockProvider);

                ModelName modelName = new ModelName("oci/meta.llama-3.1-405b-instruct");
                StreamingChatModel result = supplier.create(modelName);

                assertThat(result, is(sameInstance(mockChatModel)));
                verify(mockProvider).getStreamingChatModel("meta.llama-3.1-405b-instruct");
                }
            }

        @Test
        @DisplayName("should handle unicode characters in model names")
        void shouldHandleUnicodeCharactersInModelNames()
            {
            try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
                {
                when(mockProvider.getStreamingChatModel("模型-名称")).thenReturn(mockChatModel);
                mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, "unicode-provider"))
                        .thenReturn(mockProvider);

                ModelName modelName = new ModelName("unicode-provider/模型-名称");
                StreamingChatModel result = supplier.create(modelName);

                assertThat(result, is(sameInstance(mockChatModel)));
                verify(mockProvider).getStreamingChatModel("模型-名称");
                }
            }

        @Test
        @DisplayName("should handle very long model names")
        void shouldHandleVeryLongModelNames()
            {
            try (MockedStatic<CdiHelper> mockedCdi = mockStatic(CdiHelper.class))
                {
                String longModelName = "a".repeat(500);
                when(mockProvider.getStreamingChatModel(longModelName)).thenReturn(mockChatModel);
                mockedCdi.when(() -> CdiHelper.getNamedBean(ModelProvider.class, "long-provider"))
                        .thenReturn(mockProvider);

                ModelName modelName = new ModelName("long-provider/" + longModelName);
                StreamingChatModel result = supplier.create(modelName);

                assertThat(result, is(sameInstance(mockChatModel)));
                verify(mockProvider).getStreamingChatModel(longModelName);
                }
            }
        }
    }
