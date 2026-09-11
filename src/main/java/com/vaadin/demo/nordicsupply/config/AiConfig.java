package com.vaadin.demo.nordicsupply.config;

import java.util.function.Supplier;

import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.ai.provider.SpringAILLMProvider;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The pluggable model. Spring AI's auto-configured {@link ChatModel} (OpenAI by default, an OpenAI-compatible
 * local server with the {@code local} profile) is wrapped in the Vaadin provider. Each orchestrator gets its own
 * provider instance because the provider carries the conversation history.
 */
@Configuration
public class AiConfig {

    @Bean
    public Supplier<LLMProvider> llmProviders(ChatModel chatModel) {
        return () -> new SpringAILLMProvider(chatModel);
    }
}
