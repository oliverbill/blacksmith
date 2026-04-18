package com.oliversoft.blacksmith.model.entity;

import com.oliversoft.blacksmith.model.enumeration.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="task_executions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor @Builder
public class TaskExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    // o ArchitectTasklet cria várias TaskExecution para um RunArtifact(ArchitectOutput)
    @ManyToOne
    @JoinColumn(name = "artifact_id", nullable = false)
    private RunArtifact artifact;

    @Column(name = "planned_task_uuid", nullable = false)
    private UUID plannedTaskId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private TaskStatus status = TaskStatus.DEV_PENDING;

    @Column(name = "llm_provider")
    private String llmProvider;

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        TaskExecution other = (TaskExecution) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
