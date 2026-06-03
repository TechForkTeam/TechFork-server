package com.techfork.personalization.application;

import com.techfork.personalization.application.event.PersonalizedProfileGeneratedEvent;
import com.techfork.personalization.application.generation.PersonalizedProfileGenerator;
import com.techfork.personalization.infrastructure.PersonalizationProfileDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalizationProfileService {

    private final PersonalizedProfileGenerator personalizedProfileGenerator;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @Transactional
    public void generatePersonalizationProfile(Long userId) {
        generatePersonalizationProfileSync(userId);
    }

    /**
     * 개인화 프로필 생성 (동기 버전)
     * 테스트 환경이나 동기 실행이 필요한 경우 사용
     */
    @Transactional
    public void generatePersonalizationProfileSync(Long userId) {
        try {
            PersonalizationProfileDocument profileDocument = personalizedProfileGenerator.generate(userId);
            eventPublisher.publishEvent(new PersonalizedProfileGeneratedEvent(
                    userId,
                    profileDocument.getProfileVector(),
                    profileDocument.getKeyKeywords()
            ));

            log.info("Personalization profile generated successfully for userId: {}", userId);

        } catch (Exception e) {
            log.error("Failed to generate personalization profile for userId: {}", userId, e);
            throw e;
        }
    }
}
