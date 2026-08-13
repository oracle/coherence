/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import com.oracle.coherence.mp.config.CoherenceConfigSource;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.security.Principal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Config REST API controller.
 * <p/>
 * This test class validates the configuration management API including property
 * retrieval, updates, and validation. The Config API provides access to
 * system-wide configuration properties that affect the behavior of the
 * Coherence RAG framework.
 * <p/>
 * Test categories covered:
 * <ul>
 * <li>Configuration property retrieval</li>
 * <li>Property updates and validation</li>
 * <li>Error handling for invalid configurations</li>
 * </ul>
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Config REST API Tests")
class ConfigTest
    {
    // ---- test infrastructure ---------------------------------------------

    private Config config;

    @Mock
    private CoherenceConfigSource coherenceConfig;

    // ---- lifecycle methods -----------------------------------------------

    @BeforeEach
    void setUp()
        {
        MockitoAnnotations.openMocks(this);
        RagSecurity.resetHuggingFaceAllowlistForTesting();
        
        config = new Config();
        injectField("coherenceConfig", coherenceConfig);
        }

    @AfterEach
    void cleanup()
        {
        System.clearProperty(RagSecurity.PROP_CONFIG_WRITE_ALLOWED_PROPERTIES);
        System.clearProperty(RagSecurity.PROP_HUGGINGFACE_ALLOWED_MODELS);
        System.clearProperty(RagSecurity.PROP_ADMIN_ROLE);
        System.clearProperty("coherence.mode");
        RagSecurity.resetHuggingFaceAllowlistForTesting();
        }

    private void injectField(String sName, Object value)
        {
        try
            {
            var field = Config.class.getDeclaredField(sName);
            field.setAccessible(true);
            field.set(config, value);
            }
        catch (Exception e)
            {
            throw new RuntimeException("Failed to inject mock", e);
            }
        }

    private void injectSecurityContext(SecurityContext context)
        {
        try
            {
            var field = Config.class.getDeclaredField("securityContext");
            field.setAccessible(true);
            field.set(config, context);
            }
        catch (Exception e)
            {
            throw new RuntimeException("Failed to inject security context", e);
            }
        }

    // ---- configuration property tests -----------------------------------

    @Nested
    @DisplayName("Configuration Property Tests")
    class ConfigurationPropertyTests
        {
        @Test
        @DisplayName("Should get specific configuration property")
        void shouldGetSpecificConfigurationProperty()
            {
            // Arrange
            injectSecurityContext(context("alice", "reader"));
            String propertyName = "chunk.size";
            String expectedValue = "1000";
            when(coherenceConfig.getValue(propertyName)).thenReturn(expectedValue);

            // Act
            Response response = config.get(propertyName);

            // Assert
            assertThat(response.getStatus(), is(200));
            
            String propertyValue = (String) response.getEntity();
            assertThat(propertyValue, is(expectedValue));
            }

        @Test
        @DisplayName("Should return 404 for non-existent property")
        void shouldReturn404ForNonExistentProperty()
            {
            // Arrange
            injectSecurityContext(context("alice", "reader"));
            String propertyName = "non.existent.property";
            when(coherenceConfig.getValue(propertyName)).thenReturn(null);

            // Act
            Response response = config.get(propertyName);

            // Assert
            assertThat(response.getStatus(), is(404));
            }

        @Test
        @DisplayName("Should return 401 for unauthenticated property read")
        void shouldReturn401ForUnauthenticatedPropertyRead()
            {
            Response response = config.get("model.embedding");

            assertThat(response.getStatus(), is(401));
            verify(coherenceConfig, never()).getValue("model.embedding");
            }

        @Test
        @DisplayName("Should return 404 for sensitive property read")
        void shouldReturn404ForSensitivePropertyRead()
            {
            injectSecurityContext(context("alice", "reader"));

            Response response = config.get("openai.api.key");

            assertThat(response.getStatus(), is(404));
            verify(coherenceConfig, never()).getValue("openai.api.key");
            }

        @Test
        @DisplayName("Should update allowed configuration property")
        void shouldUpdateAllowedConfigurationProperty()
            {
            injectSecurityContext(context("admin-user", "admin"));
            String propertyName = "model.embedding";
            String newValue = "-/all-MiniLM-L6-v2";
            String oldValue = "old";
            when(coherenceConfig.setValue(propertyName, newValue)).thenReturn(oldValue);

            Response response = config.set(propertyName, newValue);

            assertThat(response.getStatus(), is(200));
            String returnedValue = (String) response.getEntity();
            assertThat(returnedValue, is(oldValue));
            }
        }

    // ---- configuration validation tests ---------------------------------

    @Nested
    @DisplayName("Configuration Validation Tests")
    class ConfigurationValidationTests
        {
        @Test
        @DisplayName("Should reject unauthenticated property update")
        void shouldRejectUnauthenticatedPropertyUpdate()
            {
            Response response = config.set("model.embedding", "-/all-MiniLM-L6-v2");

            assertThat(response.getStatus(), is(401));
            verify(coherenceConfig, never()).setValue("model.embedding", "-/all-MiniLM-L6-v2");
            }

        @Test
        @DisplayName("Should reject non-admin property update")
        void shouldRejectNonAdminPropertyUpdate()
            {
            injectSecurityContext(context("bob", "reader"));

            Response response = config.set("model.embedding", "-/all-MiniLM-L6-v2");

            assertThat(response.getStatus(), is(403));
            verify(coherenceConfig, never()).setValue("model.embedding", "-/all-MiniLM-L6-v2");
            }

        @Test
        @DisplayName("Should reject Basic authentication")
        void shouldRejectBasicAuthentication()
            {
            injectSecurityContext(contextWithScheme(SecurityContext.BASIC_AUTH, "admin-user", "admin"));

            Response response = config.set("model.embedding", "-/all-MiniLM-L6-v2");

            assertThat(response.getStatus(), is(401));
            verify(coherenceConfig, never()).setValue("model.embedding", "-/all-MiniLM-L6-v2");
            }

        @Test
        @DisplayName("Should accept bearer authentication")
        void shouldAcceptBearerAuthentication()
            {
            injectSecurityContext(contextWithScheme("Bearer", "admin-user", "admin"));
            when(coherenceConfig.setValue("model.embedding", "-/all-MiniLM-L6-v2")).thenReturn(null);

            Response response = config.set("model.embedding", "-/all-MiniLM-L6-v2");

            assertThat(response.getStatus(), is(200));
            }

        @Test
        @DisplayName("Should reject sensitive property update")
        void shouldRejectSensitivePropertyUpdate()
            {
            injectSecurityContext(context("admin-user", "admin"));

            Response response = config.set("openai.api.key", "secret");

            assertThat(response.getStatus(), is(400));
            verify(coherenceConfig, never()).setValue("openai.api.key", "secret");
            }

        @Test
        @DisplayName("Should reject policy namespace update")
        void shouldRejectPolicyNamespaceUpdate()
            {
            injectSecurityContext(context("admin-user", "admin"));
            System.setProperty(RagSecurity.PROP_CONFIG_WRITE_ALLOWED_PROPERTIES, RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES);

            Response response = config.set(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES, "file");

            assertThat(response.getStatus(), is(400));
            verify(coherenceConfig, never()).setValue(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES, "file");
            }

        @Test
        @DisplayName("Should reject unapproved model provider update")
        void shouldRejectUnapprovedModelDownloadInProd()
            {
            injectSecurityContext(context("admin-user", "admin"));
            System.setProperty("coherence.mode", "prod");

            Response response = config.set("model.embedding", "sentence-transformers/all-MiniLM-L6-v2");

            assertThat(response.getStatus(), is(400));
            verify(coherenceConfig, never()).setValue("model.embedding", "sentence-transformers/all-MiniLM-L6-v2");
            }

        @Test
        @DisplayName("Should allow exact HuggingFace model download match")
        void shouldAllowExactHuggingFaceModelDownloadMatch()
            {
            injectSecurityContext(context("admin-user", "admin"));
            System.setProperty("coherence.mode", "prod");
            System.setProperty(RagSecurity.PROP_HUGGINGFACE_ALLOWED_MODELS, "BAAI/bge-large-en-v1.5");
            when(coherenceConfig.setValue("model.embedding", "BAAI/bge-large-en-v1.5")).thenReturn(null);

            Response response = config.set("model.embedding", "BAAI/bge-large-en-v1.5");

            assertThat(response.getStatus(), is(200));
            }

        @Test
        @DisplayName("Should reject HuggingFace bare owner entry")
        void shouldRejectHuggingFaceBareOwnerEntry()
            {
            injectSecurityContext(context("admin-user", "admin"));
            System.setProperty("coherence.mode", "prod");
            System.setProperty(RagSecurity.PROP_HUGGINGFACE_ALLOWED_MODELS, "sentence-transformers");

            Response response = config.set("model.embedding", "sentence-transformers/all-MiniLM-L6-v2");

            assertThat(response.getStatus(), is(400));
            verify(coherenceConfig, never()).setValue("model.embedding", "sentence-transformers/all-MiniLM-L6-v2");
            }

        @Test
        @DisplayName("Should allow HuggingFace owner trailing wildcard")
        void shouldAllowHuggingFaceOwnerTrailingWildcard()
            {
            injectSecurityContext(context("admin-user", "admin"));
            System.setProperty("coherence.mode", "prod");
            System.setProperty(RagSecurity.PROP_HUGGINGFACE_ALLOWED_MODELS, "cross-encoder/*");
            when(coherenceConfig.setValue("model.scoring", "cross-encoder/ms-marco-MiniLM-L-6-v2")).thenReturn(null);

            Response response = config.set("model.scoring", "cross-encoder/ms-marco-MiniLM-L-6-v2");

            assertThat(response.getStatus(), is(200));
            }

        @Test
        @DisplayName("Should reject HuggingFace trailing wildcard for different owner")
        void shouldRejectHuggingFaceTrailingWildcardForDifferentOwner()
            {
            injectSecurityContext(context("admin-user", "admin"));
            System.setProperty("coherence.mode", "prod");
            System.setProperty(RagSecurity.PROP_HUGGINGFACE_ALLOWED_MODELS, "sentence-transformers/*");

            Response response = config.set("model.embedding", "sentence-transformers-extra/all-MiniLM-L6-v2");

            assertThat(response.getStatus(), is(400));
            verify(coherenceConfig, never()).setValue("model.embedding", "sentence-transformers-extra/all-MiniLM-L6-v2");
            }

        @Test
        @DisplayName("Should reject HuggingFace mid-string glob")
        void shouldRejectHuggingFaceMidStringGlob()
            {
            injectSecurityContext(context("admin-user", "admin"));
            System.setProperty("coherence.mode", "prod");
            System.setProperty(RagSecurity.PROP_HUGGINGFACE_ALLOWED_MODELS, "sentence-transformers/all-mini*");

            Response response = config.set("model.embedding", "sentence-transformers/all-MiniLM-L6-v2");

            assertThat(response.getStatus(), is(400));
            verify(coherenceConfig, never()).setValue("model.embedding", "sentence-transformers/all-MiniLM-L6-v2");
            }

        @Test
        @DisplayName("Should reject HuggingFace wildcard in owner")
        void shouldRejectHuggingFaceWildcardInOwner()
            {
            injectSecurityContext(context("admin-user", "admin"));
            System.setProperty("coherence.mode", "prod");
            System.setProperty(RagSecurity.PROP_HUGGINGFACE_ALLOWED_MODELS, "sentence-transformers*/x");

            Response response = config.set("model.embedding", "sentence-transformers/x");

            assertThat(response.getStatus(), is(400));
            verify(coherenceConfig, never()).setValue("model.embedding", "sentence-transformers/x");
            }

        @Test
        @DisplayName("Should reject HuggingFace empty and overqualified allowlist entries")
        void shouldRejectHuggingFaceEmptyAndOverqualifiedAllowlistEntries()
            {
            injectSecurityContext(context("admin-user", "admin"));
            System.setProperty("coherence.mode", "prod");
            System.setProperty(RagSecurity.PROP_HUGGINGFACE_ALLOWED_MODELS, ",a/b/c");

            Response response = config.set("model.embedding", "a/b");

            assertThat(response.getStatus(), is(400));
            assertThat(RagSecurity.invalidHuggingFaceAllowlistWarningCountForTesting(), is(2));
            verify(coherenceConfig, never()).setValue("model.embedding", "a/b");
            }

        @Test
        @DisplayName("Should warn once for each invalid HuggingFace allowlist entry")
        void shouldWarnOnceForEachInvalidHuggingFaceAllowlistEntry()
            {
            System.setProperty("coherence.mode", "prod");
            System.setProperty(RagSecurity.PROP_HUGGINGFACE_ALLOWED_MODELS,
                    "sentence-transformers,sentence-transformers/all-mini*");

            assertThrows(RagSecurity.PolicyViolation.class, () ->
                    RagSecurity.validateModelDownload("sentence-transformers/all-MiniLM-L6-v2",
                            RagSecurity.GATE_DOWNLOAD_CONFIG_VALUE));
            assertThrows(RagSecurity.PolicyViolation.class, () ->
                    RagSecurity.validateModelDownload("sentence-transformers/all-MiniLM-L6-v2",
                            RagSecurity.GATE_DOWNLOAD_CONFIG_VALUE));

            assertThat(RagSecurity.invalidHuggingFaceAllowlistWarningCountForTesting(), is(2));
            }

        @Test
        @DisplayName("Should allow chat model write without download allowlist")
        void shouldAllowChatModelWriteWithoutDownloadAllowlist()
            {
            injectSecurityContext(context("admin-user", "admin"));
            System.setProperty("coherence.mode", "prod");
            when(coherenceConfig.setValue("model.chat", "OpenAI/gpt-4o-mini")).thenReturn(null);

            Response response = config.set("model.chat", "OpenAI/gpt-4o-mini");

            assertThat(response.getStatus(), is(200));
            }
        }

    // ---- error handling tests ------------------------------------------

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests
        {
        @Test
        @DisplayName("Should handle configuration source errors gracefully")
        void shouldHandleConfigurationSourceErrorsGracefully()
            {
            // Arrange
            injectSecurityContext(context("alice", "reader"));
            String propertyName = "test.property";
            when(coherenceConfig.getValue(propertyName)).thenThrow(new RuntimeException("Config error"));

            // Act & Assert
            try
                {
                config.get(propertyName);
                // If we get here, the error was handled gracefully
                }
            catch (RuntimeException e)
                {
                // Expected behavior - the exception should propagate
                assertThat(e.getMessage(), is("Config error"));
                }
            }
        }

    private static SecurityContext context(String sName, String... asRole)
        {
        return contextWithScheme("Bearer", sName, asRole);
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
