package com.techfork.activity.readpost.converter;

import com.techfork.activity.readpost.dto.ReadPostDto;
import com.techfork.activity.readpost.dto.ReadPostListResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReadPostConverter {

    public ReadPostListResponse toReadPostListResponse(List<ReadPostDto> readPosts, int requestedSize) {
        boolean hasNext = readPosts.size() > requestedSize;
        List<ReadPostDto> content = hasNext ? readPosts.subList(0, requestedSize) : readPosts;

        Long lastReadPostId = content.isEmpty() ? null : content.get(content.size() - 1).readPostId();

        return ReadPostListResponse.builder()
                .readPosts(content)
                .lastReadPostId(lastReadPostId)
                .hasNext(hasNext)
                .build();
    }
}
