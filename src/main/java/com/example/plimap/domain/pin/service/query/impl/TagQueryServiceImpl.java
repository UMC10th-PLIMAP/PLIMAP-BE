package com.example.plimap.domain.pin.service.query.impl;

import com.example.plimap.domain.pin.entity.Tag;
import com.example.plimap.domain.pin.exception.TagErrorCode;
import com.example.plimap.domain.pin.exception.TagException;
import com.example.plimap.domain.pin.repository.TagRepository;
import com.example.plimap.domain.pin.service.query.TagQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagQueryServiceImpl implements TagQueryService {

    private final TagRepository tagRepository;

    public List<Tag> getTagsByNames(List<String> stringTags) {
        List<Tag> tags = tagRepository.findAllByNameIn(stringTags);
        if (tags.size() != stringTags.size()) {
            throw new TagException(TagErrorCode.TAG_NOT_FOUND);
        }
        return tags;
    }
}
