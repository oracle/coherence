/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.util;

import io.helidon.http.Status;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for creating mock HTTP servers for testing external API interactions.
 * <p/>
 * This class provides convenient methods for setting up mock servers that simulate
 * external services like vector stores, AI models, and document repositories.
 * It's designed to support comprehensive testing of HTTP client interactions
 * without requiring actual external services.
 * <p/>
 * Features:
 * <ul>
 * <li>Mock HTTP servers with configurable responses</li>
 * <li>Support for different HTTP status codes and content types</li>
 * <li>Automatic port allocation to avoid conflicts</li>
 * <li>Predefined response templates for common scenarios</li>
 * <li>Easy cleanup and resource management</li>
 * </ul>
 * <p/>
 * Usage examples:
 * <pre>{@code
 * // Create a mock vector store server
 * WebServer mockServer = MockServerUtils.createMockVectorStore(8080);
 * 
 * // Create a mock AI model server
 * WebServer aiServer = MockServerUtils.createMockAiModelServer(8081);
 * 
 * // Don't forget to stop servers after testing
 * mockServer.stop();
 * aiServer.stop();
 * }</pre>
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
public class MockServerUtils
    {
    // ---- constants -------------------------------------------------------

    /**
     * Base port range for automatic port allocation.
     */
    private static final int PORT_RANGE_START = 50000;
    private static final int PORT_RANGE_END = 59999;

    // ---- mock server creation methods -----------------------------------

    /**
     * Creates a mock vector store server with standard endpoints.
     * <p/>
     * This server simulates a vector store with endpoints for storing and
     * retrieving document chunks with embeddings.
     *
     * @param port the port to bind the server to (0 for automatic allocation)
     *
     * @return configured WebServer instance (not yet started)
     */
    public static WebServer createMockVectorStore(int port)
        {
        if (port == 0)
            {
            port = getRandomPort();
            }

        return WebServer.builder()
                .port(port)
                .routing(MockServerUtils::configureVectorStoreRoutes)
                .build();
        }

    /**
     * Creates a mock AI model server with embedding and chat endpoints.
     * <p/>
     * This server simulates AI model APIs with endpoints for generating
     * embeddings and chat completions.
     *
     * @param port the port to bind the server to (0 for automatic allocation)
     *
     * @return configured WebServer instance (not yet started)
     */
    public static WebServer createMockAiModelServer(int port)
        {
        if (port == 0)
            {
            port = getRandomPort();
            }

        return WebServer.builder()
                .port(port)
                .routing(MockServerUtils::configureAiModelRoutes)
                .build();
        }

    /**
     * Creates a mock HTTP document server for testing document loading.
     * <p/>
     * This server serves documents over HTTP with appropriate content types
     * and metadata headers.
     *
     * @param port the port to bind the server to (0 for automatic allocation)
     *
     * @return configured WebServer instance (not yet started)
     */
    public static WebServer createMockDocumentServer(int port)
        {
        if (port == 0)
            {
            port = getRandomPort();
            }

        return WebServer.builder()
                .port(port)
                .routing(MockServerUtils::configureDocumentRoutes)
                .build();
        }

    /**
     * Creates a customizable mock server with user-defined routing.
     * <p/>
     * This method provides a flexible way to create mock servers with
     * custom endpoints and behaviors for specific testing scenarios.
     *
     * @param port    the port to bind the server to (0 for automatic allocation)
     * @param routing custom routing configuration
     *
     * @return configured WebServer instance (not yet started)
     */
    public static WebServer createCustomMockServer(int port, HttpRouting.Builder routing)
        {
        if (port == 0)
            {
            port = getRandomPort();
            }

        return WebServer.builder()
                .port(port)
                .routing(routing)
                .build();
        }

    // ---- routing configuration methods ----------------------------------

    /**
     * Configures routes for a mock vector store server.
     *
     * @param routing the routing builder to configure
     */
    private static void configureVectorStoreRoutes(HttpRouting.Builder routing)
        {
        routing
            .post("/vectors", (req, res) -> {
                // Simulate successful vector storage
                res.status(Status.OK_200).send("{\"success\": true, \"count\": 10}");
            })
            .get("/vectors", (req, res) -> {
                // Simulate vector search results
                String results = """
                    {
                      "results": [
                        {
                          "id": "chunk-1",
                          "score": 0.95,
                          "text": "This is a test chunk with high similarity."
                        },
                        {
                          "id": "chunk-2",
                          "score": 0.82,
                          "text": "This is another relevant chunk."
                        }
                      ],
                      "total": 2
                    }
                    """;
                res.status(Status.OK_200).send(results);
            })
            .delete("/vectors", (req, res) -> {
                // Simulate successful deletion
                res.status(Status.NO_CONTENT_204).send();
            })
            .get("/health", (req, res) -> {
                // Health check endpoint
                res.status(Status.OK_200).send("{\"status\": \"healthy\"}");
            })
            .any("/error", (req, res) -> {
                // Simulate error responses
                res.status(Status.INTERNAL_SERVER_ERROR_500)
                   .send("{\"error\": \"Simulated server error\"}");
            });
        }

    /**
     * Configures routes for a mock AI model server.
     *
     * @param routing the routing builder to configure
     */
    private static void configureAiModelRoutes(HttpRouting.Builder routing)
        {
        routing
            .post("/embeddings", (req, res) -> {
                // Simulate embedding generation
                String embeddings = """
                    {
                      "data": [
                        {
                          "embedding": [0.1, 0.2, 0.3, 0.4, 0.5],
                          "index": 0
                        }
                      ],
                      "model": "test-embedding-model",
                      "usage": {
                        "total_tokens": 5
                      }
                    }
                    """;
                res.status(Status.OK_200).send(embeddings);
            })
            .post("/chat/completions", (req, res) -> {
                // Simulate chat completion
                String completion = """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "This is a test AI response based on the provided context.",
                            "role": "assistant"
                          },
                          "finish_reason": "stop",
                          "index": 0
                        }
                      ],
                      "model": "test-chat-model",
                      "usage": {
                        "total_tokens": 50
                      }
                    }
                    """;
                res.status(Status.OK_200).send(completion);
            })
            .post("/scoring", (req, res) -> {
                // Simulate document scoring
                String scores = """
                    {
                      "scores": [
                        {"document": "doc1", "score": 0.95},
                        {"document": "doc2", "score": 0.82}
                      ]
                    }
                    """;
                res.status(Status.OK_200).send(scores);
            })
            .get("/models", (req, res) -> {
                // List available models
                String models = """
                    {
                      "data": [
                        {"id": "test-embedding-model", "type": "embedding"},
                        {"id": "test-chat-model", "type": "chat"},
                        {"id": "test-scoring-model", "type": "scoring"}
                      ]
                    }
                    """;
                res.status(Status.OK_200).send(models);
            });
        }

    /**
     * Configures routes for a mock document server.
     *
     * @param routing the routing builder to configure
     */
    private static void configureDocumentRoutes(HttpRouting.Builder routing)
        {
        routing
            .get("/documents/test.pdf", (req, res) -> {
                // Simulate PDF document
                res.status(Status.OK_200)
                   .header("Content-Type", "application/pdf")
                   .header("Content-Length", "1024")
                   .send("Mock PDF content");
            })
            .get("/documents/test.txt", (req, res) -> {
                // Simulate text document
                res.status(Status.OK_200)
                   .header("Content-Type", "text/plain")
                   .header("Content-Length", "100")
                   .send("This is a mock text document for testing purposes.");
            })
            .get("/documents/test.json", (req, res) -> {
                // Simulate JSON document
                String json = """
                    {
                      "title": "Test Document",
                      "content": "This is test content",
                      "metadata": {
                        "author": "Test Author",
                        "created": "2025-01-01T00:00:00Z"
                      }
                    }
                    """;
                res.status(Status.OK_200)
                   .header("Content-Type", "application/json")
                   .send(json);
            })
            .get("/documents/notfound.txt", (req, res) -> {
                // Simulate 404 error
                res.status(Status.NOT_FOUND_404).send("Document not found");
            })
            .any("/documents/error", (req, res) -> {
                // Simulate server error
                res.status(Status.INTERNAL_SERVER_ERROR_500)
                   .send("Internal server error");
            });
        }

    // ---- utility methods ------------------------------------------------

    /**
     * Generates a random port number within the defined range.
     * <p/>
     * This method helps avoid port conflicts when running multiple
     * mock servers simultaneously during testing.
     *
     * @return a random port number
     */
    public static int getRandomPort()
        {
        return ThreadLocalRandom.current().nextInt(PORT_RANGE_START, PORT_RANGE_END + 1);
        }

    /**
     * Creates and starts a mock server, returning the server and port.
     * <p/>
     * This convenience method creates, starts, and returns a mock server
     * along with the actual port it's bound to.
     *
     * @param serverType the type of mock server to create
     *
     * @return MockServerInfo containing the server and port
     */
    public static MockServerInfo createAndStartMockServer(MockServerType serverType)
        {
        WebServer server = switch (serverType)
            {
            case VECTOR_STORE -> createMockVectorStore(0);
            case AI_MODEL -> createMockAiModelServer(0);
            case DOCUMENT -> createMockDocumentServer(0);
            };

        server.start();
        return new MockServerInfo(server, server.port());
        }

    // ---- inner classes --------------------------------------------------

    /**
     * Enumeration of supported mock server types.
     */
    public enum MockServerType
        {
        /**
         * Vector store mock server.
         */
        VECTOR_STORE,
        
        /**
         * AI model mock server.
         */
        AI_MODEL,
        
        /**
         * Document server mock.
         */
        DOCUMENT
        }

    /**
     * Record containing mock server information.
     * <p/>
     * This record encapsulates a running mock server and its bound port
     * for easy access during testing.
     *
     * @param server the WebServer instance
     * @param port   the port the server is bound to
     */
    public record MockServerInfo(WebServer server, int port)
        {
        /**
         * Gets the base URL for the mock server.
         *
         * @return the base URL (e.g., "http://localhost:12345")
         */
        public String getBaseUrl()
            {
            return "http://localhost:" + port;
            }

        /**
         * Stops the mock server and releases resources.
         */
        public void stop()
            {
            server.stop();
            }
        }

    // ---- private constructor --------------------------------------------

    /**
     * Private constructor to prevent instantiation.
     */
    private MockServerUtils()
        {
        }
    } 
