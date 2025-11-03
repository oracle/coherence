/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.oci;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import com.oracle.bmc.generativeaiinference.model.AssistantMessage;
import com.oracle.bmc.generativeaiinference.model.BaseChatRequest;
import com.oracle.bmc.generativeaiinference.model.BaseChatResponse;
import com.oracle.bmc.generativeaiinference.model.ChatChoice;
import com.oracle.bmc.generativeaiinference.model.ChatContent;
import com.oracle.bmc.generativeaiinference.model.ChatDetails;
import com.oracle.bmc.generativeaiinference.model.CohereChatBotMessage;
import com.oracle.bmc.generativeaiinference.model.CohereChatRequest;
import com.oracle.bmc.generativeaiinference.model.CohereChatResponse;
import com.oracle.bmc.generativeaiinference.model.CohereMessage;
import com.oracle.bmc.generativeaiinference.model.CohereSystemMessage;
import com.oracle.bmc.generativeaiinference.model.CohereToolCall;
import com.oracle.bmc.generativeaiinference.model.CohereToolMessage;
import com.oracle.bmc.generativeaiinference.model.CohereToolResult;
import com.oracle.bmc.generativeaiinference.model.CohereUserMessage;
import com.oracle.bmc.generativeaiinference.model.DedicatedServingMode;
import com.oracle.bmc.generativeaiinference.model.GenericChatRequest;
import com.oracle.bmc.generativeaiinference.model.GenericChatResponse;
import com.oracle.bmc.generativeaiinference.model.Message;
import com.oracle.bmc.generativeaiinference.model.OnDemandServingMode;
import com.oracle.bmc.generativeaiinference.model.ServingMode;
import com.oracle.bmc.generativeaiinference.model.TextContent;
import com.oracle.bmc.generativeaiinference.requests.ChatRequest;
import com.oracle.bmc.generativeaiinference.responses.ChatResponse;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.FinishReason;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;

