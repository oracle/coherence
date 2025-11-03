/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.anthropic;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;


import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.CountDownLatch;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.tangosol.coherence.config.Config.getProperty;
import static dev.langchain4j.model.anthropic.AnthropicChatModelName.CLAUDE_3_7_SONNET_20250219;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for AnthropicModelProvider that can run against both
 * real Anthropic API and WireMock recorded responses.
 * <p/>
 * The tests use WireMock in proxy/replay mode to capture real API responses
 * for later offline testing. Based on the base URL configuration, tests will
 * either run against the real API or use recorded responses.
 * <p/>
 * Test modes:
 * <ul>
 * <li>Real API: When ANTHROPIC_API_KEY does not start with "test"</li>
 * <li>WireMock: When ANTHROPIC_API_KEY starts with "test" or when using recorded responses</li>
 * </ul>
 * <p/>
 * To capture new responses:
 * <ol>
 * <li>Run the Wiremock Proxy: {@code mvn exec:java@wiremock-proxy}</li>
 * <li>Set ANTHROPIC_API_KEY to a real API key</li>
 * <li>Set ANTHROPIC_BASE_URL to {@code http://localhost:8089/v1}</li>
 * <li>Run the tests to capture responses</li>
 * <li>Copy responses from {@code target/wiremock} to {@code test/resources/wiremock}</li>
 * <li>Stop WireMock proxy</li>
 * </ol>
 *
 *
 * @author Aleks Seovic/ Tim Middleton 2025.08.05
 * @since 25.09
 */
@DisplayName("AnthropicModelProvider Integration Tests")
class AnthropicModelProviderIT
    {
    // ---- test lifecycle --------------------------------------------------

    @BeforeAll
    static void setupClass()
        {
        // Determine if we should use WireMock or real API
        String apiKey = getProperty("anthropic.api.key", TEST_API_KEY);
        useWireMock   = apiKey.startsWith("test");
        
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
            System.out.println("Running against real Anthropic API");
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
        provider = new AnthropicModelProvider();
        
        // Inject test config using reflection
        Field configField = AnthropicModelProvider.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(provider, new TestConfig());
        }

    // ---- tests -----------------------------------------------------------

    @Test
    @DisplayName("should call chat model successfully")
    void shouldCreateChatModelSuccessfully()
        {
        ChatModel model = provider.getChatModel(CHAT_MODEL_NAME);
        assertThat(model, is(notNullValue()));
        assertThat(model, is(instanceOf(AnthropicChatModel.class)));

        // make model request and verify response
        var response = model.chat("Who are you?");
        System.out.println(response);
        assertThat(response, is(notNullValue()));
        assertThat(response.toLowerCase(), anyOf(containsString("anthropic"), containsString("assistant"), containsString("ai")));
        }

    @Test
    @DisplayName("should call streaming chat model successfully")
    void shouldCreateStreamingChatModelSuccessfully()
            throws InterruptedException
        {
        StreamingChatModel model = provider.getStreamingChatModel(CHAT_MODEL_NAME);
        assertThat(model, is(notNullValue()));
        assertThat(model, is(instanceOf(AnthropicStreamingChatModel.class)));

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
        }

    // ---- inner classes ---------------------------------------------------

    /**
     * Test configuration that provides appropriate Anthropic settings
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
            if ("anthropic.api.key".equals(propertyName))
                {
                String apiKey = getProperty("anthropic.api.key", TEST_API_KEY);
                return Optional.of((T) apiKey);
                }
            else if ("anthropic.base.url".equals(propertyName))
                {
                if (useWireMock)
                    {
                    // Point to WireMock server
                    return Optional.of((T) ("http://localhost:" + wireMockServer.port() + "/v1"));
                    }
                else
                    {
                    // Use real Anthropic API or custom base URL
                    return Optional.ofNullable((T) getProperty("anthropic.base.url", DEFAULT_BASE_URL));
                    }
                }
            
            return Optional.empty();
            }

        @Override
        public Iterable<String> getPropertyNames()
            {
            return java.util.Arrays.asList("anthropic.base.url");
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

    private AnthropicModelProvider provider;
    
    private static WireMockServer wireMockServer;

    private static boolean useWireMock;

    // ---- constants -------------------------------------------------------

    /**
     * Test API key.
     */
    public static final String TEST_API_KEY = "test-api-key";

    /**
     * Default Anthropic base URL.
     */
    public static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1/";

    /**
     * Model for testing.
     */
    private static final String CHAT_MODEL_NAME = CLAUDE_3_7_SONNET_20250219.toString();
    } 
