package com.chamera.patryk.exception;

/**
 * Thrown to indicate an error during command-line argument validation.
 * This typically occurs if the number of provided arguments is incorrect.
 */
public class ArgsValidationException extends Exception{

    /**
     * Constructs an {@code ArgsValidationException} with the specified detail message.
     * @param message the detail message.
     */
    public ArgsValidationException(String message) {
        super(message);
    }
}
