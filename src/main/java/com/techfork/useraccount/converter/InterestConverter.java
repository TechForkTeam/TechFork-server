package com.techfork.useraccount.converter;

import com.techfork.useraccount.application.command.UpdateUserInterestsCommand;
import com.techfork.useraccount.application.command.UserInterestCommand;
import com.techfork.useraccount.application.query.result.GetInterestListResult;
import com.techfork.useraccount.application.query.result.GetUserInterestsResult;
import com.techfork.useraccount.dto.InterestListResponse;
import com.techfork.useraccount.dto.SaveInterestRequest;
import com.techfork.useraccount.dto.UserInterestDto;
import com.techfork.useraccount.dto.UserInterestResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InterestConverter {

    public List<UserInterestCommand> toUserInterestCommands(List<UserInterestDto> interests) {
        return interests.stream()
                .map(interest -> UserInterestCommand.builder()
                        .category(interest.category())
                        .keywords(interest.keywords())
                        .build())
                .toList();
    }

    public UpdateUserInterestsCommand toUpdateUserInterestsCommand(Long userId, SaveInterestRequest request) {
        return new UpdateUserInterestsCommand(userId, toUserInterestCommands(request.interests()));
    }

    public InterestListResponse toInterestListResponse(GetInterestListResult result) {
        return InterestListResponse.builder()
                .categories(result.categories().stream()
                        .map(category -> InterestListResponse.Category.builder()
                                .category(category.category())
                                .displayName(category.displayName())
                                .keywords(category.keywords().stream()
                                        .map(keyword -> new InterestListResponse.Keyword(keyword.keyword(), keyword.displayName()))
                                        .toList())
                                .build())
                        .toList())
                .build();
    }

    public UserInterestResponse toUserInterestResponse(GetUserInterestsResult result) {
        return UserInterestResponse.builder()
                .interests(result.interests().stream()
                        .map(interest -> UserInterestDto.builder()
                                .category(interest.category())
                                .keywords(interest.keywords())
                                .build())
                        .toList())
                .build();
    }
}
