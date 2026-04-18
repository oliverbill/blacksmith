package com.oliversoft.blacksmith.model.entity;

import com.oliversoft.blacksmith.model.enumeration.IssueType;
import com.oliversoft.blacksmith.model.enumeration.RunStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;


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
