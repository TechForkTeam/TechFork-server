package com.techfork.useraccount.application.command;

import com.techfork.useraccount.application.command.input.UpdateUserInterestsCommand;
import com.techfork.useraccount.application.command.input.UserInterestCommand;
import com.techfork.useraccount.application.event.UserInterestsChangedEvent;
import com.techfork.useraccount.application.reader.UserAggregateReader;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;
import com.techfork.useraccount.domain.vo.UserInterestSelection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InterestCommandService {

    private final UserAggregateReader userAggregateReader;
    private final ApplicationEventPublisher eventPublisher;

    public void updateUserInterests(UpdateUserInterestsCommand command) {
        User user = userAggregateReader.getByIdWithInterestCategories(command.userId());
        saveUserInterests(user, command.interests());

        eventPublisher.publishEvent(new UserInterestsChangedEvent(command.userId()));
    }

    void saveUserInterests(User user, List<UserInterestCommand> interests) {
        List<UserInterestSelection> interestSelections = toInterestSelections(interests);
        user.replaceInterests(interestSelections);

        log.info("Saved {} interest categories for user {}", interestSelections.size(), user.getId());
    }

    private List<UserInterestSelection> toInterestSelections(List<UserInterestCommand> interests) {
        return interests.stream()
                .map(this::toInterestSelection)
                .toList();
    }

    private UserInterestSelection toInterestSelection(UserInterestCommand command) {
        EInterestCategory category = EInterestCategory.from(command.category());
        List<EInterestKeyword> keywords = command.keywords() == null
                ? List.of()
                : command.keywords().stream()
                        .map(EInterestKeyword::from)
                        .toList();

        return new UserInterestSelection(category, keywords);
    }
}
