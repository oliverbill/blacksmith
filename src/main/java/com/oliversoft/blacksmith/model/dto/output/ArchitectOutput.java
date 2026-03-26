package com.oliversoft.blacksmith.model.dto.output;

import java.util.List;
import java.util.UUID;



public record ArchitectOutput 
(
    ChangeManagementPlan plan,
    List<ChangeTask> tasks  
){

    public record ChangeManagementPlan(
        String changeTitle, String changeDetail, List<String> affectedFiles, List<String> newFiles, List<String> dependencies, List<String> risks
    ){}

    public record ChangeTask(
        UUID id, String description, String filenamePath, List<UUID> dependentTasks
    ){}
}
