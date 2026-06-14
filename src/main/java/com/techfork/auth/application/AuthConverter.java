package com.techfork.auth.application;

import com.techfork.auth.application.dto.DeveloperTokenResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthConverter {

    public DeveloperTokenResponse toDeveloperTokenResponse(String token){
        return DeveloperTokenResponse.builder()
                .developerToken(token)
                .build();
    }

}
