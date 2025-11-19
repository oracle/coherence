/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

/**
 * A record representing a structured model name with provider and model components.
 * <p/>
 * This record encapsulates AI model names in the format "provider/model" and provides
 * convenient methods to extract the provider and model name components. It serves as
 * a standardized way to identify and reference AI models across the Coherence RAG
 * framework.
 * <p/>
 * Model names follow the convention of "provider/modelName" where:
 * <ul>
 *   <li>provider - identifies the AI service provider (e.g., "OpenAI", "OCI", "Ollama")</li>
 *   <li>modelName - specifies the particular model within that provider's catalog</li>
 * </ul>
 * <p/>
 * Example model names:
 * <ul>
 *   <li>"OpenAI/text-embedding-ada-002"</li>
 *   <li>"OCI/cohere.embed-english-v3.0"</li>
 *   <li>"Ollama/llama2"</li>
 * </ul>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 * 
 * @param fullName the complete model name in "provider/model" format
 */
public record ModelName(String fullName)
    {
    /**
     * Creates a ModelName instance from a full model name string.
     * <p/>
     * This factory method provides a convenient way to create ModelName
     * instances while maintaining consistency with the record constructor.
     * 
     * @param fullName the complete model name in "provider/model" format
     * 
     * @return a new ModelName instance
     */
    public static ModelName of(String fullName)
        {
        return new ModelName(fullName);
        }
    
    /**
     * Extracts the provider component from the model name.
     * <p/>
     * This method returns the part of the model name before the "/" separator,
     * which identifies the AI service provider.
     * 
     * @return the provider name (e.g., "openai", "oci", "ollama")
     */
    public String provider()
        {
        return fullName.split("/")[0];
        }

    /**
     * Extracts the model name component from the full model name.
     * <p/>
     * This method returns the part of the model name after the "/" separator,
     * which identifies the specific model within the provider's catalog.
     * 
     * @return the model name (e.g., "text-embedding-ada-002", "llama2")
     */
    public String name()
        {
        return fullName.split("/")[1];
        }
    }
