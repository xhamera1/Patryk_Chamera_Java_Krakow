package com.chamera.patryk.exception;


/**
 * Thrown to indicate an error during input file validation.
 * This can occur if a file is not found, not readable, not a regular file,
 * has an incorrect extension, or the path is invalid.
 */
public class FileValidationException extends Exception{


    /**
     * Constructs a {@code FileValidationException} with the specified detail message.
     * @param message the detail message.
     */
    public FileValidationException(String message) {
        super(message);
    }
}
