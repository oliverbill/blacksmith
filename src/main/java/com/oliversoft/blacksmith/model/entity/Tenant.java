package com.oliversoft.blacksmith.model.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.oliversoft.blacksmith.model.enumeration.TenantStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenants")
@Getter @Setter
@NoArgsConstructor
public class Tenant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name="name", nullable = false)
    private String name;

    @Column(name="constitution_manual", nullable = true)
    private String constitutionManual;

    @Column(name="constitution_auto", nullable = true)
    private String constitutionAuto;

    @Column(name="status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TenantStatus status = TenantStatus.PENDING;

    @Column(name="user_id", nullable = false, unique = true)
    private UUID userId;


    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Tenant other = (Tenant) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
