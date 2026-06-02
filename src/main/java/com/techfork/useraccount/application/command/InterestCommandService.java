package com.techfork.useraccount.application.command;

import com.techfork.useraccount.application.command.input.UpdateUserInterestsCommand;
import com.techfork.useraccount.application.command.input.UserInterestCommand;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.domain.vo.UserInterestSelection;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.domain.personalization.service.PersonalizationProfileService;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InterestCommandService {

    private final UserRepository userRepository;
    private final PersonalizationProfileService personalizationProfileService;

    public void updateUserInterests(UpdateUserInterestsCommand command) {
        User user = userRepository.findByIdWithInterestCategories(command.userId())
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        saveUserInterests(user, command.interests());
    }

    void saveUserInterests(User user, List<UserInterestCommand> interests) {
        List<UserInterestSelection> interestSelections = toInterestSelections(interests);
        user.replaceInterests(interestSelections);

        log.info("Saved {} interest categories for user {}", interestSelections.size(), user.getId());

        personalizationProfileService.generatePersonalizationProfile(user.getId());
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
