package ru.practicum.moviehub.api;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponse extends RuntimeException  {

    final private int status;
    final private String error;
    final private List<String> errors;

    public ErrorResponse(int status, String error, List<String> errors) {
        this.status = status;
        this.error = error;
        this.errors = errors;
    }

    public ErrorResponse(int status, String error) {
        this.status = status;
        this.error = error;
        this.errors = null;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return LocalDateTime.now() + error;
    }

    public List<String> getErrors() {
        return errors;
    }
}