package com.oliversoft.blacksmith.router;

import com.oliversoft.blacksmith.model.enumeration.AgentName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LLMRouterTest {

    private LLMRouter router;
    private ChatClient minimaxClient;
    private ChatClient openrouterClient;
    private ChatClient openrouterClient2;
    private ChatClient openrouterClient3;

    @BeforeEach
    void setUp() {
        minimaxClient = mock(ChatClient.class);
        openrouterClient = mock(ChatClient.class);
        openrouterClient2 = mock(ChatClient.class);
        openrouterClient3 = mock(ChatClient.class);

        router = new LLMRouter(minimaxClient, openrouterClient, openrouterClient2, openrouterClient3);
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
}
