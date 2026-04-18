package com.oliversoft.blacksmith.model.dto.output;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record DeveloperOutput(

    @JsonAlias({"changedFiles"})
    List<GeneratedFile> changedFiles,
    @JsonAlias({"newFiles"})
    List<GeneratedFile> newFiles) implements AgentOutput
{
    public record GeneratedFile(

        @JsonAlias({"filePath"})
        String filePath,
        @JsonAlias({"content"})
        String content,
        @JsonAlias({"repoUrl"})
        String repoUrl
) {}

}