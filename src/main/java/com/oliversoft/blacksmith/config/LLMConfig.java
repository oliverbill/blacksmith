package com.oliversoft.blacksmith.config;

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
        @Value("${spring.ai.openrouter.base-url}") String baseUrl)
    {
        var openAiApi = OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();
        
        var options = OpenAiChatOptions.builder().model("x-ai/grok-code-fast-1").build();
        
        var chatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .build();

        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Qualifier("openrouter2")
    public ChatClient openRouterClient2(
        @Value("${spring.ai.openrouter.api-key}") String apiKey,
        @Value("${spring.ai.openrouter.base-url}") String baseUrl    ) 
    {
        var openAiApi = OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();
        
        var options = OpenAiChatOptions.builder().model("qwen/qwen3-coder-next").build();
        
        var chatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .build();

        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Qualifier("openrouter3")
    public ChatClient openRouterClient3(
        @Value("${spring.ai.openrouter.api-key}") String apiKey,
        @Value("${spring.ai.openrouter.base-url}") String baseUrl) 
    {
        var openAiApi = OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();
        
        var options = OpenAiChatOptions.builder().model("openai/gpt-5.1-codex-mini").build();
        
        var chatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .build();

        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Qualifier("minimax")
    public ChatClient minimaxClient(MiniMaxChatModel model){
        
        return ChatClient.builder(model).build();
    }
}