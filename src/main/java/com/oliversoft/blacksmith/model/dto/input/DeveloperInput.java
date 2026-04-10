package com.oliversoft.blacksmith.model.dto.input;

import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput.PlannedTask;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;

import jakarta.validation.constraints.NotBlank;

public record DeveloperInput(
    PlannedTask currentOngoingTask,
    @NotBlank ArchitectOutput architectOutput,
    ConstitutionOutput constitutionOutput
) implements AgentInput {}
