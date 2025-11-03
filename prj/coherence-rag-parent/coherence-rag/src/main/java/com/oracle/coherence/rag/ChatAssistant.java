/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag;

import dev.langchain4j.service.TokenStream;

/**
 * Interface for chat assistant functionality in the Coherence RAG framework.
 * <p/>
 * This interface defines the contract for creating AI-powered chat assistants
 * that can answer questions using retrieved augmented generation (RAG) techniques.
 * The assistant integrates with language models to provide intelligent responses
 * based on indexed document content.
 * <p/>
 * Implementations of this interface should handle question processing,
 * context retrieval, and response generation using appropriate AI models.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
public interface ChatAssistant
    {
    /**
     * Asks the assistant a question and returns a streaming response.
     * <p/>
     * This method processes the provided question, retrieves relevant context
     * from indexed documents, and generates a streaming response using an
     * AI language model. The response is returned as a TokenStream for
     * real-time processing and display.
     *
     * @param question the question text to be answered by the assistant
     *
     * @return a TokenStream containing the assistant's response tokens
     * 
     * @throws IllegalArgumentException if the question is null or empty
     * @throws RuntimeException if the AI model is unavailable or processing fails
     */
    TokenStream answer(String question);
    }
