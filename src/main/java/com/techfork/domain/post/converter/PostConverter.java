package com.techfork.domain.post.converter;

import com.techfork.domain.post.dto.CompanyListResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostConverter {

    public CompanyListResponse toCompanyListResponse(List<String> companies) {
        return CompanyListResponse.builder()
                .companies(companies)
                .build();
    }
}
