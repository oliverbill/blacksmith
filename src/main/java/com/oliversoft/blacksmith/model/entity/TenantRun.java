package com.oliversoft.blacksmith.model.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.oliversoft.blacksmith.model.enumeration.IssueType;
import com.oliversoft.blacksmith.model.enumeration.RunStatus;



@Entity
@Table(name="tenant_runs")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor @Builder
public class TenantRun {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "full_sync_repo")
    private boolean fullSyncRepo;

    @Column(name="created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name="issue_type", nullable = false)
    private IssueType issueType;

    @Column(name="title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name="status", nullable = false)
    private RunStatus status = RunStatus.STARTED;

    @Column(name="spec", nullable = false)
    private String spec;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        TenantRun other = (TenantRun) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        } else if (!this.id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
