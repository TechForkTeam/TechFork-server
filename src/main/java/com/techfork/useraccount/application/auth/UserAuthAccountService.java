package com.techfork.useraccount.application.auth;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.infrastructure.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAuthAccountService {

    private final UserRepository userRepository;

    public Optional<UserAuthProfile> findAuthProfileById(Long userId) {
        return userRepository.findById(userId)
                .map(UserAuthProfile::from);
    }

    @Transactional
    public UserAuthProfile getOrCreateSocialAuthProfile(
            SocialType socialType,
            String socialId,
            String email,
            String profileImage
    ) {
        User user = userRepository.findBySocialTypeAndSocialId(socialType, socialId)
                .map(existingUser -> reactivateIfWithdrawn(existingUser, email, profileImage))
                .orElseGet(() -> createSocialUser(socialType, socialId, email, profileImage));

        return UserAuthProfile.from(user);
    }

    private User createSocialUser(SocialType socialType, String socialId, String email, String profileImage) {
        User newUser = User.createSocialUser(socialType, socialId, email, profileImage);
        return userRepository.save(newUser);
    }

    private User reactivateIfWithdrawn(User user, String email, String profileImage) {
        if (user.isWithdrawn()) {
            user.reactivate(email, profileImage);
        }
        return user;
    }
}
