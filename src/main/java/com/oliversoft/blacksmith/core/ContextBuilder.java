package com.oliversoft.blacksmith.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.exception.AgentConfigException;
import com.oliversoft.blacksmith.model.dto.input.AgentInput;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class ContextBuilder {
    
    private final ObjectMapper mapper;

    public ContextBuilder(ObjectMapper mapper){
        this.mapper = mapper;
    }

    public Optional<String> getSystemPrompt(AgentName agent){

        var filename = agent.name().toLowerCase().replace("_", "-") + "-agent.md";
        var path = "prompts/" + filename;
        ClassPathResource resource = new ClassPathResource(path);
        try {
            return Optional.of(resource.getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AgentConfigException("Failed to load system prompt for agent "+agent, e);
        }
    }

    public String buildUserPrompt(AgentInput input){

        try {
            return this.mapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new AgentConfigException("Failed to write user prompt json ", e);
        }
    }
}
