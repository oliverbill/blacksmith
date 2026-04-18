package com.oliversoft.blacksmith.model.dto.output;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.List;


public record ArchitectOutput
(
    @JsonAlias({"changeManagementPlan", "changePlan"}) ChangeManagementPlan plan,
    @JsonSetter(nulls = Nulls.AS_EMPTY) @JsonAlias({"changeTasks", "tasks", "changeTaskList"}) List<PlannedTask> plannedTasks
) implements AgentOutput
{

    public record ChangeManagementPlan(
        String changeTitle, String changeDetail, List<String> affectedFiles, List<String> newFiles, List<String> dependencies, List<String> risks
    ){}

    public record PlannedTask(
        @JsonAlias({"taskId", "task_id"}) String id,
        String description,
        @JsonAlias({"filePath", "file", "path", "filename"}) String filenamePath,
        @JsonAlias({"dependencies", "dependsOn"}) List<String> dependentTasks
    ){}
}
