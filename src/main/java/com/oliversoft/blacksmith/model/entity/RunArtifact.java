package com.oliversoft.blacksmith.model.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;

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
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="run_artifacts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor @Builder
public class RunArtifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    // 1 Run produces N Artifacts(AgentOutput records)
    @ManyToOne
    @JoinColumn(name="run_id", nullable = false)
    private TenantRun run;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "agent", nullable = false)
    private AgentName agentName;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "type", nullable = false)
    private ArtifactType artifactType;

    @NotNull(message = "artifact content is empty")
    @Column(name = "content", nullable = false)
    private String content;

    

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RunArtifact other = (RunArtifact) obj;
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
