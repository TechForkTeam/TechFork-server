package com.techfork.personalization.integration;

import com.techfork.domain.recommendation.listener.PersonalizedProfileGeneratedEventListener;
import com.techfork.domain.recommendation.service.RecommendationService;
import com.techfork.personalization.application.event.PersonalizedProfileGeneratedEvent;
import com.techfork.useraccount.application.query.lookup.UserLookupService;
import com.techfork.useraccount.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Tag("integration")
@SpringJUnitConfig(PersonalizedProfileGeneratedAfterCommitIntegrationTest.TestConfig.class)
class PersonalizedProfileGeneratedAfterCommitIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserLookupService userLookupService;

    @Autowired
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        reset(userLookupService, recommendationService);
    }

    @Nested
    @DisplayName("profile generated event")
    class ProfileGeneratedEvent {

        @Test
        @DisplayName("프로필 생성 이벤트는 실제 트랜잭션 커밋 이후 추천 생성을 실행한다")
        void committedTransaction_GeneratesRecommendationAfterCommit() {
            Long userId = 1L;
            User user = mock(User.class);
            float[] profileVector = new float[]{0.1f, 0.2f};
            List<String> keyKeywords = List.of("Spring", "JPA");
            given(userLookupService.getUserOrThrow(userId)).willReturn(user);
            given(recommendationService.generateRecommendationsForUser(
                    eq(user),
                    argThat(vector -> Arrays.equals(vector, profileVector)),
                    eq(keyKeywords)
            )).willReturn(5);

            transactionTemplate.executeWithoutResult(status -> {
                eventPublisher.publishEvent(new PersonalizedProfileGeneratedEvent(userId, profileVector, keyKeywords));

                verifyNoInteractions(userLookupService, recommendationService);
            });

            verify(userLookupService).getUserOrThrow(userId);
            verify(recommendationService).generateRecommendationsForUser(
                    eq(user),
                    argThat(vector -> Arrays.equals(vector, profileVector)),
                    eq(keyKeywords)
            );
            verify(recommendationService, never()).generateRecommendationsForUser(user);
        }

        @Test
        @DisplayName("트랜잭션이 롤백되면 프로필 생성 이벤트의 추천 후처리는 실행되지 않는다")
        void rolledBackTransaction_DoesNotGenerateRecommendation() {
            transactionTemplate.executeWithoutResult(status -> {
                eventPublisher.publishEvent(new PersonalizedProfileGeneratedEvent(
                        1L,
                        new float[]{0.1f},
                        List.of("Spring")
                ));
                status.setRollbackOnly();
            });

            verifyNoInteractions(userLookupService, recommendationService);
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        PersonalizedProfileGeneratedEventListener personalizedProfileGeneratedEventListener(
                UserLookupService userLookupService,
                RecommendationService recommendationService
        ) {
            return new PersonalizedProfileGeneratedEventListener(userLookupService, recommendationService);
        }

        @Bean
        UserLookupService userLookupService() {
            return mock(UserLookupService.class);
        }

        @Bean
        RecommendationService recommendationService() {
            return mock(RecommendationService.class);
        }

        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
            return new TransactionTemplate(transactionManager);
        }
    }
}
