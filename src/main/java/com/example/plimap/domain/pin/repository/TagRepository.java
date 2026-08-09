package com.example.plimap.domain.pin.repository;

import com.example.plimap.domain.pin.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    List<Tag> findAllByNameIn(List<String> nameList);
}
