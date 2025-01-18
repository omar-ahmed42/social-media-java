package com.omarahmed42.socialmedia.exception.handler;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.datastax.oss.driver.api.core.servererrors.AlreadyExistsException;
import com.omarahmed42.socialmedia.exception.BadRequestException;
import com.omarahmed42.socialmedia.exception.ConflictException;
import com.omarahmed42.socialmedia.exception.ForbiddenException;
import com.omarahmed42.socialmedia.exception.NotFoundException;
import com.omarahmed42.socialmedia.exception.UnauthorizedException;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    @Data
    private static class ErrorMessage implements Serializable {
        private String message;
        private boolean success = false;

        public ErrorMessage(String message) {
            this.message = message;
        }
    }

    @Getter
    @Setter
    private static class FieldErrorMessage extends ErrorMessage {
        private String fieldName;
        private String code;

        public FieldErrorMessage(String message, String fieldName, String code) {
            super(message);
            this.fieldName = fieldName;
            this.code = code;
        }
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFoundException(NotFoundException notFoundException) {
        logError(notFoundException);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage(notFoundException.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorMessage> handleConflictException(ConflictException conflictException) {
        logError(conflictException);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorMessage(conflictException.getMessage()));
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<Object> handleAlreadyExistsException(AlreadyExistsException alreadyExistsException) {
        logError(alreadyExistsException);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorMessage(alreadyExistsException.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorMessage> handleBadCredentialsException(BadCredentialsException badCredentialsException) {
        logError(badCredentialsException);
        return ResponseEntity.status(401).body(new ErrorMessage("Incorrect email or password"));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorMessage> handleForbiddenException(ForbiddenException forbiddenException) {
        logError(forbiddenException);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorMessage(forbiddenException.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorMessage> handleUnauthorizedException(UnauthorizedException unauthorizedException) {
        logError(unauthorizedException);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorMessage(unauthorizedException.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorMessage> handleBadRequestException(BadRequestException badRequestException) {
        logError(badRequestException);
        return ResponseEntity.badRequest().body(new ErrorMessage(badRequestException.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        logError(ex);
        List<ErrorMessage> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new FieldErrorMessage(err.getDefaultMessage(), err.getField(), err.getCode()))
                .collect(Collectors.toList());

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorMessage> handleRuntimeException(RuntimeException runtimeException) {
        logError(runtimeException);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception e) {
        logError(e);
        return ResponseEntity.internalServerError().build();
    }

    private void logError(Exception e) {
        log.error(e.getMessage(), e);
    }
}