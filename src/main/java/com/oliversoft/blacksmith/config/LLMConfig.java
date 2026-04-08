package com.oliversoft.blacksmith.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMConfig {
    
    @Bean
    @Qualifier("openrouter")
    public ChatClient openRouterClient(
        @Value("${spring.ai.openrouter.api-key}") String apiKey,
        @Value("${spring.ai.openrouter.base-url}") String baseUrl,
        @Value("${spring.ai.openrouter.chat.options.model}") String model
    ) 
    {
        var openAiApi = OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();
        
        var options = OpenAiChatOptions.builder().model(model).build();
        
        var chatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .build();

        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Qualifier("anthropic")
    public ChatClient anthropicClient(AnthropicChatModel model){
        
        return ChatClient.builder(model).build();
    }


    @Bean
    @Qualifier("minimax")
    public ChatClient minimaxClient(MiniMaxChatModel model){
        
        return ChatClient.builder(model).build();
    }
}