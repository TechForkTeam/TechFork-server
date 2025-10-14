package com.techfork.global.exception;

import com.techfork.global.response.BaseResponseDTO;
import com.techfork.global.response.code.CommonErrorCode;
import com.techfork.global.response.dto.ErrorDetailDTO;
import com.techfork.global.response.dto.ReasonDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(annotations = {RestController.class})
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 비즈니스 로직 예외
     */
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<BaseResponseDTO<Object>> handleGeneralException(
            GeneralException ex, WebRequest request) {

        ReasonDTO errorReason = ex.getErrorReason();

        if (errorReason.httpStatus().is4xxClientError()) {
            log.info("Business Exception (4xx): uri={}, code={}, message={}",
                    getRequestURI(request), errorReason.code(), errorReason.message());
        } else {
            log.warn("Business Exception (5xx): uri={}, code={}, message={}",
                    getRequestURI(request), errorReason.code(), errorReason.message());
        }

        return BaseResponseDTO.of(ex.getCode());
    }

    /**
     * @Valid 어노테이션으로 binding error 발생 시 (@RequestBody)
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        List<ErrorDetailDTO.FieldErrorDTO> fieldErrors = extractFieldErrorDetails(ex);

        log.info("Validation failed: uri={}, errors={}", getRequestURI(request), fieldErrors);

        ErrorDetailDTO errorDetail = ErrorDetailDTO.of(fieldErrors);

        return BaseResponseDTO.ofObject(CommonErrorCode.VALIDATION_FAILED, errorDetail);
    }

    /**
     * @Validated 어노테이션으로 binding error 발생 시 (@PathVariable, @RequestParam)
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException ex, WebRequest request) {

        List<ErrorDetailDTO.FieldErrorDTO> violations = extractConstraintViolationDetails(ex);

        log.info("Constraint violation: uri={}, violations={}", getRequestURI(request), violations);

        ErrorDetailDTO errorDetail = ErrorDetailDTO.of(violations);

        return BaseResponseDTO.ofObject(CommonErrorCode.VALIDATION_FAILED, errorDetail);
    }

    /**
     * RequestParam 타입 변환 실패
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        String expectedType = getExpectedType(ex);

        log.info("Type mismatch: uri={}, param={}, value={}, expectedType={}",
                getRequestURI(request), ex.getName(), ex.getValue(), expectedType);

        String detail = String.format("파라미터 '%s'의 값 '%s'을(를) %s 타입으로 변환할 수 없습니다.",
                ex.getName(), ex.getValue(), expectedType);

        ErrorDetailDTO errorDetail = ErrorDetailDTO.of(detail);

        return BaseResponseDTO.ofObject(CommonErrorCode.INVALID_PARAMETER_TYPE, errorDetail);
    }

    /**
     * 필수 RequestParam 누락
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        log.info("Missing parameter: uri={}, param={}", getRequestURI(request), ex.getParameterName());

        String detail = String.format("필수 파라미터 '%s' (%s 타입)가 누락되었습니다.",
                ex.getParameterName(), ex.getParameterType());

        ErrorDetailDTO errorDetail = ErrorDetailDTO.of(detail);

        return BaseResponseDTO.ofObject(CommonErrorCode.MISSING_REQUIRED_PARAMETER, errorDetail);
    }

    /**
     * JSON 파싱 오류 (@RequestBody)
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        log.warn("JSON parsing error: uri={}, message={}", getRequestURI(request), ex.getMessage());

        String detail = "요청 본문의 JSON 형식이 올바르지 않거나 필수 필드가 누락되었습니다.";
        ErrorDetailDTO errorDetail = ErrorDetailDTO.of(detail);

        return BaseResponseDTO.ofObject(CommonErrorCode.INVALID_REQUEST_FORMAT, errorDetail);
    }

    /**
     * 최종 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(
            Exception ex, WebRequest request) {

        log.error("Unexpected exception: uri={}, type={}, message={}, stackTrace={}",
                getRequestURI(request), ex.getClass().getSimpleName(), ex.getMessage(), getStackTrace(ex));

        ErrorDetailDTO errorDetail = ErrorDetailDTO.of("예상치 못한 오류가 발생했습니다. 관리자에게 문의해주세요.");

        return BaseResponseDTO.ofObject(CommonErrorCode.INTERNAL_SERVER_ERROR, errorDetail);
    }

    private String getRequestURI(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private List<ErrorDetailDTO.FieldErrorDTO> extractFieldErrorDetails(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorDetailDTO.FieldErrorDTO.of(
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()
                ))
                .toList();
    }

    private List<ErrorDetailDTO.FieldErrorDTO> extractConstraintViolationDetails(
            jakarta.validation.ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .map(violation -> {
                    String propertyPath = violation.getPropertyPath().toString();

                    // 메서드명.파라미터명 형태에서 파라미터명만 추출
                    String fieldName = propertyPath.contains(".")
                            ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1)
                            : propertyPath;

                    return ErrorDetailDTO.FieldErrorDTO.of(
                            fieldName,
                            violation.getInvalidValue(),
                            violation.getMessage()
                    );
                })
                .toList();
    }

    private String getExpectedType(MethodArgumentTypeMismatchException ex) {
        return Optional.ofNullable(ex.getRequiredType())
                .map(Class::getSimpleName)
                .orElse("unknown");
    }

    private String getStackTrace(Exception ex) {
        return Arrays.stream(ex.getStackTrace())
                .limit(5)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining(" | "));
    }
}
