package com.oliversoft.blacksmith.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "tenants")
@Getter @Setter
@NoArgsConstructor
@Builder @AllArgsConstructor
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

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "git_repos_urls", columnDefinition = "TEXT[]")
    private List<String> gitReposUrls;


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
