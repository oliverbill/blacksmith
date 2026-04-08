package com.oliversoft.blacksmith.model.dto.output;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record ConstitutionOutput (
    
    DetectedStack stack,
    
    @NotEmpty
    List<String> architecturalPatterns,
    
    List<String> existingModels,

    List<String> existingApiEndpoints,
    
    @NotEmpty
    List<String> codeQualityConsiderations,
    List<String> techDebtsIdentified,
    
    @NotBlank 
    String testCoverage,
    
    CodeConventions codeConventions,
    DependencyAudit dependencyAudit,
    @NotBlank
    String summary
) implements AgentOutput
{
    public record DetectedStack(
        @NotBlank String language, @NotEmpty List<String> frameworks, @NotBlank String testingStack, @NotBlank String buildTool, @NotEmpty List<String> dependencies, @NotBlank String database
    ){}

    public record DependencyAudit(
        List<String> outdated, List<String> securityConcerns
    ){}

    public record CodeConventions(
        @NotBlank String logging, @NotBlank String naming, @NotBlank String errorHandling
    ){}
}
