package com.techfork.global.exception;

import com.techfork.global.common.code.BaseCode;
import com.techfork.global.response.ReasonDTO;
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
