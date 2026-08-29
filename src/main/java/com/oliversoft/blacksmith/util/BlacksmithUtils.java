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
                if (hasContentAfterFirstTopLevelObject(inner)) {
                    // Jackson's readValue/readTree silently parse only the first object and drop
                    // everything after it (no exception, no trailing-token check by default) — so
                    // an unguarded unwrap here would make later objects (and any files in them)
                    // vanish with zero trace. Fail loudly instead; this feeds into the same
                    // retry/next-provider fallback already used for malformed JSON.
                    throw new PipelineExecutionException(
                        "LLM returned a JSON array with more than one object; refusing to silently " +
                        "collapse it to a single object: " + content);
                }
                cleaned = inner;
            }
        }
        return cleaned;
    }

    /** True if there is non-whitespace content after the first top-level {...} object closes. */
    private static boolean hasContentAfterFirstTopLevelObject(String s) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') { inString = true; continue; }
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return !s.substring(i + 1).trim().isEmpty();
                }
            }
        }
        return false;
    }

    public static boolean isOutputValid(AgentOutput output) {
        if (!(output instanceof DeveloperOutput)) return true;

        DeveloperOutput devOut = (DeveloperOutput) output;
        boolean hasChanged = devOut.changedFiles() != null && !devOut.changedFiles().isEmpty();
        boolean hasNew = devOut.newFiles() != null && !devOut.newFiles().isEmpty();
        return hasChanged || hasNew;
    }
}

