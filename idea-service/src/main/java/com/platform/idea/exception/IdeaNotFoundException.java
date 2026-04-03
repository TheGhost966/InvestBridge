package com.platform.idea.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class IdeaNotFoundException extends RuntimeException {
    public IdeaNotFoundException(String id) {
        super("Idea not found: " + id);
    }
}
