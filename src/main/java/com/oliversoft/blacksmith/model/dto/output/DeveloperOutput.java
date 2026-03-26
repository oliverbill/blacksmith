package com.oliversoft.blacksmith.model.dto.output;

import java.util.List;


public record DeveloperOutput(

    List<GeneratedFile> changedFiles,
    List<GeneratedFile> newFiles
) {
    public record GeneratedFile(
        String filePath,
        String content
) {}
}
