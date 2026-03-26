package com.oliversoft.blacksmith.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMConfig {
    
    @Bean
    @Qualifier("anthropic")
    public ChatClient anthropicClient(AnthropicChatModel model){

        return ChatClient.builder(model).build();
    }

    @Bean
    @Qualifier("openai")
    public ChatClient openApiClient(OpenAiChatModel model){

        return ChatClient.builder(model).build();
    }

    @Bean
    @Qualifier("minimax")
    public ChatClient minimaxApiClient(MiniMaxChatModel model){

        return ChatClient.builder(model).build();
    }
}