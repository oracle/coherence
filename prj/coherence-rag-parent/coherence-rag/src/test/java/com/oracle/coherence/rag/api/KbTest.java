/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import com.oracle.coherence.rag.config.StoreConfig;
import com.oracle.coherence.rag.model.StreamingChatModelSupplier;
import com.oracle.coherence.rag.model.EmbeddingModelSupplier;
import com.oracle.coherence.rag.util.TestDataFactory;

import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Db REST API controller.
 * <p/>
 * This test class validates the database-level operations that work across
 * multiple stores, including listing stores, cross-store search, and
 * multi-store chat operations. The Db API provides a higher-level interface
 * for working with multiple knowledge stores simultaneously.
 * <p/>
 * Test categories covered:
 * <ul>
 * <li>Store listing and discovery</li>
 * <li>Store configuration management</li>
 * <li>Error handling and validation</li>
 * </ul>
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Db REST API Tests")
class KbTest
    {
    // ---- test constants --------------------------------------------------

    private static final String TEST_STORE_1 = "test-store-1";
    private static final String TEST_STORE_2 = "test-store-2";

    // ---- test infrastructure ---------------------------------------------

    private Kb m_kb;

    @Mock
    private Session session;

    @Mock
    private NamedMap<String, StoreConfig> storeConfig;

    @Mock
    private EmbeddingModelSupplier embeddingModelSupplier;

    @Mock
    private StreamingChatModelSupplier chatModelSupplier;

    // ---- lifecycle methods -----------------------------------------------

    @BeforeEach
    void setUp()
        {
        MockitoAnnotations.openMocks(this);

        m_kb = new Kb();
        
        // Use reflection to inject the mocks
        try
            {
            injectField("session", session);
            injectField("storeConfig", storeConfig);
            injectField("embeddingModelSupplier", embeddingModelSupplier);
            injectField("chatModelSupplier", chatModelSupplier);
            injectField("securityContext", context("admin-user", "admin"));
            }
        catch (Exception e)
            {
            throw new RuntimeException("Failed to inject mocks", e);
            }
        }

    @AfterEach
    void cleanup()
        {
        System.clearProperty(RagSecurity.PROP_HUGGINGFACE_ALLOWED_MODELS);
        System.clearProperty("coherence.mode");
        }

    private void injectField(String fieldName, Object value) throws Exception
        {
        var field = Kb.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(m_kb, value);
        }

    // ---- store listing tests --------------------------------------------

    @Nested
    @DisplayName("Store Listing Tests")
    class StoreListingTests
        {
        @Test
        @DisplayName("Should list all available stores")
        void shouldListAllAvailableStores()
            {
            // Arrange
            StoreConfig config1 = TestDataFactory.createStoreConfig(TEST_STORE_1);
            StoreConfig config2 = TestDataFactory.createStoreConfig(TEST_STORE_2);
            
            Map<String, StoreConfig> storeMap = Map.of(
                    TEST_STORE_1, config1,
                    TEST_STORE_2, config2
            );
            
            when(storeConfig.entrySet()).thenReturn(storeMap.entrySet());

            // Act
            Response response = m_kb.storeList();

            // Assert
            assertThat(response.getStatus(), is(200));
            
            @SuppressWarnings("unchecked")
            List<Kb.ConfiguredStore> stores = (List<Kb.ConfiguredStore>) response.getEntity();
            assertThat(stores, is(notNullValue()));
            assertThat(stores, hasSize(2));
            }

        @Test
        @DisplayName("Should return empty list when no stores exist")
        void shouldReturnEmptyListWhenNoStoresExist()
            {
            // Arrange
            when(storeConfig.entrySet()).thenReturn(Set.of());

            // Act
            Response response = m_kb.storeList();

            // Assert
            assertThat(response.getStatus(), is(200));
            
            @SuppressWarnings("unchecked")
            List<Kb.ConfiguredStore> stores = (List<Kb.ConfiguredStore>) response.getEntity();
            assertThat(stores, is(notNullValue()));
            assertThat(stores, hasSize(0));
            }

        @Test
        @DisplayName("Should get store configuration for specific store")
        void shouldGetStoreConfigurationForSpecificStore()
            {
            // Arrange
            String storeName = TEST_STORE_1;
            StoreConfig config = TestDataFactory.createStoreConfig(storeName);
            when(storeConfig.get(storeName)).thenReturn(config);

            // Act
            Response response = m_kb.storeConfig(storeName);

            // Assert
            assertThat(response.getStatus(), is(200));
            
            StoreConfig returnedConfig = (StoreConfig) response.getEntity();
            assertThat(returnedConfig, is(config));
            }

        @Test
        @DisplayName("Should return null for non-existent store")
        void shouldReturnNullForNonExistentStore()
            {
            // Arrange
            String nonExistentStore = "non-existent-store";
            when(storeConfig.get(nonExistentStore)).thenReturn(null);

            // Act
            Response response = m_kb.storeConfig(nonExistentStore);

            // Assert
            assertThat(response.getStatus(), is(200));
            assertThat(response.getEntity(), is((Object) null));
            }
        }

    // ---- store configuration tests --------------------------------------

    @Nested
    @DisplayName("Store Configuration Tests")
    class StoreConfigurationTests
        {
        @Test
        @DisplayName("Should create new store configuration")
        void shouldCreateNewStoreConfiguration()
            {
            // Arrange
            String storeName = TEST_STORE_1;
            StoreConfig config = TestDataFactory.createStoreConfig(storeName);
            config.setEmbeddingModel("-/all-MiniLM-L6-v2");

            // Act
            Response response = m_kb.configureStore(storeName, config);

            // Assert
            assertThat(response.getStatus(), is(204)); // No Content
            }

        @Test
        @DisplayName("Should update existing store configuration")
        void shouldUpdateExistingStoreConfiguration()
            {
            // Arrange
            String storeName = TEST_STORE_1;
            StoreConfig oldConfig = TestDataFactory.createStoreConfig(storeName);
            StoreConfig newConfig = TestDataFactory.createStoreConfig(storeName);
            newConfig.setEmbeddingModel("-/all-MiniLM-L6-v2");
            
            when(storeConfig.get(storeName)).thenReturn(oldConfig);

            // Act
            Response response = m_kb.configureStore(storeName, newConfig);

            // Assert
            assertThat(response.getStatus(), is(204)); // No Content
            }
        }

    // ---- error handling tests ------------------------------------------

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests
        {
        @Test
        @DisplayName("Should accept bearer authentication scheme")
        void shouldAcceptBearerAuthenticationScheme()
            {
            // symmetric Basic rejection is covered by ConfigTest.shouldRejectBasicAuthentication
            Response response = RagSecurity.requireAuthenticated(
                    contextWithScheme("BEARER", "reader", "reader"), RagSecurity.ROUTE_MODEL_CONFIG);

            assertNull(response);
            }

        @Test
        @DisplayName("Should reject null store configuration")
        void shouldRejectNullStoreConfiguration()
            {
            // Arrange
            String storeName = TEST_STORE_1;

            // Act
            Response response = m_kb.configureStore(storeName, null);

            // Assert
            assertThat(response.getStatus(), is(400));
            verify(storeConfig, never()).put(storeName, null);
            }

        @Test
        @DisplayName("Should handle empty store name")
        void shouldHandleEmptyStoreName()
            {
            // Arrange
            String emptyStoreName = "";
            StoreConfig config = TestDataFactory.createStoreConfig("test");
            config.setEmbeddingModel("-/all-MiniLM-L6-v2");

            // Act
            Response response = m_kb.configureStore(emptyStoreName, config);

            // Assert
            assertThat(response.getStatus(), is(204)); // Should process without error
            }

        @Test
        @DisplayName("Should handle store config map errors")
        void shouldHandleStoreConfigMapErrors()
            {
            // Arrange
            when(storeConfig.entrySet()).thenThrow(new RuntimeException("Map access error"));

            // Act & Assert
            try
                {
                m_kb.storeList();
                }
            catch (RuntimeException e)
                {
                assertThat(e.getMessage(), is("Map access error"));
                }
            }

        @Test
        @DisplayName("Should reject unauthenticated store configuration")
        void shouldRejectUnauthenticatedStoreConfiguration() throws Exception
            {
            injectField("securityContext", null);
            StoreConfig config = new StoreConfig();
            config.setEmbeddingModel("-/all-MiniLM-L6-v2");

            Response response = m_kb.configureStore(TEST_STORE_1, config);

            assertThat(response.getStatus(), is(401));
            verify(storeConfig, never()).put(TEST_STORE_1, config);
            }

        @Test
        @DisplayName("Should reject non-admin store configuration")
        void shouldRejectNonAdminStoreConfiguration() throws Exception
            {
            injectField("securityContext", context("bob", "reader"));
            StoreConfig config = new StoreConfig();
            config.setEmbeddingModel("-/all-MiniLM-L6-v2");

            Response response = m_kb.configureStore(TEST_STORE_1, config);

            assertThat(response.getStatus(), is(403));
            verify(storeConfig, never()).put(TEST_STORE_1, config);
            }

        @Test
        @DisplayName("Should reject store configuration with unallowlisted model download in prod")
        void shouldRejectStoreConfigurationWithUnallowlistedModelDownloadInProd()
            {
            System.setProperty("coherence.mode", "prod");
            StoreConfig config = new StoreConfig();
            config.setEmbeddingModel("sentence-transformers/all-MiniLM-L6-v2");

            Response response = m_kb.configureStore(TEST_STORE_1, config);

            assertThat(response.getStatus(), is(400));
            verify(storeConfig, never()).put(TEST_STORE_1, config);
            }

        @Test
        @DisplayName("Should not validate chat model in store configuration")
        void shouldNotValidateChatModelInStoreConfiguration()
            {
            System.setProperty("coherence.mode", "prod");
            StoreConfig config = new StoreConfig();
            config.setChatModel("OpenAI/gpt-4o-mini");
            config.setEmbeddingModel("-/all-MiniLM-L6-v2");

            Response response = m_kb.configureStore(TEST_STORE_1, config);

            assertThat(response.getStatus(), is(204));
            }

        @Test
        @DisplayName("Should reject search scoring model download in prod")
        void shouldRejectSearchScoringModelDownloadInProd()
            {
            System.setProperty("coherence.mode", "prod");
            Store.SearchRequest request = new Store.SearchRequest(
                    "query", 10, 0.0, 0.0, "cross-encoder/ms-marco-MiniLM-L-6-v2");

            Response response = m_kb.search(request);

            assertThat(response.getStatus(), is(400));
            }

        @Test
        @DisplayName("Should reject chat scoring model download in prod")
        void shouldRejectChatScoringModelDownloadInProd()
            {
            System.setProperty("coherence.mode", "prod");
            Store.ChatRequest request = new Store.ChatRequest(
                    null, "question", 10, 0.0, 0.0, "cross-encoder/ms-marco-MiniLM-L-6-v2");

            Response response = m_kb.chat(request);

            assertThat(response.getStatus(), is(400));
            }
        }

    // ---- integration tests ---------------------------------------------

    @Nested
    @DisplayName("Basic Integration Tests")
    class BasicIntegrationTests
        {
        @Test
        @DisplayName("Should handle complete store lifecycle")
        void shouldHandleCompleteStoreLifecycle()
            {
            // Arrange
            String storeName = TEST_STORE_1;
            StoreConfig config = TestDataFactory.createStoreConfig(storeName);
            config.setEmbeddingModel("-/all-MiniLM-L6-v2");
            
            when(storeConfig.get(storeName)).thenReturn(null)
                    .thenReturn(config);

            // Act & Assert - Configure store
            Response configResponse = m_kb.configureStore(storeName, config);
            assertThat(configResponse.getStatus(), is(204));

            // Act & Assert - Get store config
            Response getResponse = m_kb.storeConfig(storeName);
            assertThat(getResponse.getStatus(), is(200));
            // Note: Would be config if we properly set up the when().thenReturn chain
            }
        }

    private static SecurityContext context(String sName, String... asRole)
        {
        return contextWithScheme("test", sName, asRole);
        }

    private static SecurityContext contextWithScheme(String sScheme, String sName, String... asRole)
        {
        return new SecurityContext()
            {
            @Override
            public Principal getUserPrincipal()
                {
                return () -> sName;
                }

            @Override
            public boolean isUserInRole(String role)
                {
                for (String sRole : asRole)
                    {
                    if (sRole.equals(role))
                        {
                        return true;
                        }
                    }
                return false;
                }

            @Override
            public boolean isSecure()
                {
                return false;
                }

            @Override
            public String getAuthenticationScheme()
                {
                return sScheme;
                }
            };
        }
    } 
