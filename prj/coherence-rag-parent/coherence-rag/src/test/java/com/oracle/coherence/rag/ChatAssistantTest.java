/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag;

import dev.langchain4j.service.TokenStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Comprehensive unit tests for the ChatAssistant interface.
 * <p/>
 * This test class validates the chat assistant functionality including
 * question processing and response generation using streaming tokens.
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@DisplayName("ChatAssistant Tests")
class ChatAssistantTest
    {
    @Mock
    private TokenStream mockTokenStream;

    private TestChatAssistant testChatAssistant;

    @BeforeEach
    void setUp()
        {
        MockitoAnnotations.openMocks(this);
        testChatAssistant = new TestChatAssistant();
        }

    @Test
    @DisplayName("Should return token stream for valid question")
    void shouldReturnTokenStreamForValidQuestion()
        {
        // Arrange
        String question = "What is machine learning?";

        // Act
        TokenStream response = testChatAssistant.answer(question);

        // Assert
        assertThat(response, is(notNullValue()));
        }

    @Test
    @DisplayName("Should throw exception for null question")
    void shouldThrowExceptionForNullQuestion()
        {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            testChatAssistant.answer(null);
        });
        }

    @Test
    @DisplayName("Should throw exception for empty question")
    void shouldThrowExceptionForEmptyQuestion()
        {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            testChatAssistant.answer("");
        });
        }

    // ---- test implementation --------------------------------------------

    /**
     * Test implementation of ChatAssistant for testing purposes.
     */
    private class TestChatAssistant implements ChatAssistant
        {
        @Override
        public TokenStream answer(String question)
            {
            if (question == null || question.trim().isEmpty())
                {
                throw new IllegalArgumentException("Question cannot be null or empty");
                }
            return mockTokenStream;
            }
        }
    } 
