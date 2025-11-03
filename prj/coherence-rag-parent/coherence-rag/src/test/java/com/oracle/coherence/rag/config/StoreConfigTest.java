/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.config;

import com.oracle.coherence.rag.config.index.BinaryQuantIndexConfig;
import com.oracle.coherence.rag.config.index.HnswIndexConfig;
import com.oracle.coherence.rag.config.index.IndexConfig;

import com.oracle.coherence.rag.config.index.SimpleIndexConfig;
import com.oracle.coherence.rag.internal.json.StoreConfigDeserializer;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import jakarta.json.bind.JsonbConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link StoreConfig} class.
 * <p/>
 * This test class validates the configuration management including constructor validation,
 * fluent API functionality, POF serialization, JSON serialization, and equals/hashCode contract.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@DisplayName("StoreConfig")
class StoreConfigTest
    {
    @Nested
    @DisplayName("Construction")
    class ConstructionTests
        {
        @Test
        @DisplayName("should create instance with default constructor")
        void shouldCreateInstanceWithDefaultConstructor()
            {
            StoreConfig config = new StoreConfig();
            
            assertThat(config, is(notNullValue()));
            assertThat(config.getEmbeddingModel(), is(nullValue()));
            assertThat(config.isNormalizeEmbeddings(), is(false));
            assertThat(config.getIndex(), is(nullValue()));
            assertThat(config.getChunkSize(), is(0));
            assertThat(config.getChunkOverlap(), is(0));
            }

        @Test
        @DisplayName("should create instance with all parameters")
        void shouldCreateInstanceWithAllParameters()
            {
            IndexConfig<?> index = new SimpleIndexConfig();
            
            StoreConfig config = new StoreConfig(
                "openai/text-embedding-ada-002",
                true,
                index,
                1000,
                100
            );
            
            assertThat(config.getEmbeddingModel(), is("openai/text-embedding-ada-002"));
            assertThat(config.isNormalizeEmbeddings(), is(true));
            assertThat(config.getIndex(), is(sameInstance(index)));
            assertThat(config.getChunkSize(), is(1000));
            assertThat(config.getChunkOverlap(), is(100));
            }

        @Test
        @DisplayName("should create instance with null values")
        void shouldCreateInstanceWithNullValues()
            {
            StoreConfig config = new StoreConfig(null, false, null, 0, 0);
            
            assertThat(config.getEmbeddingModel(), is(nullValue()));
            assertThat(config.isNormalizeEmbeddings(), is(false));
            assertThat(config.getIndex(), is(nullValue()));
            assertThat(config.getChunkSize(), is(0));
            assertThat(config.getChunkOverlap(), is(0));
            }
        }

    @Nested
    @DisplayName("Fluent API")
    class FluentApiTests
        {
        @Test
        @DisplayName("should support method chaining for all setters")
        void shouldSupportMethodChaining()
            {
            IndexConfig<?> index = new HnswIndexConfig();

            StoreConfig config = new StoreConfig()
                .setEmbeddingModel("oci/cohere.embed-english-v3.0")
                .setNormalizeEmbeddings(true)
                .setIndex(index)
                .setChunkSize(2000)
                .setChunkOverlap(200);
            
            assertThat(config.getEmbeddingModel(), is("oci/cohere.embed-english-v3.0"));
            assertThat(config.isNormalizeEmbeddings(), is(true));
            assertThat(config.getIndex(), is(sameInstance(index)));
            assertThat(config.getChunkSize(), is(2000));
            assertThat(config.getChunkOverlap(), is(200));
            }

        @Test
        @DisplayName("should return same instance for method chaining")
        void shouldReturnSameInstanceForMethodChaining()
            {
            StoreConfig config = new StoreConfig();
            
            assertThat(config.setEmbeddingModel("test"), is(sameInstance(config)));
            assertThat(config.setNormalizeEmbeddings(true), is(sameInstance(config)));
            assertThat(config.setIndex(new IndexConfig<>()), is(sameInstance(config)));
            assertThat(config.setChunkSize(100), is(sameInstance(config)));
            assertThat(config.setChunkOverlap(50), is(sameInstance(config)));
            }
        }

    @Nested
    @DisplayName("Getters and Accessors")
    class GettersAndAccessorsTests
        {
        @Test
        @DisplayName("should have dual accessor methods")
        void shouldHaveDualAccessorMethods()
            {
            IndexConfig<?> index = new BinaryQuantIndexConfig();
            StoreConfig config = new StoreConfig(
                "ollama/llama2",
                true,
                index,
                1500,
                150
            );
            
            // Test traditional getters
            assertThat(config.getEmbeddingModel(), is("ollama/llama2"));
            assertThat(config.isNormalizeEmbeddings(), is(true));
            assertThat(config.getIndex(), is(sameInstance(index)));
            assertThat(config.getChunkSize(), is(1500));
            assertThat(config.getChunkOverlap(), is(150));

            // Test functional-style accessors
            assertThat(config.getEmbeddingModel(), is("ollama/llama2"));
            assertThat(config.isNormalizeEmbeddings(), is(true));
            assertThat(config.getIndex(), is(sameInstance(index)));
            assertThat(config.getChunkSize(), is(1500));
            assertThat(config.getChunkOverlap(), is(150));
            }

        @Test
        @DisplayName("should handle edge case values")
        void shouldHandleEdgeCaseValues()
            {
            StoreConfig config = new StoreConfig(
                "",
                false,
                new IndexConfig<>(),
                -1,
                -1
            );
            
            assertThat(config.getEmbeddingModel(), is(""));
            assertThat(config.isNormalizeEmbeddings(), is(false));
            assertThat(config.getIndex().type(), is("NONE"));
            assertThat(config.getChunkSize(), is(-1));
            assertThat(config.getChunkOverlap(), is(-1));
            }
        }

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityAndHashCodeTests
        {
        @Test
        @DisplayName("should be equal for same values")
        void shouldBeEqualForSameValues()
            {
            IndexConfig<?> index1 = new IndexConfig<>();
            IndexConfig<?> index2 = new IndexConfig<>();
            
            StoreConfig config1 = new StoreConfig(
                "openai/text-embedding-ada-002",
                true,
                index1,
                1000,
                100
            );
            
            StoreConfig config2 = new StoreConfig(
                "openai/text-embedding-ada-002",
                true,
                index2,
                1000,
                100
            );
            
            assertThat(config1, is(equalTo(config2)));
            assertThat(config1.hashCode(), is(equalTo(config2.hashCode())));
            }

        @Test
        @DisplayName("should not be equal for different values")
        void shouldNotBeEqualForDifferentValues()
            {
            IndexConfig<?> index = new IndexConfig<>();
            
            StoreConfig config1 = new StoreConfig(
                "openai/text-embedding-ada-002",
                true,
                index,
                1000,
                100
            );
            
            StoreConfig config2 = new StoreConfig(
                "oci/cohere.embed-english-v3.0",
                true,
                index,
                1000,
                100
            );
            
            assertThat(config1, is(not(equalTo(config2))));
            }

        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself()
            {
            StoreConfig config = new StoreConfig(
                "test",
                true,
                new IndexConfig<>(),
                100,
                50
            );
            
            assertThat(config, is(equalTo(config)));
            }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull()
            {
            StoreConfig config = new StoreConfig();
            
            assertThat(config, is(not(equalTo(null))));
            }

        @Test
        @DisplayName("should not be equal to different class")
        void shouldNotBeEqualToDifferentClass()
            {
            StoreConfig config = new StoreConfig();
            
            assertThat(config, is(not(equalTo("not a StoreConfig"))));
            }

        @Test
        @DisplayName("should handle null fields in equality")
        void shouldHandleNullFieldsInEquality()
            {
            StoreConfig config1 = new StoreConfig(null, false, null, 0, 0);
            StoreConfig config2 = new StoreConfig(null, false, null, 0, 0);
            
            assertThat(config1, is(equalTo(config2)));
            assertThat(config1.hashCode(), is(equalTo(config2.hashCode())));
            }
        }

    @Nested
    @DisplayName("String Representation")
    class StringRepresentationTests
        {
        @Test
        @DisplayName("should include all fields in toString")
        void shouldIncludeAllFieldsInToString()
            {
            IndexConfig<?> index = new HnswIndexConfig();
            StoreConfig config = new StoreConfig(
                "openai/text-embedding-ada-002",
                true,
                index,
                1000,
                100
            );
            
            String toString = config.toString();
            
            assertThat(toString, containsString("StoreConfig"));
            assertThat(toString, containsString("openai/text-embedding-ada-002"));
            assertThat(toString, containsString("true"));
            assertThat(toString, containsString("1000"));
            assertThat(toString, containsString("100"));
            }

        @Test
        @DisplayName("should handle null values in toString")
        void shouldHandleNullValuesInToString()
            {
            StoreConfig config = new StoreConfig(null, false, null, 0, 0);
            
            String toString = config.toString();
            
            assertThat(toString, containsString("StoreConfig"));
            assertThat(toString, containsString("null"));
            assertThat(toString, containsString("false"));
            assertThat(toString, containsString("0"));
            }
        }

    @Nested
    @DisplayName("POF Serialization")
    class PofSerializationTests
        {
        @Test
        @DisplayName("should have correct implementation version")
        void shouldHaveCorrectImplementationVersion()
            {
            StoreConfig config = new StoreConfig();
            
            assertThat(config.getImplVersion(), is(StoreConfig.IMPLEMENTATION_VERSION));
            assertThat(config.getImplVersion(), is(1));
            }

        @Test
        @DisplayName("should round-trip through POF serialization with all fields")
        void shouldRoundTripThroughPofSerializationWithAllFields()
            {
            IndexConfig<?> index = new BinaryQuantIndexConfig();
            StoreConfig original = new StoreConfig(
                "ollama/llama2",
                true,
                index,
                1500,
                150
            );
            
            // Serialize to binary
            ConfigurablePofContext pofContext = new ConfigurablePofContext("coherence-rag-pof-config.xml");
            Binary binary = ExternalizableHelper.toBinary(original, pofContext);
            
            // Deserialize from binary
            StoreConfig deserialized = ExternalizableHelper.fromBinary(binary, pofContext);
            
            // Verify all fields are preserved
            assertThat(deserialized, is(equalTo(original)));
            assertThat(deserialized.getEmbeddingModel(), is(original.getEmbeddingModel()));
            assertThat(deserialized.isNormalizeEmbeddings(), is(original.isNormalizeEmbeddings()));
            assertThat(deserialized.getIndex(), is(equalTo(original.getIndex())));
            assertThat(deserialized.getChunkSize(), is(original.getChunkSize()));
            assertThat(deserialized.getChunkOverlap(), is(original.getChunkOverlap()));
            }

        @Test
        @DisplayName("should round-trip through POF serialization with null values")
        void shouldRoundTripThroughPofSerializationWithNullValues()
            {
            StoreConfig original = new StoreConfig(null, false, null, 0, 0);
            
            // Serialize to binary
            ConfigurablePofContext pofContext = new ConfigurablePofContext("coherence-rag-pof-config.xml");
            Binary binary = ExternalizableHelper.toBinary(original, pofContext);
            
            // Deserialize from binary
            StoreConfig deserialized = ExternalizableHelper.fromBinary(binary, pofContext);
            
            // Verify all fields are preserved
            assertThat(deserialized, is(equalTo(original)));
            assertThat(deserialized.getEmbeddingModel(), is(nullValue()));
            assertThat(deserialized.isNormalizeEmbeddings(), is(false));
            assertThat(deserialized.getIndex(), is(nullValue()));
            assertThat(deserialized.getChunkSize(), is(0));
            assertThat(deserialized.getChunkOverlap(), is(0));
            }

        @Test
        @DisplayName("should round-trip through POF serialization with edge case values")
        void shouldRoundTripThroughPofSerializationWithEdgeCaseValues()
            {
            IndexConfig<?> index = new IndexConfig<>();
            StoreConfig original = new StoreConfig(
                "",
                false,
                index,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE
            );
            
            // Serialize to binary
            ConfigurablePofContext pofContext = new ConfigurablePofContext("coherence-rag-pof-config.xml");
            Binary binary = ExternalizableHelper.toBinary(original, pofContext);
            
            // Deserialize from binary
            StoreConfig deserialized = ExternalizableHelper.fromBinary(binary, pofContext);
            
            // Verify all fields are preserved
            assertThat(deserialized, is(equalTo(original)));
            assertThat(deserialized.getEmbeddingModel(), is(""));
            assertThat(deserialized.isNormalizeEmbeddings(), is(false));
            assertThat(deserialized.getIndex().type(), is("NONE"));
            assertThat(deserialized.getChunkSize(), is(Integer.MAX_VALUE));
            assertThat(deserialized.getChunkOverlap(), is(Integer.MIN_VALUE));
            }

        @Test
        @DisplayName("should preserve evolvability during POF serialization")
        void shouldPreserveEvolvabilityDuringPofSerialization()
            {
            IndexConfig<?> index = new HnswIndexConfig();
            StoreConfig original = new StoreConfig(
                "openai/text-embedding-ada-002",
                true,
                index,
                1000,
                100
            );
            
            // Serialize to binary
            ConfigurablePofContext pofContext = new ConfigurablePofContext("coherence-rag-pof-config.xml");
            Binary binary = ExternalizableHelper.toBinary(original, pofContext);
            
            // Deserialize from binary
            StoreConfig deserialized = ExternalizableHelper.fromBinary(binary, pofContext);
            
            // Verify implementation version is preserved
            assertThat(deserialized.getImplVersion(), is(original.getImplVersion()));
            assertThat(deserialized.getImplVersion(), is(StoreConfig.IMPLEMENTATION_VERSION));
            }
        }

    @Nested
    @DisplayName("JSON-B Serialization")
    class JsonbSerializationTests
        {
        private static final JsonbConfig CONFIG = new JsonbConfig()
                        .withDeserializers(new StoreConfigDeserializer());
    
        @Test
        @DisplayName("should round-trip through JSON-B serialization with all fields")
        void shouldRoundTripThroughJsonbSerializationWithAllFields()
            {
            IndexConfig<?> index = new IndexConfig<>();
            StoreConfig original = new StoreConfig(
                "openai/text-embedding-ada-002",
                true,
                index,
                1000,
                100
            );
            
            try (Jsonb jsonb = JsonbBuilder.create(CONFIG))
                {
                // Serialize to JSON
                String json = jsonb.toJson(original);
                
                // Verify JSON contains expected fields
                assertThat(json, containsString("\"embeddingModel\""));
                assertThat(json, containsString("\"normalizeEmbeddings\""));
                assertThat(json, containsString("\"index\""));
                assertThat(json, containsString("\"chunkSize\""));
                assertThat(json, containsString("\"chunkOverlap\""));
                
                // Deserialize from JSON
                StoreConfig deserialized = jsonb.fromJson(json, StoreConfig.class);
                
                // Verify all fields are preserved
                assertThat(deserialized, is(equalTo(original)));
                assertThat(deserialized.getEmbeddingModel(), is(original.getEmbeddingModel()));
                assertThat(deserialized.isNormalizeEmbeddings(), is(original.isNormalizeEmbeddings()));
                assertThat(deserialized.getIndex(), is(equalTo(original.getIndex())));
                assertThat(deserialized.getChunkSize(), is(original.getChunkSize()));
                assertThat(deserialized.getChunkOverlap(), is(original.getChunkOverlap()));
                }
            catch (Exception e)
                {
                throw new RuntimeException("JSON-B serialization failed", e);
                }
            }

        @Test
        @DisplayName("should round-trip through JSON-B serialization with null values")
        void shouldRoundTripThroughJsonbSerializationWithNullValues()
            {
            StoreConfig original = new StoreConfig(null, false, null, 0, 0);
            
            try (Jsonb jsonb = JsonbBuilder.create(CONFIG))
                {
                // Serialize to JSON
                String json = jsonb.toJson(original);
                
                // Deserialize from JSON
                StoreConfig deserialized = jsonb.fromJson(json, StoreConfig.class);
                
                // Verify all fields are preserved
                assertThat(deserialized, is(equalTo(original)));
                assertThat(deserialized.getEmbeddingModel(), is(nullValue()));
                assertThat(deserialized.isNormalizeEmbeddings(), is(false));
                assertThat(deserialized.getIndex(), is(nullValue()));
                assertThat(deserialized.getChunkSize(), is(0));
                assertThat(deserialized.getChunkOverlap(), is(0));
                }
            catch (Exception e)
                {
                throw new RuntimeException("JSON-B serialization failed", e);
                }
            }

        @Test
        @DisplayName("should round-trip through JSON-B serialization with edge case values")
        void shouldRoundTripThroughJsonbSerializationWithEdgeCaseValues()
            {
            IndexConfig<?> index = new IndexConfig<>();
            StoreConfig original = new StoreConfig(
                "",
                false,
                index,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE
            );
            
            try (Jsonb jsonb = JsonbBuilder.create(CONFIG))
                {
                // Serialize to JSON
                String json = jsonb.toJson(original);
                
                // Deserialize from JSON
                StoreConfig deserialized = jsonb.fromJson(json, StoreConfig.class);
                
                // Verify all fields are preserved
                assertThat(deserialized, is(equalTo(original)));
                assertThat(deserialized.getEmbeddingModel(), is(""));
                assertThat(deserialized.isNormalizeEmbeddings(), is(false));
                assertThat(deserialized.getIndex().type(), is("NONE"));
                assertThat(deserialized.getChunkSize(), is(Integer.MAX_VALUE));
                assertThat(deserialized.getChunkOverlap(), is(Integer.MIN_VALUE));
                }
            catch (Exception e)
                {
                throw new RuntimeException("JSON-B serialization failed", e);
                }
            }

        @Test
        @DisplayName("should serialize complex model names correctly")
        void shouldSerializeComplexModelNamesCorrectly()
            {
            String complexModelName = "custom-provider/model-name-with-dots.and-dashes_v2.0";
            IndexConfig<?> index = new HnswIndexConfig();
            StoreConfig original = new StoreConfig(
                complexModelName,
                true,
                index,
                2048,
                512
            );
            
            try (Jsonb jsonb = JsonbBuilder.create(CONFIG))
                {
                // Serialize to JSON
                String json = jsonb.toJson(original);
                
                // Verify complex names are properly escaped/handled
                assertThat(json, containsString(complexModelName));

                // Deserialize from JSON
                StoreConfig deserialized = jsonb.fromJson(json, StoreConfig.class);
                
                // Verify complex names are preserved
                assertThat(deserialized.getEmbeddingModel(), is(complexModelName));
                assertThat(deserialized.getIndex().type(), is("HNSW"));
                }
            catch (Exception e)
                {
                throw new RuntimeException("JSON-B serialization failed", e);
                }
            }

        @Test
        @DisplayName("should handle JSON deserialization from minimal JSON")
        void shouldHandleJsonDeserializationFromMinimalJson()
            {
            String minimalJson = "{\"embeddingModel\":\"test-model\",\"normalizeEmbeddings\":false,\"chunkSize\":500,\"chunkOverlap\":50}";
            
            try (Jsonb jsonb = JsonbBuilder.create(CONFIG))
                {
                StoreConfig deserialized = jsonb.fromJson(minimalJson, StoreConfig.class);
                
                assertThat(deserialized.getEmbeddingModel(), is("test-model"));
                assertThat(deserialized.isNormalizeEmbeddings(), is(false));
                assertThat(deserialized.getIndex(), is(nullValue()));
                assertThat(deserialized.getChunkSize(), is(500));
                assertThat(deserialized.getChunkOverlap(), is(50));
                }
            catch (Exception e)
                {
                throw new RuntimeException("JSON-B deserialization failed", e);
                }
            }
        }

    @Nested
    @DisplayName("Validation and Edge Cases")
    class ValidationAndEdgeCasesTests
        {
        @Test
        @DisplayName("should handle large chunk sizes")
        void shouldHandleLargeChunkSizes()
            {
            StoreConfig config = new StoreConfig()
                .setChunkSize(Integer.MAX_VALUE)
                .setChunkOverlap(Integer.MAX_VALUE);
            
            assertThat(config.getChunkSize(), is(Integer.MAX_VALUE));
            assertThat(config.getChunkOverlap(), is(Integer.MAX_VALUE));
            }

        @Test
        @DisplayName("should handle negative chunk sizes")
        void shouldHandleNegativeChunkSizes()
            {
            StoreConfig config = new StoreConfig()
                .setChunkSize(-1)
                .setChunkOverlap(-1);
            
            assertThat(config.getChunkSize(), is(-1));
            assertThat(config.getChunkOverlap(), is(-1));
            }

        @Test
        @DisplayName("should handle empty strings")
        void shouldHandleEmptyStrings()
            {
            StoreConfig config = new StoreConfig()
                .setEmbeddingModel("");
            
            assertThat(config.getEmbeddingModel(), is(""));
            }

        @Test
        @DisplayName("should handle complex model names")
        void shouldHandleComplexModelNames()
            {
            String complexModelName = "custom-provider/model-name-with-dots.and-dashes_v2.0";
            StoreConfig config = new StoreConfig()
                .setEmbeddingModel(complexModelName);
            
            assertThat(config.getEmbeddingModel(), is(complexModelName));
            }
        }
    }
