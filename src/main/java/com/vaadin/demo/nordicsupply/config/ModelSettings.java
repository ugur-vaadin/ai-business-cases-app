package com.vaadin.demo.nordicsupply.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * What the views need to know about the configured model, bound from {@code spring.ai.openai.chat.options.*};
 * the response metadata carries no model name, so the activity log takes it from here.
 */
@ConfigurationProperties("spring.ai.openai.chat.options")
public record ModelSettings(String model) {}
