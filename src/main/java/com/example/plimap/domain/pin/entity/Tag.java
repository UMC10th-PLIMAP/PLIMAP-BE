package com.example.plimap.domain.pin.entity;

import com.example.plimap.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "tag")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 20, nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Short displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "tag")
    private List<PinTag> pinTagList = new ArrayList<>();

    @Builder
    private Tag(String name, Short displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
    }

    public static Tag create(String name, Short displayOrder) {
        return Tag.builder()
                .name(name)
                .displayOrder(displayOrder)
                .build();
    }
}
