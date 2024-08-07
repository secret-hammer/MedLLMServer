package com.vipa.medllm.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ImageGroup")
public class ImageGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer imageGroupId;

    @Column(nullable = false, length = 50)
    private String imageGroupName;

    @Column(nullable = true, length = 2000)
    private String description = "N/A";

    @Column(nullable = false, updatable = false)
    @org.hibernate.annotations.CreationTimestamp
    private Timestamp createdTime;

    @Column(nullable = false)
    @org.hibernate.annotations.UpdateTimestamp
    private Timestamp updatedTime;

    @ManyToOne
    @JoinColumn(name = "projectId", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "imageGroup")
    private List<Image> images = new ArrayList<>();

    @Version
    private Integer version;

}