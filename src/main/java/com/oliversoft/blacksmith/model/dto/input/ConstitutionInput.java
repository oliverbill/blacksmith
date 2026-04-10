package com.oliversoft.blacksmith.model.dto.input;

import java.util.List;

public record ConstitutionInput (
    List<String> localRepoPaths,
    String constitutionManual
)implements AgentInput{}
