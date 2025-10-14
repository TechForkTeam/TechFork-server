package com.techfork.global.exception;

import com.techfork.global.response.code.BaseCode;
import com.techfork.global.response.dto.ReasonDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GeneralException extends RuntimeException {

    private final BaseCode code;

    public ReasonDTO getErrorReason() {
        return this.code.getReason();
    }
}
