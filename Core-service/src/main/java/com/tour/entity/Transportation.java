package com.tour.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="transportations")
@Data
public class Transportation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;
}