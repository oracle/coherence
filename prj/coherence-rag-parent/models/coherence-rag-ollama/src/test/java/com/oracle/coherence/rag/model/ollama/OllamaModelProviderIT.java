/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.ollama;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.tangosol.coherence.config.Config.getProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for OllamaModelProvider that can run against both
 * real Ollama API and WireMock recorded responses.
 * <p/>
 * The tests use WireMock in proxy/replay mode to capture real API responses
 * for later offline testing. Based on the base URL configuration, tests will
 * either run against the real API or use recorded responses.
 * <p/>
 * Test modes:
 * <ul>
 * <li>Real API: When OLLAMA_BASE_URL points to actual Ollama server</li>
 * <li>WireMock: When OLLAMA_BASE_URL points to WireMock server or when using recorded responses</li>
 * </ul>
 * <p/>
 * To capture new responses:
 * <ol>
 * <li>Start Ollama server with models: {@code ollama serve}</li>
 * <li>Pull test model: {@code ollama pull llama3.2:1b}</li>
 * <li>Run the Wiremock Proxy: {@code mvn exec:java@wiremock-proxy}</li>
 * <li>Set OLLAMA_BASE_URL to {@code http://localhost:8089}</li>
 * <li>Run the tests to capture responses</li>
 * <li>Copy responses from {@code target/wiremock} to {@code test/resources/wiremock}</li>
 * <li>Stop WireMock proxy</li>
 * </ol>
 * <p/>
 * <strong>Note:</strong> This test uses {@code llama3.2:1b}, a lightweight 1GB model
 * that can run on reasonably sized laptops. Make sure this model is available
 * in your local Ollama installation when running against the real API.
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
@DisplayName("OllamaModelProvider Integration Tests")
class OllamaModelProviderIT
    {
    // ---- test lifecycle --------------------------------------------------

    @BeforeAll
    static void setupClass()
        {
        // Determine if we should use WireMock or real API
        String baseUrl = getProperty("ollama.base.url");
        useWireMock = baseUrl == null;
        
        if (useWireMock)
            {
            // Start WireMock server for replay mode
            wireMockServer = new WireMockServer(wireMockConfig()
                .port(0)
                .usingFilesUnderClasspath("wiremock"));
            
            wireMockServer.start();
            WireMock.configureFor("localhost", wireMockServer.port());
            
            System.out.println("WireMock server started on port " + wireMockServer.port());
            }
        else
            {
            System.out.println("Running against real Ollama API at: " + baseUrl);
            System.out.println("Make sure 'ollama serve' is running and model '" + CHAT_MODEL_NAME + "' is available");
            }
        }

    @AfterAll
    static void tearDownClass()
        {
        if (wireMockServer != null && wireMockServer.isRunning())
            {
            wireMockServer.stop();
            System.out.println("WireMock server stopped");
            }
        }

    @BeforeEach
    void setUp() throws Exception
        {
        provider = new OllamaModelProvider();
        
        // Inject test config using reflection
        Field configField = OllamaModelProvider.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(provider, new TestConfig());
        }

    // ---- tests -----------------------------------------------------------

    @Test
    @DisplayName("should call embedding model successfully")
    void shouldCreateEmbeddingModelSuccessfully()
        {
        EmbeddingModel model = provider.getEmbeddingModel(EMBEDDING_MODEL_NAME);
        assertThat(model, is(notNullValue()));
        assertThat(model, is(instanceOf(OllamaEmbeddingModel.class)));

        // make model request and verify response
        var response = model.embed("Hello!");
        assertThat(response.content(), is(notNullValue()));
        assertThat(response.content().dimension(), is(768));
        }

    @Test
    @DisplayName("should call chat model successfully")
    void shouldCreateChatModelSuccessfully()
        {
        ChatModel model = provider.getChatModel(CHAT_MODEL_NAME);
        assertThat(model, is(notNullValue()));
        assertThat(model, is(instanceOf(OllamaChatModel.class)));

        // make model request and verify response
        var response = model.chat("Who are you?");
        System.out.println(response);
        assertThat(response, is(notNullValue()));
        assertThat(response.toLowerCase(), anyOf(containsString("llama"), containsString("assistant"), containsString("ai")));
        }

    @Test
    @DisplayName("should call streaming chat model successfully")
    void shouldCreateStreamingChatModelSuccessfully()
            throws InterruptedException
        {
        StreamingChatModel model = provider.getStreamingChatModel(CHAT_MODEL_NAME);
        assertThat(model, is(notNullValue()));
        assertThat(model, is(instanceOf(OllamaStreamingChatModel.class)));

        // make model request and verify response
        List<String>   tokens    = new ArrayList<>();
        CountDownLatch completed = new CountDownLatch(1);
        model.chat("Who are you?", new StreamingChatResponseHandler()
            {
            public void onPartialResponse(String partialResponse)
                {
                tokens.add(partialResponse.trim());
                }

            public void onCompleteResponse(ChatResponse completeResponse)
                {
                System.out.println(completeResponse);
                completed.countDown();
                }

            public void onError(Throwable error)
                {
                error.printStackTrace();
                completed.countDown();
                }
            });

        completed.await();

        assertThat(tokens, is(not(empty())));
        assertThat(String.join("", tokens).toLowerCase(), 
            anyOf(containsString("llama"), containsString("assistant"), containsString("ai")));
        }

    // ---- inner classes ---------------------------------------------------

    /**
     * Test configuration that provides appropriate Ollama settings
     * based on whether we're using WireMock or real API.
     */
    private static class TestConfig implements Config
        {
        @Override
        public <T> T getValue(String propertyName, Class<T> propertyType)
            {
            return getOptionalValue(propertyName, propertyType)
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + propertyName));
            }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType)
            {
            if ("ollama.base.url".equals(propertyName))
                {
                if (useWireMock)
                    {
                    // Point to WireMock server
                    return Optional.of((T) ("http://localhost:" + wireMockServer.port()));
                    }
                else
                    {
                    // Use real Ollama API or custom base URL
                    return Optional.ofNullable((T) getProperty("ollama.base.url", DEFAULT_BASE_URL));
                    }
                }
            
            return Optional.empty();
            }

        @Override
        public Iterable<String> getPropertyNames()
            {
            return java.util.Arrays.asList("ollama.base.url");
            }

        @Override
        public Iterable<ConfigSource> getConfigSources()
            {
            return java.util.Collections.emptyList();
            }

        @Override
        public <T> T unwrap(Class<T> type)
            {
            throw new UnsupportedOperationException("unwrap not supported in test config");
            }

        @Override
        public <T> Optional<org.eclipse.microprofile.config.spi.Converter<T>> getConverter(Class<T> forType)
            {
            return Optional.empty();
            }

        @Override
        public org.eclipse.microprofile.config.ConfigValue getConfigValue(String propertyName)
            {
            throw new UnsupportedOperationException("getConfigValue not supported in test config");
            }
        }

    // ---- data members ----------------------------------------------------

    private OllamaModelProvider provider;
    
    private static WireMockServer wireMockServer;
    private static boolean useWireMock;

    // ---- constants -------------------------------------------------------

    /**
     * Default Ollama base URL.
     */
    public static final String DEFAULT_BASE_URL = "http://localhost:11434";

    /**
     * Lightweight chat model for testing that can run on laptops.
     * llama3.2:1b requires only about 1GB of memory.
     */
    private static final String CHAT_MODEL_NAME = "llama3.2:1b";

    /**
     * Embedding model for testing.
     * nomic-embed-text is a lightweight embedding model.
     */
    private static final String EMBEDDING_MODEL_NAME = "nomic-embed-text";
    } 
