package com.oliversoft.blacksmith.util;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.model.dto.output.AgentOutput;
import com.oliversoft.blacksmith.model.entity.RunArtifact;

@Component
public class BlacksmithUtils {
    
    private final ObjectMapper jsonMapper;

    public BlacksmithUtils(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public AgentOutput getJsonOutputByArtifact(RunArtifact artifact, Class<? extends AgentOutput> outputType) {
        AgentOutput agentOutput = null;

        try {
            agentOutput = jsonMapper.readValue(artifact.getContent(), outputType);
        } catch (JsonProcessingException e) {
            throw new PipelineExecutionException("Failed to read impact analysis artifact json ", e);
        }
        return agentOutput;
    }

    public String toJSON(AgentOutput output) {
        String outputJson = "";
        try {
            outputJson = jsonMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new PipelineExecutionException("Failed to write artifact json ", e);
        }
        if (outputJson == null || outputJson.isBlank())
            throw new PipelineExecutionException("jsonOutput is empty");
        return outputJson;
    }
}
