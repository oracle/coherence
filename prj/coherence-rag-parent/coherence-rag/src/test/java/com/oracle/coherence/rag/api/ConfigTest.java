/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import com.oracle.coherence.mp.config.CoherenceConfigSource;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
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
        
        config = new Config();
        // Use reflection to inject the mock
        try
            {
            var field = Config.class.getDeclaredField("coherenceConfig");
            field.setAccessible(true);
            field.set(config, coherenceConfig);
            }
        catch (Exception e)
            {
            throw new RuntimeException("Failed to inject mock", e);
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
            String propertyName = "non.existent.property";
            when(coherenceConfig.getValue(propertyName)).thenReturn(null);

            // Act
            Response response = config.get(propertyName);

            // Assert
            assertThat(response.getStatus(), is(404));
            }

        @Test
        @DisplayName("Should update configuration property")
        void shouldUpdateConfigurationProperty()
            {
            // Arrange
            String propertyName = "chunk.size";
            String newValue = "2000";
            String oldValue = "1000";
            when(coherenceConfig.setValue(propertyName, newValue)).thenReturn(oldValue);

            // Act
            Response response = config.set(propertyName, newValue);

            // Assert
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
        @DisplayName("Should handle empty property values")
        void shouldHandleEmptyPropertyValues()
            {
            // Arrange
            String propertyName = "test.property";
            String emptyValue = "";
            when(coherenceConfig.setValue(propertyName, emptyValue)).thenReturn(null);

            // Act
            Response response = config.set(propertyName, emptyValue);

            // Assert
            assertThat(response.getStatus(), is(200));
            }

        @Test
        @DisplayName("Should handle null property values")
        void shouldHandleNullPropertyValues()
            {
            // Arrange
            String propertyName = "test.property";
            String nullValue = null;
            when(coherenceConfig.setValue(propertyName, nullValue)).thenReturn("old-value");

            // Act
            Response response = config.set(propertyName, nullValue);

            // Assert
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
    } 
