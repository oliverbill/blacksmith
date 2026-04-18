package com.oliversoft.blacksmith.model.dto.output;

import com.oliversoft.blacksmith.model.entity.RunArtifact;

public record ReusedArtifact(
    AgentOutput agentOutput,
    RunArtifact sourceArtifact,
    String providerName
)
{}
