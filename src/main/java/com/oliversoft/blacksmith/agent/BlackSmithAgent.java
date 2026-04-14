package com.oliversoft.blacksmith.agent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.retry.ExhaustedRetryException;
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
public class BlacksmithAgent {

    private static final Logger log = LoggerFactory.getLogger(BlacksmithAgent.class);
    
    private final BashTools bashTools;
    private final ContextBuilder context;
    private final LLMRouter router;
    protected final ObjectMapper jsonMapper;

    public BlacksmithAgent(BashTools bashTools,
                           ContextBuilder context,
                           LLMRouter router,
                           ObjectMapper jsonMapper){
        this.bashTools = bashTools;
        this.context = context;
        this.router = router;
        this.jsonMapper = jsonMapper;
    }

    public <O> O processInput(AgentInput input, AgentName agent, Class<O> outputType){
        
        log.info("=== processInput START for agent={} ===", agent);
        
        try {
            String systemPrompt = context.getSystemPrompt(agent).orElseThrow(()->new PipelineExecutionException("empty system prompt for agent "+agent));
            String userPrompt = context.buildUserPrompt(input);

            List<RoutedChatClient> candidates = this.router.getClientsByPriority(agent);
            log.info("Found {} LLM candidates for agent {}: {}", candidates.size(), agent, 
                    candidates.stream().map(RoutedChatClient::name).toList());
            if (candidates.isEmpty()) throw new PipelineExecutionException("no LLM clients setup");
            
            List<Throwable> failures = new ArrayList<>();

            for (int i = 0; i < candidates.size(); i++) {
                RoutedChatClient candidate = candidates.get(i);
                log.info(">>> TRYING PROVIDER {} for agent {} (provider {}/{}) <<<", 
                        candidate.name(), agent, i + 1, candidates.size());
                try {
                    var result = invoke(candidate.client(), systemPrompt, userPrompt, outputType, agent);
                    log.info("=== SUCCESS with provider {} for agent {} ===", candidate.name(), agent);
                    return result;
                } catch (Exception e) {
                    failures.add(e);
                    log.warn("*** Provider {} FAILED for agent {}: {} | type={} ***", 
                            candidate.name(), agent, e.getMessage(), e.getClass().getSimpleName());
                }
            }

            log.error("=== ALL {} PROVIDERS FAILED for agent {} ===", candidates.size(), agent);
            for (int i = 0; i < failures.size(); i++) {
                log.error("  Provider {} error: {}", candidates.get(i).name(), failures.get(i).getMessage());
            }
            throw buildFallbackException(agent, failures);
        } catch (RuntimeException e) {
            log.error("=== RuntimeException in processInput, rethrowing: {} ===", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("=== Unexpected Exception in processInput: {} ===", e.getMessage());
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
        log.info(">>> invoke START for agent={} useTools={}", agent, useTools);

        try {
            var promptSpec = client
                    .prompt()
                        .system(systemPrompt)
                        .user(userPrompt);

            var resp = (useTools ? promptSpec.tools(this.bashTools) : promptSpec)
                    .call()
                    .chatResponse();

            var usage = resp.getMetadata().getUsage();
            log.info(">>> LLM tokens — input: {}, output: {}, total: {}",
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens());
            
            if (resp.getResult() == null) {
                throw new PipelineExecutionException("LLM returned a response with no generations — possible content filtering or provider error");
            }
            var content = resp.getResult().getOutput().getText();
            log.info(">>> Raw LLM response content for agent {}: {}", agent, content);
            var cleaned = cleanJson(content);
           
            if (cleaned == null || cleaned.isBlank()) {
                log.warn(">>> LLM returned empty text after tool calls. Sending follow-up to request JSON output.");
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
                var result = this.jsonMapper.readValue(cleaned, outputType);
                log.info(">>> invoke SUCCESS for agent={}", agent);
                return result;
            } catch (com.fasterxml.jackson.core.JsonParseException e) {
                log.warn(">>> LLM returned non-JSON response, retrying with JSON reminder. Content preview: {}",
                    cleaned.substring(0, Math.min(100, cleaned.length())));
                var retryResp = client
                        .prompt()
                            .system(systemPrompt)
                            .user(userPrompt)
                            .user("Your previous response was not valid JSON. Return ONLY the raw JSON object. No explanations, no markdown, no text before or after the JSON.")
                        .call()
                        .chatResponse();
                var retryContent = retryResp.getResult().getOutput().getText();
                var retryResult = this.jsonMapper.readValue(cleanJson(retryContent), outputType);
                log.info(">>> invoke SUCCESS after JSON retry for agent={}", agent);
                return retryResult;
            }
        } catch (Exception e) {
            log.error(">>> invoke EXCEPTION for agent={}: {} | type={}", agent, e.getMessage(), e.getClass().getName());
            throw e;
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
            String className = current.getClass().getName();
            String message = current.getMessage();
            
            // Check for PipelineExecutionException with invalid output - these should retry on different providers
            if (className.contains("PipelineExecutionException")) {
                if (message != null && (message.contains("invalid output") 
                        || message.contains("returned empty response")
                        || message.contains("returned non-JSON")
                        || message.contains("no generations")
                        || message.contains("content filtering"))) {
                    log.debug("Detected invalid output error that may succeed on different provider: {}", message);
                    return true;
                }
            }

            // Check class name for Spring AI TransientAiException and Spring Retry ExhaustedRetryException
            if (className.contains("TransientAiException") || className.contains("AiException")
                    || className.contains("ExhaustedRetryException") || className.contains("RetryException")) {
                if (message != null) {
                    String normalized = message.toLowerCase();
                    if (normalized.contains("429") || normalized.contains("529") 
                            || normalized.contains("503") || normalized.contains("502")
                            || normalized.contains("overload") || normalized.contains("rate limit")
                            || normalized.contains("too many request") || normalized.contains("unavailable")
                            || normalized.contains("http")) {
                        log.debug("Detected retryable error from Spring AI/Retry exception: {} - {}", className, message);
                        return true;
                    }
                }
                // ExhaustedRetryException means retries were exhausted due to a retryable error
                if (className.contains("ExhaustedRetryException")) {
                    log.debug("ExhaustedRetryException detected - original error was retryable");
                    return true;
                }
            }

            if (current instanceof RestClientResponseException responseException) {
                int statusCode = responseException.getStatusCode().value();
                if (statusCode == 429 || statusCode == 529 || statusCode == 503 || statusCode == 502) {
                    log.debug("Detected retryable HTTP status: {}", statusCode);
                    return true;
                }
            }

            Integer reflectedStatus = extractStatusCode(current);
            if (reflectedStatus != null && (reflectedStatus == 429 || reflectedStatus == 529 || reflectedStatus == 503 || reflectedStatus == 502)) {
                log.debug("Detected retryable status via reflection: {}", reflectedStatus);
                return true;
            }

            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("429") || normalized.contains("529") || normalized.contains("503")
                        || normalized.contains("too many requests")
                        || normalized.contains("rate limit")
                        || normalized.contains("rate_limit")
                        || normalized.contains("rst_stream")
                        || normalized.contains("connection reset")
                        || normalized.contains("connection closed")
                        || normalized.contains("no generations")
                        || normalized.contains("no choices")
                        || normalized.contains("overload")) {
                    log.debug("Detected retryable error via message: {}", normalized);
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
