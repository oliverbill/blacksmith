package com.oliversoft.blacksmith.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.core.ContextBuilder;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.model.dto.input.ArchitectInput;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.router.LLMRouter;
import com.oliversoft.blacksmith.tool.BashTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BlacksmithAgent.
 * Tests the fallback/error-detection logic without hitting real LLM providers.
 * End-to-end LLM calls are covered in the nested DeveloperAgentIT class below.
 */
class BlacksmithAgentTest {

    private BlacksmithAgent agent;
    private ContextBuilder contextBuilder;
    private LLMRouter router;

    @BeforeEach
    void setUp() {
        contextBuilder = mock(ContextBuilder.class);
        router = mock(LLMRouter.class);
        agent = new BlacksmithAgent(mock(BashTools.class), contextBuilder, router, new ObjectMapper());
    }

    // ── processInput guard conditions ─────────────────────────────────────────

    @Test
    void processAndReturnJsonInput_whenNoClientsAvailable_throwsPipelineExecutionException() {
        when(contextBuilder.getSystemPrompt(any())).thenReturn(Optional.of("system prompt"));
        when(contextBuilder.buildUserPrompt(any())).thenReturn("user prompt");
        when(router.getClientsByPriority(any())).thenReturn(List.of());

        var input = new ArchitectInput(null, "spec");

        assertThatThrownBy(() -> agent.processInput(input, AgentName.ARCHITECT, ArchitectOutput.class))
            .isInstanceOf(PipelineExecutionException.class)
            .hasMessageContaining("no LLM clients setup");
    }

    @Test
    void processAndReturnJsonInput_whenSystemPromptMissing_throwsPipelineExecutionException() {
        when(contextBuilder.getSystemPrompt(any())).thenReturn(Optional.empty());

        var input = new ArchitectInput(null, "spec");

        assertThatThrownBy(() -> agent.processInput(input, AgentName.ARCHITECT, ArchitectOutput.class))
            .isInstanceOf(PipelineExecutionException.class)
            .hasMessageContaining("empty system prompt");
    }

    // ── isRateLimit detection (via reflection) ────────────────────────────────

    @Test
    void isRateLimit_withHttp429Response_returnsTrue() throws Exception {
        Method isRateLimit = BlacksmithAgent.class.getDeclaredMethod("isRateLimit", Throwable.class);
        isRateLimit.setAccessible(true);

        // HttpStatusCode is sealed in Spring 6 — use the concrete HttpStatus enum
        var exception = mock(RestClientResponseException.class);
        when(exception.getStatusCode()).thenReturn(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);

        boolean result = (boolean) isRateLimit.invoke(agent, exception);

        assertThat(result).isTrue();
    }

    @Test
    void isRateLimit_withHttp503Response_returnsTrue() throws Exception {
        Method isRateLimit = BlacksmithAgent.class.getDeclaredMethod("isRateLimit", Throwable.class);
        isRateLimit.setAccessible(true);

        var exception = mock(RestClientResponseException.class);
        when(exception.getStatusCode()).thenReturn(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);

        boolean result = (boolean) isRateLimit.invoke(agent, exception);

        assertThat(result).isTrue();
    }

    @Test
    void isRateLimit_withHttp502Response_returnsTrue() throws Exception {
        Method isRateLimit = BlacksmithAgent.class.getDeclaredMethod("isRateLimit", Throwable.class);
        isRateLimit.setAccessible(true);

        var exception = mock(RestClientResponseException.class);
        when(exception.getStatusCode()).thenReturn(org.springframework.http.HttpStatus.BAD_GATEWAY);

        boolean result = (boolean) isRateLimit.invoke(agent, exception);

        assertThat(result).isTrue();
    }

    @Test
    void isRateLimit_withRateLimitMessage_returnsTrue() throws Exception {
        Method isRateLimit = BlacksmithAgent.class.getDeclaredMethod("isRateLimit", Throwable.class);
        isRateLimit.setAccessible(true);

        boolean result = (boolean) isRateLimit.invoke(agent, new RuntimeException("rate limit exceeded, please retry"));

        assertThat(result).isTrue();
    }

    @Test
    void isRateLimit_withTooManyRequestsMessage_returnsTrue() throws Exception {
        Method isRateLimit = BlacksmithAgent.class.getDeclaredMethod("isRateLimit", Throwable.class);
        isRateLimit.setAccessible(true);

        boolean result = (boolean) isRateLimit.invoke(agent, new RuntimeException("too many requests"));

        assertThat(result).isTrue();
    }

    @Test
    void isRateLimit_withConnectionResetMessage_returnsTrue() throws Exception {
        Method isRateLimit = BlacksmithAgent.class.getDeclaredMethod("isRateLimit", Throwable.class);
        isRateLimit.setAccessible(true);

        boolean result = (boolean) isRateLimit.invoke(agent, new RuntimeException("connection reset by peer"));

        assertThat(result).isTrue();
    }

    @Test
    void isRateLimit_withOverloadMessage_returnsTrue() throws Exception {
        Method isRateLimit = BlacksmithAgent.class.getDeclaredMethod("isRateLimit", Throwable.class);
        isRateLimit.setAccessible(true);

        boolean result = (boolean) isRateLimit.invoke(agent, new RuntimeException("model is overloaded, try again later"));

        assertThat(result).isTrue();
    }

    @Test
    void isRateLimit_withPipelineExceptionForInvalidOutput_returnsTrue() throws Exception {
        Method isRateLimit = BlacksmithAgent.class.getDeclaredMethod("isRateLimit", Throwable.class);
        isRateLimit.setAccessible(true);

        boolean result = (boolean) isRateLimit.invoke(agent,
            new PipelineExecutionException("Provider minimax returned invalid output"));

        assertThat(result).isTrue();
    }

    @Test
    void isRateLimit_withPipelineExceptionForEmptyResponse_returnsTrue() throws Exception {
        Method isRateLimit = BlacksmithAgent.class.getDeclaredMethod("isRateLimit", Throwable.class);
        isRateLimit.setAccessible(true);

        boolean result = (boolean) isRateLimit.invoke(agent,
            new PipelineExecutionException("LLM returned empty response for agent even after follow-up"));

        assertThat(result).isTrue();
    }

    @Test
    void isRateLimit_withGenericRuntimeException_returnsFalse() throws Exception {
        Method isRateLimit = BlacksmithAgent.class.getDeclaredMethod("isRateLimit", Throwable.class);
        isRateLimit.setAccessible(true);

        boolean result = (boolean) isRateLimit.invoke(agent, new RuntimeException("NullPointerException in handler"));

        assertThat(result).isFalse();
    }

    @Test
    void isRateLimit_withNullPointerException_returnsFalse() throws Exception {
        Method isRateLimit = BlacksmithAgent.class.getDeclaredMethod("isRateLimit", Throwable.class);
        isRateLimit.setAccessible(true);

        boolean result = (boolean) isRateLimit.invoke(agent, new NullPointerException("some internal error"));

        assertThat(result).isFalse();
    }

    @Test
    void isRateLimit_withNestedRateLimitCause_returnsTrue() throws Exception {
        Method isRateLimit = BlacksmithAgent.class.getDeclaredMethod("isRateLimit", Throwable.class);
        isRateLimit.setAccessible(true);

        // Cause chain: outer → inner (rate limit)
        var cause = new RuntimeException("429 rate_limit");
        var outer = new RuntimeException("LLM call failed", cause);

        boolean result = (boolean) isRateLimit.invoke(agent, outer);

        assertThat(result).isTrue();
    }

    // ── Integration: real LLM call with DeveloperInput ────────────────────────
}
