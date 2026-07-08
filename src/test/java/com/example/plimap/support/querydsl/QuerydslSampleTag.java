package com.example.plimap.support.querydsl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tag")
public class QuerydslSampleTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Short displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected QuerydslSampleTag() {
    }
}
