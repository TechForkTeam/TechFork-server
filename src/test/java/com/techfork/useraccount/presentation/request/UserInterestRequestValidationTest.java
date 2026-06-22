package com.techfork.useraccount.presentation.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserInterestRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Nested
    @DisplayName("SaveInterestRequest")
    class SaveInterestRequestValidation {

        @Test
        @DisplayName("null 관심사 항목을 허용하지 않는다")
        void nullInterestItem_ReturnsValidationError() {
            SaveInterestRequest request = new SaveInterestRequest(Collections.singletonList(null));

            assertThat(validator.validate(request))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("interests[0].<list element>");
        }

        @Test
        @DisplayName("빈 카테고리를 허용하지 않는다")
        void blankCategory_ReturnsValidationError() {
            SaveInterestRequest request = new SaveInterestRequest(List.of(
                    UserInterestRequest.builder()
                            .category("")
                            .keywords(List.of("JAVA"))
                            .build()
            ));

            assertThat(validator.validate(request))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("interests[0].category");
        }

        @Test
        @DisplayName("빈 키워드를 허용하지 않는다")
        void blankKeyword_ReturnsValidationError() {
            SaveInterestRequest request = new SaveInterestRequest(List.of(
                    UserInterestRequest.builder()
                            .category("BACKEND")
                            .keywords(List.of(""))
                            .build()
            ));

            assertThat(validator.validate(request))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("interests[0].keywords[0].<list element>");
        }
    }

    @Nested
    @DisplayName("OnboardingRequest")
    class OnboardingRequestValidation {

        @Test
        @DisplayName("관심사 항목을 중첩 검증한다")
        void invalidNestedInterestItem_ReturnsValidationError() {
            OnboardingRequest request = new OnboardingRequest(
                    "테크포크유저",
                    "user@techfork.com",
                    null,
                    List.of(
                            UserInterestRequest.builder()
                                    .category(" ")
                                    .keywords(List.of("JAVA"))
                                    .build()
                    )
            );

            assertThat(validator.validate(request))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("interests[0].category");
        }
    }
}
