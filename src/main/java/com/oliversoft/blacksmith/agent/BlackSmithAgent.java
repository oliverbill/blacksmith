package com.oliversoft.blacksmith.agent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.core.ContextBuilder;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.model.dto.input.AgentInput;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.router.LLMRouter;
import com.oliversoft.blacksmith.router.LLMRouter.RoutedChatClient;
import com.oliversoft.blacksmith.tool.BashTools;

@Component
public class BlackSmithAgent {

    private static final Logger log = LoggerFactory.getLogger(BlackSmithAgent.class);
    
    private final BashTools bashTools;
    private final ContextBuilder context;
    private final LLMRouter router;
    protected final ObjectMapper jsonMapper;

    public BlackSmithAgent(BashTools bashTools,
                           ContextBuilder context,
                           LLMRouter router,
                           ObjectMapper jsonMapper){
        this.bashTools = bashTools;
        this.context = context;
        this.router = router;
        this.jsonMapper = jsonMapper;
    }

    public <O> O processInput(AgentInput input, AgentName agent, Class<O> outputType){
        
        try {
            String systemPrompt = context.getSystemPrompt(agent).orElseThrow(()->new PipelineExecutionException("empty system prompt for agent "+agent));
            String userPrompt = context.buildUserPrompt(input);

            List<RoutedChatClient> candidates = this.router.getClientsByPriority(agent);
            if (candidates.isEmpty()) throw new PipelineExecutionException("no LLM clients setup");
            
            List<Throwable> failures = new ArrayList<>();

            for (int i = 0; i < candidates.size(); i++) {
                RoutedChatClient candidate = candidates.get(i);
                try {
                    var client = invoke(candidate.client(), systemPrompt, userPrompt, outputType, agent);
                    if (client == null)
                        throw new PipelineExecutionException("error invoke LLM client");
                    return client;
                } catch (RuntimeException e) {
                    failures.add(e);

                    // Only start cascading through fallback providers when the primary failure is a rate limit.
                    if (i == 0 && !isRateLimit(e)) {
                        throw e;
                    }

                    if (i < candidates.size() - 1) {
                        log.warn("LLM provider {} failed for agent {}. Trying fallback provider. reason={}",
                                candidate.name(), agent, e.getMessage());
                    }
                }
            }

            throw buildFallbackException(agent, failures);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineExecutionException("Unexpected error while calling LLM providers", e);
        }
    }

    private <O> O invoke(org.springframework.ai.chat.client.ChatClient client,
                         String systemPrompt,
                         String userPrompt,
                         Class<O> outputType,
                         AgentName agent) throws JsonProcessingException
    {
        boolean useTools = agent == AgentName.CONSTITUTION || agent == AgentName.DEVELOPER;
        log.info("Invoking agent={} useTools={}", agent, useTools);

        var promptSpec = client
                .prompt()
                    .system(systemPrompt)
                    .user(userPrompt);

        var resp = (useTools ? promptSpec.tools(this.bashTools) : promptSpec)
                .call()
                .chatResponse();

        var usage = resp.getMetadata().getUsage();
        log.info("Agent tokens — input: {}, output: {}, total: {}",
            usage.getPromptTokens(),
            usage.getCompletionTokens(),
            usage.getTotalTokens());
        
        if (resp.getResult() == null) {
            throw new PipelineExecutionException("LLM returned a response with no generations — possible content filtering or provider error");
        }
        var content = resp.getResult().getOutput().getText();
        log.info("Raw LLM response content for agent {}: {}", agent, content);
        var cleaned = cleanJson(content);
        if (cleaned == null || cleaned.isBlank()) {
            log.warn("LLM returned empty text after tool calls. Sending follow-up to request JSON output.");
            var followUp = client
                    .prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .user("You have finished gathering information. Now produce your final response as a raw JSON object only. No explanations, no markdown, no text before or after the JSON.")
                    .call()
                    .chatResponse();
            content = followUp.getResult().getOutput().getText();
            cleaned = cleanJson(content);
        }
        if (cleaned == null || cleaned.isBlank()) {
            throw new PipelineExecutionException("LLM returned empty response for agent even after follow-up");
        }
        try {
            return this.jsonMapper.readValue(cleaned, outputType);
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            log.warn("LLM returned non-JSON response, retrying with JSON reminder. Content preview: {}",
                cleaned.substring(0, Math.min(100, cleaned.length())));
            var retryResp = client
                    .prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .user("Your previous response was not valid JSON. Return ONLY the raw JSON object. No explanations, no markdown, no text before or after the JSON.")
                    .call()
                    .chatResponse();
            var retryContent = retryResp.getResult().getOutput().getText();
            return this.jsonMapper.readValue(cleanJson(retryContent), outputType);
        }
    }
    
    private String cleanJson(String content) {
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

    private PipelineExecutionException buildFallbackException(AgentName agent, List<Throwable> failures) {
        Throwable lastFailure = failures.getLast();
        String message = isRateLimit(failures.getFirst())
                ? "Primary LLM provider was rate limited and all fallback providers failed for agent " + agent
                : "Primary LLM failed and fallback providers also failed for agent " + agent;

        PipelineExecutionException exception = new PipelineExecutionException(message, lastFailure);
        for (int i = 0; i < failures.size() - 1; i++) {
            exception.addSuppressed(failures.get(i));
        }
        return exception;
    }

    private boolean isRateLimit(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof RestClientResponseException responseException
                    && responseException.getStatusCode().value() == 429) {
                return true;
            }

            Integer reflectedStatus = extractStatusCode(current);
            if (reflectedStatus != null && reflectedStatus == 429) {
                return true;
            }

            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("429")
                        || normalized.contains("too many requests")
                        || normalized.contains("rate limit")
                        || normalized.contains("rate_limit")
                        || normalized.contains("rst_stream")
                        || normalized.contains("connection reset")
                        || normalized.contains("connection closed")
                        || normalized.contains("no generations")
                        || normalized.contains("no choices")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private Integer extractStatusCode(Throwable error) {
        try {
            Method method = error.getClass().getMethod("getStatusCode");
            Object statusCode = method.invoke(error);

            if (statusCode instanceof HttpStatusCode httpStatusCode) {
                return httpStatusCode.value();
            }
            if (statusCode instanceof Integer integerStatusCode) {
                return integerStatusCode;
            }
        } catch (ReflectiveOperationException ignored) {
            // Some provider exceptions expose only the message.
        }
        return null;
    }
}
