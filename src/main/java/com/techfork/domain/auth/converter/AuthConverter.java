package com.techfork.domain.auth.converter;

import com.techfork.domain.auth.dto.DeveloperTokenResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthConverter {

    public DeveloperTokenResponse toDeveloperTokenResponse(String token){
        return DeveloperTokenResponse.builder()
                .developerToken(token)
                .build();
    }
}
