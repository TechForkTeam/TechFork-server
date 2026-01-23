package com.techfork.domain.search;

import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.entity.UserInterestCategory;
import com.techfork.domain.user.enums.EInterestCategory;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.repository.UserInterestCategoryRepository;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.domain.user.service.UserProfileService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.techfork.domain.user.enums.EInterestCategory.*;

@Tag("evaluation-setup")
@Disabled("데이터 셋업용 - CI 제외")
@SpringBootTest
class UserProfileServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInterestCategoryRepository userInterestCategoryRepository;

    @Autowired
    private UserProfileService userProfileService;

    @Test
    @DisplayName("10명의 각기 다른 관심사를 가진 테스트 사용자를 생성하고 프로필 벡터를 생성한다.")
    @Transactional
    @Commit
    void generateTestUserProfiles() {
        List<List<EInterestCategory>> userInterests = List.of(
                List.of(IOS, ANDROID, GAME_DEV),
                List.of(BACKEND, DEVOPS, CLOUD),
                List.of(FRONTEND, PRODUCT_UX, ARCHITECTURE),
                List.of(DATA_ENGINEERING, DATA_SCIENCE, DATABASE),
                List.of(AI_ML, DATA_SCIENCE),
                List.of(DEVOPS, CLOUD, SECURITY, NETWORKING),
                List.of(SYSTEMS_OS, EMBEDDED_IOT),
                List.of(BLOCKCHAIN_WEB3, SECURITY),
                List.of(QA_TEST, DEVOPS),
                List.of(AR_VR_XR, GAME_DEV, AI_ML)
        );

        IntStream.range(0, 10).forEach(i -> {
            User user = User.createSocialUser(SocialType.KAKAO, "testSocialId" + i, "test" + i + "@example.com", null);

            userRepository.save(user);

            List<UserInterestCategory> interestCategories = userInterests.get(i).stream()
                    .map(category -> UserInterestCategory.builder()
                            .user(user)
                            .category(category)
                            .build())
                    .collect(Collectors.toList());
            userInterestCategoryRepository.saveAll(interestCategories);

            System.out.println("Generating profile for user: " + user.getId());
            userProfileService.generateUserProfile(user.getId());
            System.out.println("Profile generated for user: " + user.getId());
        });
    }
}
