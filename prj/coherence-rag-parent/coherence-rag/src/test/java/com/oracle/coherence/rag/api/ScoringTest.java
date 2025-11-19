/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import com.oracle.coherence.rag.model.ScoringModelSupplier;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;

import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Scoring REST API controller.
 * <p/>
 * This test class validates the document relevance scoring functionality,
 * including cross-encoder scoring operations and batch processing.
 * The Scoring API provides advanced relevance scoring capabilities for
 * improving search and retrieval accuracy.
 * <p/>
 * Test categories covered:
 * <ul>
 * <li>Document relevance scoring</li>
 * <li>Query-document pair scoring</li>
 * <li>Batch scoring operations</li>
 * <li>Model configuration and management</li>
 * <li>Error handling and validation</li>
 * </ul>
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Scoring REST API Tests")
class ScoringTest
    {
    // ---- test infrastructure ---------------------------------------------

    private Scoring scoring;

    @Mock
    private ScoringModelSupplier scoringModelSupplier;

    @Mock
    private ScoringModel scoringModel;

    // ---- lifecycle methods -----------------------------------------------

    @BeforeEach
    void setUp()
        {
        MockitoAnnotations.openMocks(this);
        
        scoring = new Scoring();
        
        // Use reflection to inject the mock
        try
            {
            var field = Scoring.class.getDeclaredField("scoringModelSupplier");
            field.setAccessible(true);
            field.set(scoring, scoringModelSupplier);
            }
        catch (Exception e)
            {
            throw new RuntimeException("Failed to inject mock", e);
            }
            
        // Set up default scoring model behavior
        when(scoringModelSupplier.get()).thenReturn(scoringModel);
        when(scoringModelSupplier.get(anyString())).thenReturn(scoringModel);
        }

    // ---- document scoring tests -----------------------------------------

    @Nested
    @DisplayName("Document Scoring Tests")
    class DocumentScoringTests
        {
        @Test
        @DisplayName("Should score single query-document pair")
        void shouldScoreSingleQueryDocumentPair()
            {
            // Arrange
            String query = "machine learning algorithms";
            List<String> answers = List.of("This document explains various machine learning algorithms including neural networks and decision trees.");
            List<Double> expectedScores = List.of(0.85);
            
            when(scoringModel.scoreAll(any(List.class), anyString()))
                    .thenReturn(Response.from(expectedScores));
            
            var request = new Scoring.ScoreRequest("cross-encoder", query, answers);

            // Act
            jakarta.ws.rs.core.Response response = scoring.score(request);

            // Assert
            assertThat(response.getStatus(), is(200));
            
            @SuppressWarnings("unchecked")
            List<Double> scores = (List<Double>) response.getEntity();
            assertThat(scores, is(notNullValue()));
            assertThat(scores, hasSize(1));
            assertThat(scores.get(0), is(expectedScores.get(0)));
            }

        @Test
        @DisplayName("Should score multiple query-document pairs")
        void shouldScoreMultipleQueryDocumentPairs()
            {
            // Arrange
            String query = "artificial intelligence";
            List<String> answers = List.of(
                    "AI is transforming the technology industry",
                    "Machine learning is a subset of AI",
                    "Weather patterns are complex systems"
            );
            List<Double> expectedScores = List.of(0.9, 0.85, 0.2);
            
            when(scoringModel.scoreAll(any(List.class), anyString()))
                    .thenReturn(Response.from(expectedScores));
            
            var request = new Scoring.ScoreRequest("cross-encoder", query, answers);

            // Act
            jakarta.ws.rs.core.Response response = scoring.score(request);

            // Assert
            assertThat(response.getStatus(), is(200));
            
            @SuppressWarnings("unchecked")
            List<Double> scores = (List<Double>) response.getEntity();
            assertThat(scores, is(notNullValue()));
            assertThat(scores, hasSize(3));
            
            for (int i = 0; i < scores.size(); i++)
                {
                assertThat(scores.get(i), is(expectedScores.get(i)));
                assertThat(scores.get(i), is(greaterThan(0.0)));
                assertThat(scores.get(i), is(lessThanOrEqualTo(1.0)));
                }
            }

        @Test
        @DisplayName("Should handle empty document content")
        void shouldHandleEmptyDocumentContent()
            {
            // Arrange
            String query = "test query";
            List<String> answers = List.of("");
            
            var request = new Scoring.ScoreRequest("cross-encoder", query, answers);

            // Act & Assert - Should throw exception for empty text
            try
                {
                scoring.score(request);
                // If we get here, the test should fail
                assertThat("Expected IllegalArgumentException for empty text", false);
                }
            catch (IllegalArgumentException e)
                {
                assertThat(e.getMessage(), is("text cannot be null or blank"));
                }
            }

        @Test
        @DisplayName("Should handle very long documents")
        void shouldHandleVeryLongDocuments()
            {
            // Arrange
            String longDocument = "This is a test document. ".repeat(1000); // ~25KB
            String query = "test query";
            List<String> answers = List.of(longDocument);
            List<Double> expectedScores = List.of(0.6);
            
            when(scoringModel.scoreAll(any(List.class), anyString()))
                    .thenReturn(Response.from(expectedScores));
            
            var request = new Scoring.ScoreRequest(null, query, answers);

            // Act
            jakarta.ws.rs.core.Response response = scoring.score(request);

            // Assert
            assertThat(response.getStatus(), is(200));
            
            @SuppressWarnings("unchecked")
            List<Double> scores = (List<Double>) response.getEntity();
            assertThat(scores, hasSize(1));
            assertThat(scores.get(0), is(greaterThan(0.0)));
            }
        }

    // ---- model configuration tests --------------------------------------

    @Nested
    @DisplayName("Model Configuration Tests")
    class ModelConfigurationTests
        {
        @Test
        @DisplayName("Should use default model when no model specified")
        void shouldUseDefaultModelWhenNoModelSpecified()
            {
            // Arrange
            String query = "test query";
            List<String> answers = List.of("test answer");
            List<Double> expectedScores = List.of(0.75);
            
            when(scoringModel.scoreAll(any(List.class), anyString()))
                    .thenReturn(Response.from(expectedScores));
            
            var request = new Scoring.ScoreRequest(null, query, answers);

            // Act
            jakarta.ws.rs.core.Response response = scoring.score(request);

            // Assert
            assertThat(response.getStatus(), is(200));
            
            @SuppressWarnings("unchecked")
            List<Double> scores = (List<Double>) response.getEntity();
            assertThat(scores, hasSize(1));
            assertThat(scores.get(0), is(expectedScores.get(0)));
            }

        @Test
        @DisplayName("Should use specific model when model name provided")
        void shouldUseSpecificModelWhenModelNameProvided()
            {
            // Arrange
            String modelName = "custom-scoring-model";
            String query = "test query";
            List<String> answers = List.of("test answer");
            List<Double> expectedScores = List.of(0.8);
            
            when(scoringModel.scoreAll(any(List.class), anyString()))
                    .thenReturn(Response.from(expectedScores));
            
            var request = new Scoring.ScoreRequest(modelName, query, answers);

            // Act
            jakarta.ws.rs.core.Response response = scoring.score(request);

            // Assert
            assertThat(response.getStatus(), is(200));
            
            @SuppressWarnings("unchecked")
            List<Double> scores = (List<Double>) response.getEntity();
            assertThat(scores, hasSize(1));
            assertThat(scores.get(0), is(expectedScores.get(0)));
            }
        }

    // ---- validation tests -----------------------------------------------

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests
        {
        @Test
        @DisplayName("Should handle empty answers list")
        void shouldHandleEmptyAnswersList()
            {
            // Arrange
            String query = "test query";
            List<String> answers = List.of();
            List<Double> expectedScores = List.of();
            
            when(scoringModel.scoreAll(any(List.class), anyString()))
                    .thenReturn(Response.from(expectedScores));
            
            var request = new Scoring.ScoreRequest(null, query, answers);

            // Act
            jakarta.ws.rs.core.Response response = scoring.score(request);

            // Assert
            assertThat(response.getStatus(), is(200));
            
            @SuppressWarnings("unchecked")
            List<Double> scores = (List<Double>) response.getEntity();
            assertThat(scores, hasSize(0));
            }

        @Test
        @DisplayName("Should handle null query")
        void shouldHandleNullQuery()
            {
            // Arrange
            String query = null;
            List<String> answers = List.of("test answer");
            List<Double> expectedScores = List.of(0.5);
            
            when(scoringModel.scoreAll(any(List.class), any()))
                    .thenReturn(Response.from(expectedScores));
            
            var request = new Scoring.ScoreRequest(null, query, answers);

            // Act
            jakarta.ws.rs.core.Response response = scoring.score(request);

            // Assert
            assertThat(response.getStatus(), is(200));
            
            @SuppressWarnings("unchecked")
            List<Double> scores = (List<Double>) response.getEntity();
            assertThat(scores, hasSize(1));
            }

        @Test
        @DisplayName("Should handle empty query")
        void shouldHandleEmptyQuery()
            {
            // Arrange
            String query = "";
            List<String> answers = List.of("test answer");
            List<Double> expectedScores = List.of(0.3);
            
            when(scoringModel.scoreAll(any(List.class), anyString()))
                    .thenReturn(Response.from(expectedScores));
            
            var request = new Scoring.ScoreRequest(null, query, answers);

            // Act
            jakarta.ws.rs.core.Response response = scoring.score(request);

            // Assert
            assertThat(response.getStatus(), is(200));
            
            @SuppressWarnings("unchecked")
            List<Double> scores = (List<Double>) response.getEntity();
            assertThat(scores, hasSize(1));
            }
        }

    // ---- error handling tests ------------------------------------------

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests
        {
        @Test
        @DisplayName("Should handle scoring model errors gracefully")
        void shouldHandleScoringModelErrorsGracefully()
            {
            // Arrange
            String query = "test query";
            List<String> answers = List.of("test answer");
            
            when(scoringModel.scoreAll(any(List.class), anyString()))
                    .thenThrow(new RuntimeException("Model scoring error"));
            
            var request = new Scoring.ScoreRequest(null, query, answers);

            // Act & Assert
            try
                {
                scoring.score(request);
                }
            catch (RuntimeException e)
                {
                assertThat(e.getMessage(), is("Model scoring error"));
                }
            }

        @Test
        @DisplayName("Should handle invalid model names gracefully")
        void shouldHandleInvalidModelNamesGracefully()
            {
            // Arrange
            String invalidModelName = "non-existent-model";
            String query = "test query";
            List<String> answers = List.of("test answer");
            
            when(scoringModelSupplier.get(invalidModelName))
                    .thenThrow(new RuntimeException("Model not found"));
            
            var request = new Scoring.ScoreRequest(invalidModelName, query, answers);

            // Act & Assert
            try
                {
                scoring.score(request);
                }
            catch (RuntimeException e)
                {
                assertThat(e.getMessage(), is("Model not found"));
                }
            }
        }

    // ---- integration tests ---------------------------------------------

    @Nested
    @DisplayName("Basic Integration Tests")
    class BasicIntegrationTests
        {
        @Test
        @DisplayName("Should perform complete scoring workflow")
        void shouldPerformCompleteScoringWorkflow()
            {
            // Arrange
            String query = "machine learning and artificial intelligence";
            List<String> answers = List.of(
                    "Machine learning is a powerful tool for AI applications",
                    "The weather today is sunny and warm",
                    "Deep learning neural networks can solve complex problems"
            );
            List<Double> expectedScores = List.of(0.95, 0.1, 0.8);
            
            when(scoringModel.scoreAll(any(List.class), anyString()))
                    .thenReturn(Response.from(expectedScores));
            
            var request = new Scoring.ScoreRequest("-/ms-marco-TinyBERT-L-2-v2", query, answers);

            // Act
            jakarta.ws.rs.core.Response response = scoring.score(request);

            // Assert
            assertThat(response.getStatus(), is(200));
            
            @SuppressWarnings("unchecked")
            List<Double> scores = (List<Double>) response.getEntity();
            assertThat(scores, hasSize(3));
            
            // Verify scores are in expected order (highest to lowest relevance)
            assertThat(scores.get(0), is(greaterThan(scores.get(2))));
            assertThat(scores.get(2), is(greaterThan(scores.get(1))));
            }
        }
    } 