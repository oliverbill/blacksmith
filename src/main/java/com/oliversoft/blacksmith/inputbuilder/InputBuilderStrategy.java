package com.oliversoft.blacksmith.inputbuilder;

import com.oliversoft.blacksmith.exception.InputBuilderException;
import com.oliversoft.blacksmith.model.dto.input.AgentInput;
import com.oliversoft.blacksmith.model.entity.Tenant;

public interface InputBuilderStrategy {
    public AgentInput buildInput(Tenant tenant, String spec) throws InputBuilderException;

} 
