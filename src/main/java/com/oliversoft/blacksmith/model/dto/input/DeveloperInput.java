package com.oliversoft.blacksmith.model.dto.input;

import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput.ChangeTask;

public record DeveloperInput(
    ChangeTask task,
    ArchitectOutput architectOutput,
    ConstitutionOutput constitutionOutput,
    String spec
) {

}
