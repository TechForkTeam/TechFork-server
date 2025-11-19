package com.techfork.domain.search;

import com.techfork.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    @Override
    public List<SearchResult> searchGeneral(String query) {
        return List.of();
    }

    @Override
    public List<SearchResult> searchPersonalized(String query, User user) {
        return List.of();
    }
}