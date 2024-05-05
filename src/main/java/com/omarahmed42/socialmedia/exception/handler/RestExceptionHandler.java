package com.omarahmed42.socialmedia.exception.handler;

import java.io.Serializable;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.datastax.oss.driver.api.core.servererrors.AlreadyExistsException;
import com.omarahmed42.socialmedia.exception.ConflictException;
import com.omarahmed42.socialmedia.exception.NotFoundException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorMessage> handleRuntimeException(RuntimeException runtimeException) {
        logError(runtimeException);
        return ResponseEntity.status(422).build();
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