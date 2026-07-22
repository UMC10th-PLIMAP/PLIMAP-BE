package com.example.plimap.domain.pin.service.query;

import com.example.plimap.domain.pin.entity.Tag;

import java.util.List;

public interface TagQueryService {
    public List<Tag> getTagsByNames(List<String> stringTags);
}
