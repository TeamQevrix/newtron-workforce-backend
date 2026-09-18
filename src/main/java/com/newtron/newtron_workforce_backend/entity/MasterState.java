package com.newtron.newtron_workforce_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "master_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;
}
