package com.oliversoft.blacksmith.inputbuilder;

import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import com.oliversoft.blacksmith.adapter.GitAdapter;
import com.oliversoft.blacksmith.exception.InputBuilderException;
import com.oliversoft.blacksmith.model.dto.input.AgentInput;
import com.oliversoft.blacksmith.model.dto.input.ConstitutionInput;
import com.oliversoft.blacksmith.model.entity.Tenant;
import com.oliversoft.blacksmith.model.entity.TenantRun;

@Component
public class ConstitutionInputBuilder implements InputBuilderStrategy{

    private final GitAdapter git;

    public ConstitutionInputBuilder(GitAdapter git){
        this.git = git;
    }


    @Override
    public AgentInput buildInput(Tenant tenant, TenantRun run, String spec) throws InputBuilderException{
        
        List<String> resolvedPaths = getRepoLocalPaths(tenant);
        
        var input = new ConstitutionInput(resolvedPaths, spec);
        return input;
    }

    private List<String> getRepoLocalPaths(Tenant tenant) {
        
        List<String> resolvedPaths = tenant.getGitReposUrls().stream()
            .map(repoUrl -> {
                // Clone repository to local filesystem and return local path
                Path localPath = git.cloneOrPull(repoUrl);
                return localPath.toString();
            })
            .toList();
        return resolvedPaths;
    }
    
}
