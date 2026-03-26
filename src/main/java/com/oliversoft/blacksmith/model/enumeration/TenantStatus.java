package com.oliversoft.blacksmith.model.enumeration;

public enum TenantStatus {
    PENDING, // tenant criado, ConstitutionAgent ainda não correu (constitution_auto = null)
    ACTIVE, // constitutionAuto preenchido, pronto para runs
    INACTIVE // desactivado manualmente
}
