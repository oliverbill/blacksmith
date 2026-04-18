package com.oliversoft.blacksmith.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.model.dto.output.AgentOutput;
import com.oliversoft.blacksmith.model.dto.output.DeveloperOutput;
import com.oliversoft.blacksmith.model.entity.RunArtifact;

public class BlacksmithUtils {

    private BlacksmithUtils() {
    }

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public static AgentOutput getJsonOutputByArtifact(RunArtifact artifact, Class<? extends AgentOutput> outputType) {
        AgentOutput agentOutput = null;

        try {
            agentOutput = jsonMapper.readValue(artifact.getContent(), outputType);
        } catch (JsonProcessingException e) {
            throw new PipelineExecutionException("Failed to read impact analysis artifact json ", e);
        }
        return agentOutput;
    }

    public static String toJSON(AgentOutput output) {
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

    public static String cleanJson(String content) {
        if (content == null) return "";
        String cleaned = content
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();
        // unwrap single-element array: [{...}] -> {...}
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            String inner = cleaned.substring(1, cleaned.length() - 1).trim();
            if (inner.startsWith("{")) {
                cleaned = inner;
            }
        }
        return cleaned;
    }

    public static boolean isOutputValid(AgentOutput output) {
        if (!(output instanceof DeveloperOutput)) return true;

        DeveloperOutput devOut = (DeveloperOutput) output;
        boolean hasChanged = devOut.changedFiles() != null && !devOut.changedFiles().isEmpty();
        boolean hasNew = devOut.newFiles() != null && !devOut.newFiles().isEmpty();
        return hasChanged || hasNew;
    }
}

