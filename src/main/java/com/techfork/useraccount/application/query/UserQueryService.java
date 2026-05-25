package com.techfork.useraccount.application.query;

import com.techfork.useraccount.application.query.result.GetAccountProfileResult;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;

    public GetAccountProfileResult getAccountProfile(GetAccountProfileQuery query) {
        User user = userRepository.findById(query.userId())
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        log.info("Account profile retrieved for userId: {}", query.userId());

        return GetAccountProfileResult.builder()
                .profileImage(user.getProfileImage())
                .nickName(user.getNickName())
                .email(user.getEmail())
                .description(user.getDescription())
                .build();
    }
}
