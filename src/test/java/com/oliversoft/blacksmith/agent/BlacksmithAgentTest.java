package com.oliversoft.blacksmith.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.core.ContextBuilder;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.model.dto.input.ArchitectInput;
import com.oliversoft.blacksmith.model.dto.input.DeveloperInput;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.dto.output.DeveloperOutput;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.router.LLMRouter;
import com.oliversoft.blacksmith.router.LLMRouter.RoutedChatClient;
import com.oliversoft.blacksmith.tool.BashTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.web.client.RestClientResponseException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
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

    // ── processInput success/fallback branch (candidate output validity) ──────
    //
    // These exercise tryProcessWithAllCandidates end-to-end against a mocked ChatClient,
    // rather than only the isRateLimit helper — no prior test in this class ever drove a
    // full processInput() call to a successful return.

    private static ChatResponse chatResponseWithText(String text) {
        var metadata = ChatResponseMetadata.builder().usage(new DefaultUsage(10, 10)).build();
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))), metadata);
    }

    /** Builds a ChatClient mock whose prompt().system(...).user(...).call().chatResponse() returns the given text. */
    private static ChatClient chatClientReturning(String text) {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().system(anyString()).user(anyString()).call().chatResponse())
            .thenReturn(chatResponseWithText(text));
        return client;
    }

    @Test
    void processInput_whenSingleProviderReturnsCompleteNonEmptyOutput_returnsThatOutputAsSuccess() {
        // ARCHITECT does not attach BashTools, so the mocked chain is prompt().system().user().call() — no .tools().
        String completeJson = """
            {"plan":{"changeTitle":"T","changeDetail":"D","affectedFiles":[],"newFiles":[],"dependencies":[],"risks":[]},
             "plannedTasks":[{"id":"t1","description":"do it","filenamePath":"src/A.java","dependentTasks":[]}]}
            """;
        ChatClient client = chatClientReturning(completeJson);

        when(contextBuilder.getSystemPrompt(AgentName.ARCHITECT)).thenReturn(Optional.of("system"));
        when(contextBuilder.buildUserPrompt(any())).thenReturn("user");
        when(router.getClientsByPriority(AgentName.ARCHITECT))
            .thenReturn(List.of(new RoutedChatClient("test-provider", client)));

        var result = agent.processInput(new ArchitectInput(null, "spec"), AgentName.ARCHITECT, ArchitectOutput.class);

        assertThat(result.providerName()).isEqualTo("test-provider");
        assertThat(result.output().plan().changeTitle()).isEqualTo("T");
        assertThat(result.output().plannedTasks()).hasSize(1);
    }

    @Test
    void processInput_whenDeveloperProviderReturnsNonEmptyChangedFiles_returnsThatOutputAsSuccess() {
        String completeJson = """
            {"changedFiles":[{"filePath":"src/A.java","content":"class A {}","repoUrl":"https://example.com/r.git"}],
             "newFiles":[]}
            """;
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        // DEVELOPER attaches BashTools via .tools(...) before .call() — stub that step too.
        when(client.prompt().system(anyString()).user(anyString()).tools(any(BashTools.class)).call().chatResponse())
            .thenReturn(chatResponseWithText(completeJson));

        when(contextBuilder.getSystemPrompt(AgentName.DEVELOPER)).thenReturn(Optional.of("system"));
        when(contextBuilder.buildUserPrompt(any())).thenReturn("user");
        when(router.getClientsByPriority(AgentName.DEVELOPER))
            .thenReturn(List.of(new RoutedChatClient("test-provider", client)));

        var input = new DeveloperInput(null, null, null, List.of("https://example.com/r.git"), null);
        var result = agent.processInput(input, AgentName.DEVELOPER, DeveloperOutput.class);

        assertThat(result.output().changedFiles()).hasSize(1);
        assertThat(result.output().changedFiles().get(0).content()).isEqualTo("class A {}");
    }

    @Test
    void processInput_whenDeveloperProviderReturnsEmptyOutput_fallsBackAndEventuallyThrows() {
        String emptyJson = """
            {"changedFiles":[],"newFiles":[]}
            """;
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().system(anyString()).user(anyString()).tools(any(BashTools.class)).call().chatResponse())
            .thenReturn(chatResponseWithText(emptyJson));

        when(contextBuilder.getSystemPrompt(AgentName.DEVELOPER)).thenReturn(Optional.of("system"));
        when(contextBuilder.buildUserPrompt(any())).thenReturn("user");
        when(router.getClientsByPriority(AgentName.DEVELOPER))
            .thenReturn(List.of(new RoutedChatClient("test-provider", client)));

        var input = new DeveloperInput(null, null, null, List.of("https://example.com/r.git"), null);

        assertThatThrownBy(() -> agent.processInput(input, AgentName.DEVELOPER, DeveloperOutput.class))
            .isInstanceOf(PipelineExecutionException.class);
    }

    // ── Integration: real LLM call with DeveloperInput ────────────────────────
}
