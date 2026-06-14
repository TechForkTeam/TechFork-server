package com.techfork.auth.presentation.converter;

import com.techfork.auth.application.command.input.GenerateDeveloperTokenCommand;
import com.techfork.auth.application.command.result.DeveloperTokenResult;
import com.techfork.auth.presentation.response.DeveloperTokenResponse;
import org.springframework.stereotype.Component;

@Component
public class DeveloperTokenConverter {

    public GenerateDeveloperTokenCommand toGenerateDeveloperTokenCommand(Long userId) {
        return new GenerateDeveloperTokenCommand(userId);
    }

    public DeveloperTokenResponse toDeveloperTokenResponse(DeveloperTokenResult result) {
        return DeveloperTokenResponse.builder()
                .developerToken(result.developerToken())
                .build();
    }
}
