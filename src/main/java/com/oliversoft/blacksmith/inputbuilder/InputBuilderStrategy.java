package com.oliversoft.blacksmith.inputbuilder;

import com.oliversoft.blacksmith.exception.InputBuilderException;
import com.oliversoft.blacksmith.model.dto.input.AgentInput;
import com.oliversoft.blacksmith.model.entity.Tenant;
import com.oliversoft.blacksmith.model.entity.TenantRun;

public interface InputBuilderStrategy {
    AgentInput buildInput(Tenant tenant, TenantRun run, String spec) throws InputBuilderException;
} 
