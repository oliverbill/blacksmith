package com.oliversoft.blacksmith.model.entity;

import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;

@Entity
@Table(name="run_artifacts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"run_id", "type"}, name = "uk_run_artifact_type")
})
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

    @ManyToOne
    @JoinColumn(name="source_artifact_id", nullable = true)// pode ou nao reutilizar um artifact
    private RunArtifact sourceResusedArtifact;

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
