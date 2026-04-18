package com.oliversoft.blacksmith.router;

import com.oliversoft.blacksmith.model.enumeration.AgentName;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LLMRouter {

    public record RoutedChatClient(String name, ChatClient client) {}
    
    private final ChatClient grokCoderFast1;
    private final ChatClient qwen3Coder;
    private final ChatClient codexMini;
    private final ChatClient minimaxClient;

    public LLMRouter(@Qualifier("minimax") ChatClient minimaxClient,
                     @Qualifier("openrouter") ChatClient openrouterClient,
                     @Qualifier("openrouter2") ChatClient qwen3Coder,
                     @Qualifier("openrouter3") ChatClient codexMini){
        this.minimaxClient = minimaxClient;
        this.grokCoderFast1 = openrouterClient;
        this.qwen3Coder = qwen3Coder;
        this.codexMini = codexMini;
    }

    public List<RoutedChatClient> getClientsByPriority(AgentName agent) {
        if (agent == AgentName.DEVELOPER) return List.of(
                new RoutedChatClient("minimax", this.minimaxClient),
                new RoutedChatClient("openrouter", this.grokCoderFast1),
                new RoutedChatClient("openrouter2", this.qwen3Coder),
                new RoutedChatClient("openrouter3", this.codexMini)
        );
        else {
            return List.of(
                    new RoutedChatClient("minimax", this.minimaxClient),
                    new RoutedChatClient("openrouter", this.grokCoderFast1),
                    new RoutedChatClient("openrouter2", this.qwen3Coder)
            );
        }
    }
}
