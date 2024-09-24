package com.apzda.cloud.uc.domain.entity;

import com.apzda.cloud.gsvc.domain.AuditableEntity;
import com.apzda.cloud.gsvc.model.SoftDeletable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "uc_security_resource")
public class SecurityResource extends AuditableEntity<Long, String, Long> implements SoftDeletable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "created_at")
    private Long createdAt;

    @Column(name = "created_by", length = 32)
    private String createdBy;

    @Column(name = "updated_at")
    private Long updatedAt;

    @Column(name = "updated_by", length = 32)
    private String updatedBy;

    @Column(name = "deleted")
    private boolean deleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pid", referencedColumnName = "id")
    private SecurityResource parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<SecurityResource> children;

    @Size(max = 32)
    @NotNull
    @Column(name = "rid", nullable = false, length = 32)
    private String rid;

    @Size(max = 64)
    @NotNull
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Size(max = 512)
    @Column(name = "actions", nullable = false, length = 512)
    private String actions;

    @Size(max = 256)
    @Column(name = "explorer", length = 256)
    private String explorer;

    @Size(max = 1024)
    @Column(name = "description", length = 1024)
    private String description;

}
