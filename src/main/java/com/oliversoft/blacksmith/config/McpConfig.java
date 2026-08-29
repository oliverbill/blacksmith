package com.oliversoft.blacksmith.config;

import com.oliversoft.blacksmith.controller.BlacksmithMcpServer;
import com.oliversoft.blacksmith.tool.BashTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;


@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider blacksmithTools(@Lazy BlacksmithMcpServer server) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(server)
            .build();
    }

    /**
     * Exposes BashTools' read-only filesystem tools over MCP so the Claude CLI subprocess
     * (com.oliversoft:claude-cli-chat-model's ClaudeCliChatModel) can use them during its own
     * agentic loop. Gated by the same property that activates that provider — not exposed
     * unless claude-cli.enabled=true (never in prod).
     */
    @Bean
    @ConditionalOnProperty(prefix = "claude-cli", name = "enabled", havingValue = "true")
    public ToolCallbackProvider blacksmithDevFsTools(@Lazy BashTools bashTools) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(bashTools)
            .build();
    }
}