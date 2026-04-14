package com.oliversoft.blacksmith.model.dto.input;

import java.util.List;

import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput.PlannedTask;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;

public record DeveloperInput(
    PlannedTask currentOngoingTask,
    ArchitectOutput architectOutput,
    ConstitutionOutput constitutionOutput,
    List<String> allowedRepositoryUrls, // tenant's configured repos — repoUrl in output MUST be one of these
    String feedback // optional, used for refinementRequest
)
implements AgentInput {}
