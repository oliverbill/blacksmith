package com.oliversoft.blacksmith.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.oliversoft.blacksmith.controller.BlacksmithMcpServer;


@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider blacksmithTools(@Lazy BlacksmithMcpServer server) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(server)
            .build();
    }
}