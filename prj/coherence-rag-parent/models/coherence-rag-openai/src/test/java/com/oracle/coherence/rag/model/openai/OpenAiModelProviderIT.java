/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.openai;

import com.oracle.coherence.rag.config.ConfigRepository;
import com.oracle.coherence.rag.internal.json.JsonbProvider;

import com.tangosol.net.cache.WrapperNamedCache;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.ArrayList;
import java.util.HashMap;
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
 * Integration tests for OpenAiModelProvider that can run against both
 * real OpenAI API and WireMock recorded responses.
 * <p/>
 * The tests use WireMock in proxy/replay mode to capture real API responses
 * for later offline testing. Based on the API key configuration, tests will
 * either run against the real API or use recorded responses.
 * <p/>
 * Test modes:
 * <ul>
 * <li>Real API: When OPENAI_API_KEY does not start with "test"</li>
 * <li>WireMock: When OPENAI_API_KEY starts with "test" or when using recorded responses</li>
 * </ul>
 * <p/>
 * To capture new responses:
 * <ol>
 * <li>Run the Wiremock Proxy: {@code mvn exec:java@wiremock-proxy}</li>
 * <li>Set OPENAI_API_KEY to a real API key</li>
 * <li>Set OPENAI_BASE_URL to {@code http://localhost:8089/v1}</li>
 * <li>Run the tests to capture responses</li>
 * <li>Copy responses from {@code target/wiremock} to {@code test/resources/wiremock}</li>
 * <li>Stop WireMock proxy</li>
 * </ol>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
@DisplayName("OpenAiModelProvider Integration Tests")
class OpenAiModelProviderIT
    {
    // ---- test lifecycle --------------------------------------------------

    @BeforeAll
    static void setupClass()
        {
        // Determine if we should use WireMock or real API
        String apiKey = getProperty("openai.api.key", TEST_API_KEY);
        useWireMock = apiKey.startsWith("test");
        
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
            System.out.println("Running against real OpenAI API");
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
    void setUp()
        {
        ConfigRepository jsonConfig = new ConfigRepository(new WrapperNamedCache<>(new HashMap<>(), "jsonConfig"), new JsonbProvider());
        provider = new OpenAiModelProvider(new TestConfig(), jsonConfig);
        }

    // ---- tests -----------------------------------------------------------

    @Test
    @DisplayName("should call embedding model successfully")
    void shouldCreateEmbeddingModelSuccessfully()
        {
        // Use cost-effective embedding model
        EmbeddingModel model = provider.getEmbeddingModel(EMBEDDING_MODEL_NAME);
        assertThat(model, is(notNullValue()));
        assertThat(model, is(instanceOf(OpenAiEmbeddingModel.class)));

        // make model request and verify response
        var response = model.embed("Hello!");
        assertThat(response.content(), is(notNullValue()));
        assertThat(response.content().dimension(), is(1536));
        }

    @Test
    @DisplayName("should call chat model successfully")
    void shouldCreateChatModelSuccessfully()
        {
        ChatModel model = provider.getChatModel(CHAT_MODEL_NAME);
        assertThat(model, is(notNullValue()));
        assertThat(model, is(instanceOf(OpenAiChatModel.class)));

        // make model request and verify response
        var response = model.chat("Who are you?");
        System.out.println(response);
        assertThat(response, is(notNullValue()));
        assertThat(response, containsString("AI"));
        }

    @Test
    @DisplayName("should call streaming chat model successfully")
    void shouldCreateStreamingChatModelSuccessfully()
            throws InterruptedException
        {
        StreamingChatModel model = provider.getStreamingChatModel(CHAT_MODEL_NAME);
        assertThat(model, is(notNullValue()));
        assertThat(model, is(instanceOf(OpenAiStreamingChatModel.class)));

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
                throw new RuntimeException(error);
                }
            });

        completed.await();

        assertThat(tokens, is(not(empty())));
        assertThat(tokens, hasItem("AI"));
        }

    // ---- inner classes ---------------------------------------------------

    /**
     * Test configuration that provides appropriate OpenAI settings
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
            if ("openai.api.key".equals(propertyName))
                {
                String apiKey = getProperty("openai.api.key", TEST_API_KEY);
                return Optional.of((T) apiKey);
                }
            else if ("openai.base.url".equals(propertyName))
                {
                if (useWireMock)
                    {
                    // Point to WireMock server
                    return Optional.of((T) ("http://localhost:" + wireMockServer.port() + "/v1"));
                    }
                else
                    {
                    // Use real OpenAI API (default)
                    return Optional.ofNullable((T) getProperty("openai.base.url"));
                    }
                }
            
            return Optional.empty();
            }

        @Override
        public Iterable<String> getPropertyNames()
            {
            return java.util.Arrays.asList("openai.api.key", "openai.base.url");
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
        public <T> java.util.Optional<org.eclipse.microprofile.config.spi.Converter<T>> getConverter(Class<T> forType)
            {
            return java.util.Optional.empty();
            }

        @Override
        public org.eclipse.microprofile.config.ConfigValue getConfigValue(String propertyName)
            {
            throw new UnsupportedOperationException("getConfigValue not supported in test config");
            }
        }

    // ---- data members ----------------------------------------------------

    private OpenAiModelProvider provider;
    
    private static WireMockServer wireMockServer;
    private static boolean useWireMock;

    // ---- constants -------------------------------------------------------

    /**
     * Test API key.
     */
    public static final String TEST_API_KEY = "test-api-key";

    /**
     * Cost-effective embedding model for testing.
     * text-embedding-3-small is cheaper than text-embedding-3-large.
     */
    private static final String EMBEDDING_MODEL_NAME = "text-embedding-3-small";

    /**
     * Cost-effective chat model for testing.
     * gpt-3.5-turbo is significantly cheaper than gpt-4 models.
     */
    private static final String CHAT_MODEL_NAME = "gpt-3.5-turbo";
    } 
