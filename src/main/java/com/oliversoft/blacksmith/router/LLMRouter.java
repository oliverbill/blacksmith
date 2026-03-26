package com.oliversoft.blacksmith.router;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.oliversoft.blacksmith.model.enumeration.AgentName;

@Component
public class LLMRouter {
    
    private final ChatClient anthropicClient;
    private final ChatClient openaiClient;
    private final ChatClient minimaxClient;

    public LLMRouter(@Qualifier("anthropic") ChatClient anthropicClient,
                     @Qualifier("openai") ChatClient openaiClient,
                     @Qualifier("minimax") ChatClient minimaxClient){

        this.anthropicClient = anthropicClient;
        this.openaiClient = openaiClient;
        this.minimaxClient = minimaxClient;
    }


    public ChatClient getClient(AgentName agent){
        return switch (agent){
            case CONSTITUTION -> this.openaiClient;
            case ARCHITECT -> this.anthropicClient;
            case DEVELOPER -> this.openaiClient;
        };
    }
}
