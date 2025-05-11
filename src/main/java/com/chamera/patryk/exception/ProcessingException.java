package com.chamera.patryk.exception;


/**
 * Thrown to indicate an error during the main processing logic of the application,
 * after initial validation of arguments and files has passed.
 * This might include issues like inability to process loaded data or other runtime problems.
 */
public class ProcessingException extends Exception{

    /**
     * Constructs a {@code ProcessingException} with the specified detail message.
     * @param message the detail message.
     */
    public ProcessingException(String message) {
        super(message);
    }
}
