package com.jacob.mtg.common.exception;

/** Thrown when a resource does not exist OR belongs to another user (never reveal which, to avoid IDOR leaks). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
