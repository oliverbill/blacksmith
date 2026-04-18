package com.oliversoft.blacksmith.inputbuilder;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.model.dto.input.AgentInput;
import com.oliversoft.blacksmith.model.dto.input.ArchitectInput;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;
import com.oliversoft.blacksmith.model.entity.Tenant;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;

@Component
public class ArchitectInputBuilder implements InputBuilderStrategy{

    private final RunArtifactRepository artifactRepository;
    private final ObjectMapper jsonMapper;

    public ArchitectInputBuilder(RunArtifactRepository artifactRepository, ObjectMapper jsonMapper) {
        this.artifactRepository = artifactRepository;
        this.jsonMapper = jsonMapper;
    }


    @Override
    public AgentInput buildInput(Tenant tenant, TenantRun run, String spec) {

        var lastConstArtifactFromTenant = artifactRepository.findTopByRunTenantIdAndArtifactTypeOrderByCreatedAtDesc(
                                                            tenant.getId(),ArtifactType.CONSTITUTION)
                                                            .orElseThrow(() -> new PipelineExecutionException("Constitution Artifact not found for tenant: " + tenant.getId()));

        ConstitutionOutput constitutionOutput = null;
        try {
            constitutionOutput = this.jsonMapper.readValue(lastConstArtifactFromTenant.getContent(), ConstitutionOutput.class);
        } catch (JsonProcessingException e) {
            throw new PipelineExecutionException("Failed to read constitution artifact json ", e);
        }

        var input = new ArchitectInput(constitutionOutput, spec);
        return input;
    }
    
}
