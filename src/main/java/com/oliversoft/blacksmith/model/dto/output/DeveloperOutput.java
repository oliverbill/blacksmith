package com.oliversoft.blacksmith.model.dto.output;

import java.util.List;
import jakarta.validation.constraints.NotBlank;

public record DeveloperOutput(

    List<GeneratedFile> changedFiles,
    List<GeneratedFile> newFiles
) implements AgentOutput 
{
    public record GeneratedFile(
        @NotBlank String filePath,
        @NotBlank String content
) {}
}