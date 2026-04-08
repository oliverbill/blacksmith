package com.oliversoft.blacksmith.model.dto.input;

import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;

import jakarta.validation.constraints.NotBlank;

public record ArchitectInput(
    @NotBlank
    ConstitutionOutput constitution, 

    @NotBlank
    String spec
) implements AgentInput{}

