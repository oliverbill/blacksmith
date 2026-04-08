package com.oliversoft.blacksmith.model.dto.input;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record ConstitutionInput (
    @NotEmpty List<String> localRepoPaths,
    String constitutionManual
)implements AgentInput{}
