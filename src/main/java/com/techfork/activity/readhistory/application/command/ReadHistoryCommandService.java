package com.techfork.activity.readhistory.application.command;

import com.techfork.activity.readhistory.domain.SearchHistory;
import com.techfork.activity.readhistory.infrastructure.SearchHistoryRepository;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.exception.UserErrorCode;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadHistoryCommandService {

    private final UserRepository userRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    @Transactional
    public void saveSearchHistory(SaveSearchHistoryCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        SearchHistory searchHistory = SearchHistory.create(
                user,
                command.query(),
                command.searchedAt()
        );

        searchHistoryRepository.save(searchHistory);
        log.info("Saved search history for user {} with query: {}", command.userId(), command.query());
    }
}
