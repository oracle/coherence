/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link ModelName} record.
 * <p/>
 * This test class validates the model name parsing, validation, and component extraction
 * functionality for AI model identifiers.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@DisplayName("ModelName")
class ModelNameTest
    {
    @Nested
    @DisplayName("Construction")
    class ConstructionTests
        {
        @Test
        @DisplayName("should create instance with valid model name")
        void shouldCreateInstanceWithValidModelName()
            {
            ModelName modelName = new ModelName("OpenAI/text-embedding-ada-002");
            
            assertThat(modelName.fullName(), is("OpenAI/text-embedding-ada-002"));
            }

        @Test
        @DisplayName("should create instance with factory method")
        void shouldCreateInstanceWithFactoryMethod()
            {
            ModelName modelName = ModelName.of("OCI/cohere.embed-english-v3.0");
            
            assertThat(modelName.fullName(), is("OCI/cohere.embed-english-v3.0"));
            }

        @Test
        @DisplayName("should handle null model name")
        void shouldHandleNullModelName()
            {
            ModelName modelName = new ModelName(null);
            
            assertThat(modelName.fullName(), is(nullValue()));
            }

        @Test
        @DisplayName("should handle empty model name")
        void shouldHandleEmptyModelName()
            {
            ModelName modelName = new ModelName("");
            
            assertThat(modelName.fullName(), is(""));
            }
        }

    @Nested
    @DisplayName("Component Extraction")
    class ComponentExtractionTests
        {
        @Test
        @DisplayName("should extract provider from valid model name")
        void shouldExtractProviderFromValidModelName()
            {
            ModelName modelName = new ModelName("OpenAI/text-embedding-ada-002");
            
            assertThat(modelName.provider(), is("OpenAI"));
            }

        @Test
        @DisplayName("should extract model name from valid model name")
        void shouldExtractModelNameFromValidModelName()
            {
            ModelName modelName = new ModelName("OpenAI/text-embedding-ada-002");
            
            assertThat(modelName.name(), is("text-embedding-ada-002"));
            }

        @Test
        @DisplayName("should handle various provider formats")
        void shouldHandleVariousProviderFormats()
            {
            String[][] testCases = {
                {"OpenAI/gpt-4", "OpenAI", "gpt-4"},
                {"OCI/cohere.embed-english-v3.0", "OCI", "cohere.embed-english-v3.0"},
                {"Ollama/llama2", "Ollama", "llama2"},
                {"Azure/text-embedding-3-large", "Azure", "text-embedding-3-large"},
                {"custom-provider/custom-model", "custom-provider", "custom-model"}
            };
            
            for (String[] testCase : testCases)
                {
                ModelName modelName = new ModelName(testCase[0]);
                assertThat("Provider for " + testCase[0], modelName.provider(), is(testCase[1]));
                assertThat("Model name for " + testCase[0], modelName.name(), is(testCase[2]));
                }
            }

        @Test
        @DisplayName("should handle model names with multiple slashes")
        void shouldHandleModelNamesWithMultipleSlashes()
            {
            ModelName modelName = new ModelName("provider/model/with/slashes");
            
            assertThat(modelName.provider(), is("provider"));
            assertThat(modelName.name(), is("model"));
            }

        @Test
        @DisplayName("should handle model names with special characters")
        void shouldHandleModelNamesWithSpecialCharacters()
            {
            ModelName modelName = new ModelName("Provider-Name/model_name.v2.0-beta");
            
            assertThat(modelName.provider(), is("Provider-Name"));
            assertThat(modelName.name(), is("model_name.v2.0-beta"));
            }

        @Test
        @DisplayName("should handle model names with numbers")
        void shouldHandleModelNamesWithNumbers()
            {
            ModelName modelName = new ModelName("Provider2/model3-v4.5");
            
            assertThat(modelName.provider(), is("Provider2"));
            assertThat(modelName.name(), is("model3-v4.5"));
            }

        @Test
        @DisplayName("should throw exception for invalid model name format")
        void shouldThrowExceptionForInvalidModelNameFormat()
            {
            ModelName modelName = new ModelName("invalid-format");
            
            // provider() works fine (returns "invalid-format")
            assertThat(modelName.provider(), is("invalid-format"));
            // name() throws exception (no second element)
            assertThrows(ArrayIndexOutOfBoundsException.class, modelName::name);
            }

        @Test
        @DisplayName("should throw exception for null model name")
        void shouldThrowExceptionForNullModelName()
            {
            ModelName modelName = new ModelName(null);
            
            assertThrows(NullPointerException.class, modelName::provider);
            assertThrows(NullPointerException.class, modelName::name);
            }

        @Test
        @DisplayName("should throw exception for empty model name")
        void shouldThrowExceptionForEmptyModelName()
            {
            ModelName modelName = new ModelName("");
            
            // provider() works fine (returns "")
            assertThat(modelName.provider(), is(""));
            // name() throws exception (no second element)
            assertThrows(ArrayIndexOutOfBoundsException.class, modelName::name);
            }
        }

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityAndHashCodeTests
        {
        @Test
        @DisplayName("should be equal for same full name")
        void shouldBeEqualForSameFullName()
            {
            ModelName modelName1 = new ModelName("OpenAI/text-embedding-ada-002");
            ModelName modelName2 = new ModelName("OpenAI/text-embedding-ada-002");
            
            assertThat(modelName1, is(equalTo(modelName2)));
            assertThat(modelName1.hashCode(), is(equalTo(modelName2.hashCode())));
            }

        @Test
        @DisplayName("should not be equal for different full names")
        void shouldNotBeEqualForDifferentFullNames()
            {
            ModelName modelName1 = new ModelName("OpenAI/text-embedding-ada-002");
            ModelName modelName2 = new ModelName("OCI/cohere.embed-english-v3.0");
            
            assertThat(modelName1, is(not(equalTo(modelName2))));
            }

        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself()
            {
            ModelName modelName = new ModelName("Ollama/llama2");
            
            assertThat(modelName, is(equalTo(modelName)));
            }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull()
            {
            ModelName modelName = new ModelName("OpenAI/gpt-4");
            
            assertThat(modelName, is(not(equalTo(null))));
            }

        @Test
        @DisplayName("should not be equal to different class")
        void shouldNotBeEqualToDifferentClass()
            {
            ModelName modelName = new ModelName("OpenAI/gpt-4");
            
            assertThat(modelName, is(not(equalTo("OpenAI/gpt-4"))));
            }

        @Test
        @DisplayName("should handle null values in equality")
        void shouldHandleNullValuesInEquality()
            {
            ModelName modelName1 = new ModelName(null);
            ModelName modelName2 = new ModelName(null);
            
            assertThat(modelName1, is(equalTo(modelName2)));
            assertThat(modelName1.hashCode(), is(equalTo(modelName2.hashCode())));
            }

        @Test
        @DisplayName("should handle empty strings in equality")
        void shouldHandleEmptyStringsInEquality()
            {
            ModelName modelName1 = new ModelName("");
            ModelName modelName2 = new ModelName("");
            
            assertThat(modelName1, is(equalTo(modelName2)));
            assertThat(modelName1.hashCode(), is(equalTo(modelName2.hashCode())));
            }
        }

    @Nested
    @DisplayName("String Representation")
    class StringRepresentationTests
        {
        @Test
        @DisplayName("should include full name in toString")
        void shouldIncludeFullNameInToString()
            {
            ModelName modelName = new ModelName("OpenAI/text-embedding-ada-002");
            
            String toString = modelName.toString();
            
            assertThat(toString, containsString("ModelName"));
            assertThat(toString, containsString("OpenAI/text-embedding-ada-002"));
            }

        @Test
        @DisplayName("should handle null in toString")
        void shouldHandleNullInToString()
            {
            ModelName modelName = new ModelName(null);
            
            String toString = modelName.toString();
            
            assertThat(toString, containsString("ModelName"));
            assertThat(toString, containsString("null"));
            }

        @Test
        @DisplayName("should handle empty string in toString")
        void shouldHandleEmptyStringInToString()
            {
            ModelName modelName = new ModelName("");
            
            String toString = modelName.toString();
            
            assertThat(toString, containsString("ModelName"));
            }
        }

    @Nested
    @DisplayName("Factory Method")
    class FactoryMethodTests
        {
        @Test
        @DisplayName("should create instance equivalent to constructor")
        void shouldCreateInstanceEquivalentToConstructor()
            {
            String fullName = "OCI/cohere.embed-english-v3.0";
            
            ModelName fromConstructor = new ModelName(fullName);
            ModelName fromFactory = ModelName.of(fullName);
            
            assertThat(fromFactory, is(equalTo(fromConstructor)));
            assertThat(fromFactory.fullName(), is(fromConstructor.fullName()));
            }

        @Test
        @DisplayName("should handle null with factory method")
        void shouldHandleNullWithFactoryMethod()
            {
            ModelName modelName = ModelName.of(null);
            
            assertThat(modelName.fullName(), is(nullValue()));
            }

        @Test
        @DisplayName("should handle empty string with factory method")
        void shouldHandleEmptyStringWithFactoryMethod()
            {
            ModelName modelName = ModelName.of("");
            
            assertThat(modelName.fullName(), is(""));
            }
        }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests
        {
        @Test
        @DisplayName("should handle unicode characters")
        void shouldHandleUnicodeCharacters()
            {
            ModelName modelName = new ModelName("Provider-中文/model-日本語");
            
            assertThat(modelName.provider(), is("Provider-中文"));
            assertThat(modelName.name(), is("model-日本語"));
            }

        @Test
        @DisplayName("should handle very long model names")
        void shouldHandleVeryLongModelNames()
            {
            String longProvider = "a".repeat(100);
            String longModel = "b".repeat(100);
            String fullName = longProvider + "/" + longModel;
            
            ModelName modelName = new ModelName(fullName);
            
            assertThat(modelName.provider(), is(longProvider));
            assertThat(modelName.name(), is(longModel));
            }

        @Test
        @DisplayName("should handle special characters in components")
        void shouldHandleSpecialCharactersInComponents()
            {
            ModelName modelName = new ModelName("Provider_123-v2.0/model!@#$%^&*()");
            
            assertThat(modelName.provider(), is("Provider_123-v2.0"));
            assertThat(modelName.name(), is("model!@#$%^&*()"));
            }

        @Test
        @DisplayName("should handle trailing slash")
        void shouldHandleTrailingSlash()
            {
            ModelName modelName = new ModelName("Provider/model/");
            
            assertThat(modelName.provider(), is("Provider"));
            assertThat(modelName.name(), is("model"));
            }

        @Test
        @DisplayName("should handle leading slash")
        void shouldHandleLeadingSlash()
            {
            ModelName modelName = new ModelName("/Provider/model");
            
            assertThat(modelName.provider(), is(""));
            assertThat(modelName.name(), is("Provider"));
            }

        @Test
        @DisplayName("should handle only slash")
        void shouldHandleOnlySlash()
            {
            ModelName modelName = new ModelName("/");
            
            // Both provider() and name() throw exceptions (empty array)
            assertThrows(ArrayIndexOutOfBoundsException.class, modelName::provider);
            assertThrows(ArrayIndexOutOfBoundsException.class, modelName::name);
            }
        }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests
        {
        @Test
        @DisplayName("should be immutable record")
        void shouldBeImmutableRecord()
            {
            ModelName modelName = new ModelName("OpenAI/gpt-4");
            
            // Records are immutable by nature, verify we can't change the state
            assertThat(modelName.fullName(), is("OpenAI/gpt-4"));
            
            // Create a new instance and verify original is unchanged
            ModelName other = new ModelName("OCI/cohere.embed-english-v3.0");
            assertThat(modelName.fullName(), is("OpenAI/gpt-4"));
            assertThat(other.fullName(), is("OCI/cohere.embed-english-v3.0"));
            }

        @Test
        @DisplayName("should maintain same values across multiple calls")
        void shouldMaintainSameValuesAcrossMultipleCalls()
            {
            ModelName modelName = new ModelName("Provider/model-name");
            
            String provider1 = modelName.provider();
            String provider2 = modelName.provider();
            String name1 = modelName.name();
            String name2 = modelName.name();
            
            assertThat(provider1, is(provider2));
            assertThat(name1, is(name2));
            assertThat(provider1, is("Provider"));
            assertThat(name1, is("model-name"));
            }
        }
    }
