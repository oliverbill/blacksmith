package com.oliversoft.blacksmith.model.dto.output;

import java.util.List;

public record ConstitutionOutput (
    
    DetectedStack stack,
    
    List<String> architecturalPatterns,
    
    List<String> existingModels,

    List<String> existingApiEndpoints,
    
    List<String> codeQualityConsiderations,
    List<String> techDebtsIdentified,
    
    String testCoverage,
    
    CodeConventions codeConventions,
    DependencyAudit dependencyAudit,

    String summary
) implements AgentOutput
{
    public record DetectedStack(
        String language, List<String> frameworks, String testingStack, String buildTool, List<String> dependencies, String database
    ){}

    public record DependencyAudit(
        List<String> outdated, List<String> securityConcerns
    ){}

    public record CodeConventions(
        String logging, String naming, String errorHandling
    ){}
}
