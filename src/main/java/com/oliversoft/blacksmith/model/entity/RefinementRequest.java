package com.oliversoft.blacksmith.model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import com.oliversoft.blacksmith.model.enumeration.RefinementStatus;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refinement_requests")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor @Builder
public class RefinementRequest {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "source_artifact_id")
    private RunArtifact sourceArtifact;

    @Column(name = "feedback")
    private String feedback;

    @Column(name = "start_step")
    private String startStep;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status")
    private RefinementStatus status = RefinementStatus.PENDING_CONFIRMATION;

    @Column(name = "refinement_result")
    private String refinementResult;

    @Column(name="created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RefinementRequest other = (RefinementRequest) obj;
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