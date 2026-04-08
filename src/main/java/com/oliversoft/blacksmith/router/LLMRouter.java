package com.oliversoft.blacksmith.router;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.oliversoft.blacksmith.model.enumeration.AgentName;

@Component
public class LLMRouter {

    public record RoutedChatClient(String name, ChatClient client) {}
    
    private final ChatClient anthropicClient;
    private final ChatClient openrouterClient;
    private final ChatClient minimaxClient;

    public LLMRouter(@Qualifier("minimax") ChatClient minimaxClient,
                     @Qualifier("anthropic") ChatClient anthropicClient,
                     @Qualifier("openrouter") ChatClient openrouterClient){

        this.minimaxClient = minimaxClient;
        this.anthropicClient = anthropicClient;
        this.openrouterClient = openrouterClient;
    }

    public List<RoutedChatClient> getClientsByPriority(AgentName agent){
        return switch (agent){
            case CONSTITUTION -> List.of(
                new RoutedChatClient("minimax", this.minimaxClient),
                new RoutedChatClient("openrouter", this.openrouterClient),
                new RoutedChatClient("anthropic", this.anthropicClient)
            );
            case ARCHITECT -> List.of(
                new RoutedChatClient("minimax", this.minimaxClient),
                new RoutedChatClient("openrouter", this.openrouterClient),
                new RoutedChatClient("anthropic", this.anthropicClient)
            );
            case DEVELOPER -> List.of(
                new RoutedChatClient("minimax", this.minimaxClient),
                new RoutedChatClient("openrouter", this.openrouterClient),
                new RoutedChatClient("anthropic", this.anthropicClient)
            );
        };
    }
}
