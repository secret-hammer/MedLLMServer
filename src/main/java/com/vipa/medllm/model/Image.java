package com.vipa.medllm.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Image")
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer imageId;

    @Column(nullable = false, unique = true, length = 255)
    private String imageUrl;

    @Column(nullable = false, length = 255)
    private String imageName;

    @Column(nullable = false)
    private Integer status = 0;

    @Column(nullable = false, updatable = false)
    @org.hibernate.annotations.CreationTimestamp
    private Timestamp createdTime;

    @Column(nullable = false)
    @org.hibernate.annotations.UpdateTimestamp
    private Timestamp updatedTime;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "imageGroupId", nullable = false)
    private ImageGroup imageGroup;

    @ManyToOne
    @JoinColumn(name = "imageTypeId", nullable = false)
    private ImageType imageType;

    @Version
    @JsonIgnore
    private Integer version;
}