/**
 * OCI GenAI chat model implementation.
 * <p/>
 * This class implements the LangChain4J ChatModel interface for Oracle Cloud 
 * Infrastructure's Generative AI service. It provides conversational AI capabilities
 * using foundation models deployed on OCI, with specialized support for both
 * Cohere Command models and generic chat models.
 * <p/>
 * The model supports both on-demand and dedicated serving modes:
 * <ul>
 * <li>On-demand mode - Uses pre-deployed models identified by model ID</li>
 * <li>Dedicated mode - Uses custom model endpoints identified by endpoint OCID</li>
 * </ul>
 * <p/>
 * Key features:
 * <ul>
 * <li>Automatic model-specific message format conversion</li>
 * <li>Support for system, user, AI, and tool execution result messages</li>
 * <li>Configurable token limits and echo settings</li>
 * <li>Comprehensive error handling and finish reason mapping</li>
 * </ul>
 * <p/>
 * The implementation automatically detects Cohere models (by model name prefix)
 * and applies appropriate message formatting and request structure.
 * <p/>
 * Example usage:
 * <pre>{@code
 * OciGenAiChatModel model = OciGenAiChatModel.builder(config)
 *     .compartmentId("ocid1.compartment.oc1.....")
 *     .modelName("cohere.command-r-08-2024")
 *     .build();
 * 
 * ChatRequest request = ChatRequest.builder()
 *     .messages(List.of(
 *         SystemMessage.from("You are a helpful assistant."),
 *         UserMessage.from("Hello, how are you?")
 *     ))
 *     .build();
 * 
 * ChatResponse response = model.doChat(request);
 * }</pre>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
public class OciGenAiChatModel
        implements ChatModel
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Constructs an OCI GenAI chat model with the specified configuration.
     *
     * @param client        the OCI GenerativeAI client
     * @param compartmentId the OCI compartment ID
     * @param servingMode   the serving mode (on-demand or dedicated)
     * @param modelName     the model name or endpoint ID
     * @param fEcho         whether to echo input messages in response
     */
    private OciGenAiChatModel(GenerativeAiInferenceClient client, String compartmentId, ServingMode servingMode, String modelName, boolean fEcho)
        {
        this.client = client;
        this.compartmentId = compartmentId;
        this.servingMode = servingMode;
        this.modelName = modelName;
        this.fEcho = fEcho;
        }

    // ---- ChatModel interface implementation -----------------------------

    /**
     * {@inheritDoc}
     * <p/>
     * Processes a chat request using OCI GenAI service. The method automatically
     * determines the appropriate message format based on the model type and
     * handles the conversion between LangChain4J and OCI message formats.
     *
     * @param chatRequest the chat request containing messages and configuration
     *
     * @return chat response with AI-generated message and finish reason
     *
     * @throws IllegalStateException if an unknown response type is returned
     */
    public dev.langchain4j.model.chat.response.ChatResponse doChat(dev.langchain4j.model.chat.request.ChatRequest chatRequest)
        {
        ChatDetails details = ChatDetails.builder()
                .servingMode(servingMode)
                .compartmentId(compartmentId)
                .chatRequest(createChatRequest(chatRequest.messages()))
                .build();

        ChatRequest  request  = ChatRequest.builder().chatDetails(details).build();
        ChatResponse response = client.chat(request);

        BaseChatResponse chatResponse = response.getChatResult().getChatResponse();
        if (chatResponse instanceof CohereChatResponse cohere)
            {
            return dev.langchain4j.model.chat.response.ChatResponse.builder()
                    .aiMessage(AiMessage.from(cohere.getText()))
                    .finishReason(cohereFinishReason(cohere.getFinishReason()))
                    .build();
            }
        else if (chatResponse instanceof GenericChatResponse generic)
            {
            return dev.langchain4j.model.chat.response.ChatResponse.builder()
                    .aiMessage(AiMessage.from(choicesToText(generic.getChoices())))
                    .build();
            }

        throw new IllegalStateException("unknown response type: " + chatResponse.getClass().getName());
        }

    // ---- message conversion methods --------------------------------------

    /**
     * Creates an OCI chat request based on the model type.
     *
     * @param messages the list of chat messages
     *
     * @return appropriate OCI chat request (Cohere or generic)
     */
    private BaseChatRequest createChatRequest(List<ChatMessage> messages)
        {
        return modelName.startsWith("cohere.")
               ? createCohereChatRequest(messages)
               : createGenericChatRequest(messages);
        }

    /**
     * Creates a generic OCI chat request for non-Cohere models.
     *
     * @param messages the list of chat messages
     *
     * @return configured GenericChatRequest
     */
    private GenericChatRequest createGenericChatRequest(List<ChatMessage> messages)
        {
        return GenericChatRequest.builder()
                .isStream(false)
                .isEcho(fEcho)
                .messages(messages.stream().map(this::genericMessage).toList())
                .build();
        }

    /**
     * Converts a LangChain4J ChatMessage to an OCI generic Message.
     *
     * @param message the LangChain4J chat message
     *
     * @return corresponding OCI Message
     *
     * @throws IllegalStateException if message type is not supported
     */
    private Message genericMessage(ChatMessage message)
        {
        return switch (message.type())
            {
            case SYSTEM -> com.oracle.bmc.generativeaiinference.model.SystemMessage.builder().content(chatContent(((SystemMessage) message).text())).build();
            case AI     -> AssistantMessage.builder().content(chatContent(((AiMessage) message).text())).build();
            case USER   -> com.oracle.bmc.generativeaiinference.model.UserMessage.builder().content(chatContent(((UserMessage) message).singleText())).build();
            default     -> throw new IllegalStateException("Unexpected value: " + message.type());
            };
        }

    /**
     * Converts text content to OCI ChatContent format.
     *
     * @param text the text content
     *
     * @return list containing a single TextContent instance
     */
    private List<ChatContent> chatContent(String text)
        {
        return List.of(TextContent.builder().text(text).build());
        }

    /**
     * Creates a Cohere-specific chat request.
     *
     * @param messages the list of chat messages
     *
     * @return configured CohereChatRequest
     */
    private CohereChatRequest createCohereChatRequest(List<ChatMessage> messages)
        {
        List<CohereMessage> chatHistory = cohereChatHistory(messages);
        CohereChatRequest.Builder builder = CohereChatRequest.builder()
                .isStream(false)
                .isEcho(fEcho);

        if (!chatHistory.isEmpty())
            {
            builder.chatHistory(chatHistory);
            }

        builder.message(((UserMessage) messages.getLast()).singleText());

        return builder.build();
        }

    /**
     * Extracts chat history for Cohere models (all messages except the last).
     *
     * @param messages the complete list of chat messages
     *
     * @return list of Cohere messages representing chat history
     */
    private List<CohereMessage> cohereChatHistory(List<ChatMessage> messages)
        {
        if (messages.size() <= 1)
            {
            return Collections.emptyList();
            }

        List<CohereMessage> history = new ArrayList<>();
        for (int i = 0; i < messages.size() - 1; i++)
            {
            history.add(cohereMessage(messages.get(i)));
            }
        return history;
        }

    /**
     * Converts a LangChain4J ChatMessage to a Cohere-specific message.
     *
     * @param message the LangChain4J chat message
     *
     * @return corresponding CohereMessage
     *
     * @throws IllegalStateException if message type is not supported
     */
    private CohereMessage cohereMessage(ChatMessage message)
        {
        return switch (message.type())
            {
            case SYSTEM                -> CohereSystemMessage.builder().message(((SystemMessage) message).text()).build();
            case AI                    -> CohereChatBotMessage.builder().message(((AiMessage) message).text()).build();
            case USER                  -> CohereUserMessage.builder().message(((UserMessage) message).singleText()).build();
            case TOOL_EXECUTION_RESULT -> CohereToolMessage.builder().toolResults(toolResults((ToolExecutionResultMessage) message)).build();
            default                    -> throw new IllegalStateException("Unexpected value: " + message.type());
            };
        }

    /**
     * Converts tool execution result message to Cohere tool results.
     *
     * @param message the tool execution result message
     *
     * @return list of CohereToolResult instances
     */
    private List<CohereToolResult> toolResults(ToolExecutionResultMessage message)
        {
        return List.of(
                CohereToolResult.builder()
                        .call(CohereToolCall.builder().name(message.toolName()).build())
                        .outputs(List.of(message.text()))
                        .build()
                );
        }

    /**
     * Extracts text content from generic chat choices.
     *
     * @param choices the list of chat choices
     *
     * @return concatenated text content from all choices
     */
    private String choicesToText(List<ChatChoice> choices)
        {
        return choices.stream()
                .map(ChatChoice::getMessage)
                .map(Message::getContent)
                .flatMap(List::stream)
                .map(content -> ((TextContent) content).getText())
                .collect(Collectors.joining());
        }

    /**
     * Maps Cohere finish reason to LangChain4J finish reason.
     *
     * @param reason the Cohere finish reason
     *
     * @return corresponding LangChain4J finish reason
     */
    private dev.langchain4j.model.output.FinishReason cohereFinishReason(CohereChatResponse.FinishReason reason)
        {
        return switch (reason)
            {
            case Complete   -> FinishReason.STOP;
            case MaxTokens  -> FinishReason.LENGTH;
            case ErrorToxic -> FinishReason.CONTENT_FILTER;
            default -> FinishReason.OTHER;
            };
        }

    // ---- factory methods -------------------------------------------------

    /**
     * Creates a new builder instance for constructing OciGenAiChatModel.
     *
     * @param config Eclipse MP configuration
     *
     * @return new Builder instance
     */
    public static Builder builder(Config config)
        {
        return new Builder(config);
        }

    // ---- inner classes ---------------------------------------------------

    /**
     * Builder class for constructing OciGenAiChatModel instances.
     * <p/>
     * The builder supports various configuration options including authentication,
     * model selection, and processing parameters. It automatically detects the
     * appropriate serving mode based on the model name format.
     */
    public static class Builder
            extends AbstractOciModelBuilder
        {
        // ---- constructors -------------------------------------------------

        /**
         * Construct Builder instance.
         *
         * @param config  Eclipse MP configuration
         */
        public Builder(Config config)
            {
            super(config);
            }

        // ---- Builder API --------------------------------------------------

        /**
         * Default chat model name.
         */
        private final static String DEFAULT_MODEL = "cohere.command-r-08-2024";

        /**
         * Creates a new OciGenAiChatModel instance with the configured parameters.
         *
         * @return configured OciGenAiChatModel instance
         */
        public OciGenAiChatModel build()
            {
            return new OciGenAiChatModel(
                    GenerativeAiInferenceClient.builder()
                            .endpoint(baseUrl())
                            .build(authenticationDetailsProvider()),
                    compartmentId(),
                    servingMode(),
                    modelName(),
                    echo()
                );
            }

        /**
         * Returns the authentication details provider.
         *
         * @return the authentication provider
         */
        public AbstractAuthenticationDetailsProvider authenticationDetailsProvider()
            {
            return authenticationDetailsProvider == null
                   ? super.authenticationDetailsProvider()
                   : authenticationDetailsProvider;
            }

        /**
         * Sets the authentication details provider.
         *
         * @param authenticationDetailsProvider the authentication provider
         *
         * @return this builder instance
         */
        public Builder authenticationDetailsProvider(AbstractAuthenticationDetailsProvider authenticationDetailsProvider)
            {
            this.authenticationDetailsProvider = authenticationDetailsProvider;
            return this;
            }

        /**
         * Returns the OCI compartment ID.
         *
         * @return the compartment ID
         */
        public String compartmentId()
            {
            return compartmentId;
            }

        /**
         * Sets the OCI compartment ID.
         *
         * @param compartmentId the compartment ID
         *
         * @return this builder instance
         */
        public Builder compartmentId(String compartmentId)
            {
            this.compartmentId = compartmentId;
            return this;
            }

        /**
         * Returns the model name.
         *
         * @return the model name or default if not set
         */
        public String modelName()
            {
            return modelName == null
                    ? DEFAULT_MODEL
                    : modelName;
            }

        /**
         * Sets the model name.
         *
         * @param modelName the model name
         *
         * @return this builder instance
         */
        public Builder modelName(String modelName)
            {
            this.modelName = modelName;
            return this;
            }

        /**
         * Returns the serving mode, automatically determining it from model name if not set.
         *
         * @return the serving mode
         */
        public ServingMode servingMode()
            {
            return servingMode == null
                   ? modelName().startsWith("ocid1.")
                      ? DedicatedServingMode.builder().endpointId(modelName()).build()
                      : OnDemandServingMode.builder().modelId(modelName()).build()
                   : servingMode;
            }

        /**
         * Sets the serving mode.
         *
         * @param servingMode the serving mode
         *
         * @return this builder instance
         */
        public Builder servingMode(ServingMode servingMode)
            {
            this.servingMode = servingMode;
            return this;
            }

        /**
         * Returns whether to echo input messages in response.
         *
         * @return true if echo is enabled
         */
        public boolean echo()
            {
            return fEcho;
            }

        /**
         * Sets whether to echo input messages in response.
         *
         * @param fEcho true to enable echo
         *
         * @return this builder instance
         */
        public Builder echo(boolean fEcho)
            {
            this.fEcho = fEcho;
            return this;
            }

        // ---- data members ------------------------------------------------

        /**
         * Authentication details provider for OCI.
         */
        private AbstractAuthenticationDetailsProvider authenticationDetailsProvider;

        /**
         * OCI compartment ID.
         */
        private String compartmentId;

        /**
         * Model name.
         */
        private String modelName;

        /**
         * Serving mode configuration.
         */
        private ServingMode servingMode;

        /**
         * Whether to echo input messages.
         */
        private boolean fEcho;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The OCI GenerativeAI client.
     */
    private final GenerativeAiInferenceClient client;

    /**
     * The OCI compartment ID.
     */
    private final String compartmentId;

    /**
     * The serving mode (on-demand or dedicated).
     */
    private final ServingMode servingMode;

    /**
     * The model name or endpoint ID.
     */
    private final String modelName;

    /**
     * Whether to echo input messages in response.
     */
    private final boolean fEcho;
    }
