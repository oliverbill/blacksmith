package com.oliversoft.blacksmith.model.dto.input;

import java.util.List;

public record ConstitutionInput (
    List<String> gitReposUrls,
    String constitutionManual
){}
