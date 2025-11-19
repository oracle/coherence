/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.internal.json;

import com.oracle.coherence.rag.ModelProvider;
import com.oracle.coherence.rag.config.StoreConfig;
import com.oracle.coherence.rag.config.index.IndexConfig;
import com.oracle.coherence.rag.config.model.ChatModelConfig;
import com.oracle.coherence.rag.config.model.EmbeddingModelConfig;
import com.oracle.coherence.rag.config.model.ScoringModelConfig;
import com.oracle.coherence.rag.config.model.StreamingChatModelConfig;

import com.oracle.coherence.rag.model.ModelName;
import com.oracle.coherence.rag.util.CdiHelper;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

import java.lang.reflect.Type;

/**
 * JSON-B deserializer for {@link StoreConfig} that handles polymorphic
 * deserialization of nested model config objects using the appropriate
 * {@link ModelProvider}.
 * <p/>
 * This deserializer relies on the embedding model and chat model providers to
 * determine the correct configuration classes for each nested config object. It
 * assumes that {@link com.oracle.coherence.rag.config.index.IndexConfig} is
 * annotated with {@code @JsonbTypeInfo}, so it will be deserialized directly.
 *
 * @author Aleks Seovic  2025.07.13
 * @since 25.09
 */
public class StoreConfigDeserializer
        extends AbstractDeserializer
        implements JsonbDeserializer<StoreConfig>
    {
    @Override
    public StoreConfig deserialize(JsonParser parser, DeserializationContext ctx, Type rtType)
        {
        StoreConfig config = new StoreConfig();

        while (parser.hasNext())
            {
            Event event = parser.next();
            if (event == Event.KEY_NAME)
                {
                String key = parser.getString();
                switch (key)
                    {
                    case "chunkOverlap" ->
                            config.setChunkOverlap(getIntValue(parser));
                    case "chunkSize" ->
                            config.setChunkSize(getIntValue(parser));
                    case "embeddingModel" ->
                            config.setEmbeddingModel(getStringValue(parser));
                    case "normalizeEmbeddings" ->
                            config.setNormalizeEmbeddings(getBooleanValue(parser));

                    case "embeddingModelConfig" ->
                        {
                        ModelName     modelName = ModelName.of(config.getEmbeddingModel());
                        ModelProvider provider  = CdiHelper.getNamedBean(ModelProvider.class, modelName.provider());
                        if (provider != null)
                            {
                            //Class<? extends EmbeddingModelConfig> cls = provider.getEmbeddingModelConfigClass();
                            //config.setEmbeddingModelConfig(deserializeSubObject(parser, cls));
                            }
                        }

                    case "index" ->
                        {
                        // IndexConfig is polymorphic via @JsonbTypeInfo
                        config.setIndex(ctx.deserialize(IndexConfig.class, parser));
                        }

                    default -> parser.skipObject();
                    }
                }
            else if (event == Event.END_OBJECT)
                {
                break;
                }
            }

        return config;
        }

    }
