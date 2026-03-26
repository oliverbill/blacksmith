package com.oliversoft.blacksmith.model.dto.input;

import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;

public record ArchitectInput(
    ConstitutionOutput constitution, 
    String spec
){}

