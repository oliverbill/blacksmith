package com.oliversoft.blacksmith.model.dto.input;

import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput.PlannedTask;

public record DeveloperInput(
    @NotNull(message = "currentOngoingTask obrigatorio") PlannedTask currentOngoingTask,
    @NotNull(message = "architectOutput obrigatorio") @NotBlank ArchitectOutput architectOutput,
    @NotNull(message = "constitutionOutput obrigatorio") ConstitutionOutput constitutionOutput
) implements AgentInput {}
