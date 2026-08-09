package com.newtron.newtron_workforce_backend.entity;

import com.newtron.newtron_workforce_backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "worker_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_profile_id", nullable = false)
    private WorkerProfile workerProfile;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "file_storage_key", nullable = false)
    private String fileStorageKey;

    @Column(name = "file_name")
    private String fileName;
}