package com.oliversoft.blacksmith.service;

import com.oliversoft.blacksmith.batch.RetryPipelineJobConfig;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.RunStatus;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.explore.JobExplorer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for RetryService.
 * Focuses on the canRetry logic which is pure business rule (no external deps).
 */
class RetryServiceTest {

    private RetryService retryService;

    @BeforeEach
    void setUp() {
        retryService = new RetryService(
            mock(TenantRunRepository.class),
            mock(RunArtifactRepository.class),
            mock(JobExplorer.class),
            mock(RetryPipelineJobConfig.class),
            mock(TaskExecutionRepository.class)
        );
    }

    // ── canRetry ──────────────────────────────────────────────────────────────

    @Test
    void canRetry_withErrorStatus_returnsTrue() {
        TenantRun run = TenantRun.builder().status(RunStatus.ERROR).build();

        assertThat(retryService.canRetry(run)).isTrue();
    }

    @Test
    void canRetry_withStartedStatus_returnsTrue() {
        TenantRun run = TenantRun.builder().status(RunStatus.STARTED).build();

        assertThat(retryService.canRetry(run)).isTrue();
    }

    @Test
    void canRetry_withDoneStatus_returnsTrue() {
        TenantRun run = TenantRun.builder().status(RunStatus.DONE).build();

        assertThat(retryService.canRetry(run)).isTrue();
    }

    @Test
    void canRetry_withCancelledStatus_returnsFalse() {
        TenantRun run = TenantRun.builder().status(RunStatus.CANCELLED).build();

        assertThat(retryService.canRetry(run)).isFalse();
    }

    @Test
    void canRetry_onlyErrorStartedAndDoneAreRetryable() {
        assertThat(retryService.canRetry(TenantRun.builder().status(RunStatus.ERROR).build())).isTrue();
        assertThat(retryService.canRetry(TenantRun.builder().status(RunStatus.STARTED).build())).isTrue();
        assertThat(retryService.canRetry(TenantRun.builder().status(RunStatus.DONE).build())).isTrue();
        assertThat(retryService.canRetry(TenantRun.builder().status(RunStatus.CANCELLED).build())).isFalse();
    }
}
