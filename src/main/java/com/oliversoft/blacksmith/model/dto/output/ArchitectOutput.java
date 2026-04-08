package com.oliversoft.blacksmith.model.dto.output;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;



public record ArchitectOutput
(
    @Valid ChangeManagementPlan plan,
    @NotEmpty @JsonSetter(nulls = Nulls.AS_EMPTY) List<PlannedTask> plannedTasks
) implements AgentOutput
{

    public record ChangeManagementPlan(
        @NotBlank String changeTitle, @NotBlank String changeDetail, List<String> affectedFiles, List<String> newFiles, List<String> dependencies, List<String> risks
    ){}

    public record PlannedTask(
        @NotBlank String id, @NotBlank String description, @NotBlank String filenamePath, List<String> dependentTasks
    ){}
}
