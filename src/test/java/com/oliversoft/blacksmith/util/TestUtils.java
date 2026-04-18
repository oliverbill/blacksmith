package com.oliversoft.blacksmith.util;

import com.oliversoft.blacksmith.model.dto.output.AgentOutput;
import com.oliversoft.blacksmith.model.dto.output.DeveloperOutput;

public class TestUtils {

    public static boolean isOutputValid(AgentOutput output) {
        DeveloperOutput devOut = (DeveloperOutput) output;
        boolean hasChanged = devOut.changedFiles() != null && !devOut.changedFiles().isEmpty();
        boolean hasNew = devOut.newFiles() != null && !devOut.newFiles().isEmpty();
        return hasChanged || hasNew;
    }

}
