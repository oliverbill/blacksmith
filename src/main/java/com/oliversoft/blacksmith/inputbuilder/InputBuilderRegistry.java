package com.oliversoft.blacksmith.inputbuilder;

import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class InputBuilderRegistry {

    private final Map<AgentName, InputBuilderStrategy> builders;

    public InputBuilderRegistry(
        ConstitutionInputBuilder constitutionInputBuilder,
        ArchitectInputBuilder architectInputBuilder,
        DeveloperInputBuilder developerInputBuilder
    ) {
        this.builders = Map.of(
            AgentName.CONSTITUTION, constitutionInputBuilder,
            AgentName.ARCHITECT, architectInputBuilder,
            AgentName.DEVELOPER, developerInputBuilder
        );
    }

    public InputBuilderStrategy get(AgentName agentName) {
        return Optional.ofNullable(builders.get(agentName))
            .orElseThrow(() -> new PipelineExecutionException("No input builder found for agent: " + agentName));
    }
}