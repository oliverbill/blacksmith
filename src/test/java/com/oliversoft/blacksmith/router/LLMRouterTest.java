package com.oliversoft.blacksmith.router;

import com.oliversoft.blacksmith.model.enumeration.AgentName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LLMRouterTest {

    private LLMRouter router;
    private ChatClient minimaxClient;
    private ChatClient openrouterClient;
    private ChatClient openrouterClient2;
    private ChatClient openrouterClient3;
    private ObjectProvider<ChatClient> claudeSdkClientProvider;

    @BeforeEach
    void setUp() {
        minimaxClient = mock(ChatClient.class);
        openrouterClient = mock(ChatClient.class);
        openrouterClient2 = mock(ChatClient.class);
        openrouterClient3 = mock(ChatClient.class);
        claudeSdkClientProvider = mock(ObjectProvider.class);
        when(claudeSdkClientProvider.getIfAvailable()).thenReturn(null); // dev profile off by default

        router = new LLMRouter(minimaxClient, openrouterClient, openrouterClient2, openrouterClient3,
            claudeSdkClientProvider);
    }

    // ── CONSTITUTION ──────────────────────────────────────────────────────────

    @Test
    void getClientsByPriority_forConstitution_returnsMiniMaxFirst() {
        List<LLMRouter.RoutedChatClient> clients = router.getClientsByPriority(AgentName.CONSTITUTION);

        assertThat(clients).isNotEmpty();
        assertThat(clients.get(0).name()).isEqualTo("minimax");
        assertThat(clients.get(0).client()).isSameAs(minimaxClient);
    }

    @Test
    void getClientsByPriority_forConstitution_returnsThreeClients() {
        List<LLMRouter.RoutedChatClient> clients = router.getClientsByPriority(AgentName.CONSTITUTION);

        assertThat(clients).hasSize(3);
    }

    @Test
    void getClientsByPriority_forConstitution_hasOpenRouterAsFallback() {
        List<LLMRouter.RoutedChatClient> clients = router.getClientsByPriority(AgentName.CONSTITUTION);

        assertThat(clients.stream().map(LLMRouter.RoutedChatClient::name).toList())
            .containsExactly("minimax", "openrouter", "openrouter2");
    }

    // ── ARCHITECT ─────────────────────────────────────────────────────────────

    @Test
    void getClientsByPriority_forArchitect_returnsMiniMaxFirst() {
        List<LLMRouter.RoutedChatClient> clients = router.getClientsByPriority(AgentName.ARCHITECT);

        assertThat(clients.get(0).name()).isEqualTo("minimax");
        assertThat(clients.get(0).client()).isSameAs(minimaxClient);
    }

    @Test
    void getClientsByPriority_forArchitect_returnsThreeClients() {
        List<LLMRouter.RoutedChatClient> clients = router.getClientsByPriority(AgentName.ARCHITECT);

        assertThat(clients).hasSize(3);
    }

    @Test
    void getClientsByPriority_forArchitect_hasOpenRouterFallbacks() {
        List<LLMRouter.RoutedChatClient> clients = router.getClientsByPriority(AgentName.ARCHITECT);

        assertThat(clients.stream().map(LLMRouter.RoutedChatClient::name).toList())
            .containsExactly("minimax", "openrouter", "openrouter2");
    }

    // ── DEVELOPER ─────────────────────────────────────────────────────────────

    @Test
    void getClientsByPriority_forDeveloper_returnsMiniMaxFirst() {
        List<LLMRouter.RoutedChatClient> clients = router.getClientsByPriority(AgentName.DEVELOPER);

        assertThat(clients.get(0).name()).isEqualTo("minimax");
        assertThat(clients.get(0).client()).isSameAs(minimaxClient);
    }

    @Test
    void getClientsByPriority_forDeveloper_returnsFourClients() {
        List<LLMRouter.RoutedChatClient> clients = router.getClientsByPriority(AgentName.DEVELOPER);

        assertThat(clients).hasSize(4);
    }

    // ── General invariants ────────────────────────────────────────────────────

    @Test
    void getClientsByPriority_allAgents_primaryClientIsMinimax() {
        for (AgentName agent : AgentName.values()) {
            var clients = router.getClientsByPriority(agent);

            assertThat(clients).isNotEmpty();
            assertThat(clients.get(0).name())
                .as("Primary client for %s should be minimax", agent)
                .isEqualTo("minimax");
        }
    }

    @Test
    void getClientsByPriority_allAgents_atLeastTwoFallbacks() {
        for (AgentName agent : AgentName.values()) {
            var clients = router.getClientsByPriority(agent);

            assertThat(clients.size())
                .as("Agent %s should have at least 2 clients (primary + fallback)", agent)
                .isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void routedChatClient_record_storesNameAndClient() {
        var record = new LLMRouter.RoutedChatClient("test-provider", minimaxClient);

        assertThat(record.name()).isEqualTo("test-provider");
        assertThat(record.client()).isSameAs(minimaxClient);
    }

    // ── dev-profile Claude SDK override (Developer agent only) ─────────────────

    @Test
    void getClientsByPriority_forDeveloper_whenClaudeSdkClientAbsent_usesApiFallbackChainUnchanged() {
        // claudeSdkClientProvider.getIfAvailable() returns null by default (see setUp) — prod/non-dev behavior.
        List<LLMRouter.RoutedChatClient> clients = router.getClientsByPriority(AgentName.DEVELOPER);

        assertThat(clients.stream().map(LLMRouter.RoutedChatClient::name).toList())
            .containsExactly("minimax", "openrouter", "openrouter2", "openrouter3");
    }

    @Test
    void getClientsByPriority_forDeveloper_whenClaudeSdkClientPresent_returnsOnlyItWithNoFallback() {
        ChatClient claudeSdkClient = mock(ChatClient.class);
        when(claudeSdkClientProvider.getIfAvailable()).thenReturn(claudeSdkClient);

        List<LLMRouter.RoutedChatClient> clients = router.getClientsByPriority(AgentName.DEVELOPER);

        assertThat(clients).hasSize(1);
        assertThat(clients.get(0).name()).isEqualTo("claude-sdk");
        assertThat(clients.get(0).client()).isSameAs(claudeSdkClient);
    }

    @Test
    void getClientsByPriority_forConstitutionAndArchitect_ignoreClaudeSdkClientEvenWhenPresent() {
        ChatClient claudeSdkClient = mock(ChatClient.class);
        when(claudeSdkClientProvider.getIfAvailable()).thenReturn(claudeSdkClient);

        assertThat(router.getClientsByPriority(AgentName.CONSTITUTION).stream().map(LLMRouter.RoutedChatClient::name))
            .containsExactly("minimax", "openrouter", "openrouter2");
        assertThat(router.getClientsByPriority(AgentName.ARCHITECT).stream().map(LLMRouter.RoutedChatClient::name))
            .containsExactly("minimax", "openrouter", "openrouter2");
    }
}
