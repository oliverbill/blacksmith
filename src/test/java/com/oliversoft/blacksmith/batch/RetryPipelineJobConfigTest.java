package com.oliversoft.blacksmith.batch;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the static determineStartStep helper in RetryPipelineJobConfig.
 * This method determines which step to resume from given the set of already-completed steps.
 */
class RetryPipelineJobConfigTest {

    @Test
    void determineStartStep_withNoCompletedSteps_returnsConstitution() {
        String result = RetryPipelineJobConfig.determineStartStep(List.of());

        assertThat(result).isEqualTo("CONSTITUTION");
    }

    @Test
    void determineStartStep_withConstitutionCompleted_returnsArchitect() {
        String result = RetryPipelineJobConfig.determineStartStep(List.of("CONSTITUTION"));

        assertThat(result).isEqualTo("ARCHITECT");
    }

    @Test
    void determineStartStep_withConstitutionAndArchitectCompleted_returnsDeveloper() {
        String result = RetryPipelineJobConfig.determineStartStep(List.of("CONSTITUTION", "ARCHITECT"));

        assertThat(result).isEqualTo("DEVELOPER");
    }

    @Test
    void determineStartStep_withAllStepsCompleted_returnsNull() {
        String result = RetryPipelineJobConfig.determineStartStep(List.of("CONSTITUTION", "ARCHITECT", "DEVELOPER"));

        assertThat(result).isNull();
    }

    @Test
    void determineStartStep_withOnlyArchitectCompleted_returnsDeveloper() {
        // ARCHITECT completed without CONSTITUTION — returns the step after the highest completed
        String result = RetryPipelineJobConfig.determineStartStep(List.of("ARCHITECT"));

        assertThat(result).isEqualTo("DEVELOPER");
    }

    @Test
    void determineStartStep_withOnlyDeveloperCompleted_returnsNull() {
        // DEVELOPER is the last step — no next step available
        String result = RetryPipelineJobConfig.determineStartStep(List.of("DEVELOPER"));

        assertThat(result).isNull();
    }

    @Test
    void determineStartStep_stepOrderIsConstitutionArchitectDeveloper() {
        // Verify the full progression produces the right sequence
        assertThat(RetryPipelineJobConfig.determineStartStep(List.of())).isEqualTo("CONSTITUTION");
        assertThat(RetryPipelineJobConfig.determineStartStep(List.of("CONSTITUTION"))).isEqualTo("ARCHITECT");
        assertThat(RetryPipelineJobConfig.determineStartStep(List.of("CONSTITUTION", "ARCHITECT"))).isEqualTo("DEVELOPER");
        assertThat(RetryPipelineJobConfig.determineStartStep(List.of("CONSTITUTION", "ARCHITECT", "DEVELOPER"))).isNull();
    }
}
