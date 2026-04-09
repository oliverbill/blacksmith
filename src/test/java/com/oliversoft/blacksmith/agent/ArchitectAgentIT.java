package com.oliversoft.blacksmith.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.model.dto.input.ArchitectInput;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;
import com.oliversoft.blacksmith.model.enumeration.AgentName;

@SpringBootTest
@ActiveProfiles("test")
class ArchitectAgentIT {

    @Autowired
    private BlackSmithAgent agent;

    @Autowired
    private ObjectMapper objectMapper;

    private ConstitutionOutput constitution;

    @BeforeEach
    void loadConstitution() throws Exception {
        var resource = new ClassPathResource("constitution-sample.json");
        constitution = objectMapper.readValue(resource.getInputStream(), ConstitutionOutput.class);
    }

    @Test
    void architectAgent_shouldProducePlannedTasks_forNewFeatureSpec() {
        var spec = "crie uma nova run para uma nova feature: um novo chatclient para o provider Deepseek.";
        var input = new ArchitectInput(constitution, spec);

        ArchitectOutput output = agent.processInput(input, AgentName.ARCHITECT, ArchitectOutput.class);

        assertThat(output).isNotNull();
        assertThat(output.plan()).isNotNull();
        assertThat(output.plan().changeTitle()).isNotBlank();
        assertThat(output.plannedTasks())
                .isNotNull()
                .isNotEmpty()
                .allSatisfy(task -> {
                    assertThat(task.id()).isNotBlank();
                    assertThat(task.description()).isNotBlank();
                    assertThat(task.filenamePath()).isNotBlank();
                });

        System.out.println("=== Change Plan ===");
        System.out.println(output.plan().changeTitle());
        System.out.println(output.plan().changeDetail());
        System.out.println("\n=== Planned Tasks ===");
        output.plannedTasks().forEach(t ->
            System.out.printf("[%s] %s → %s%n", t.id(), t.description(), t.filenamePath()));
    }
}
