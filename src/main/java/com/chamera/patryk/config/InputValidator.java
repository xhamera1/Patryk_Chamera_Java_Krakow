package com.chamera.patryk.config;

import com.chamera.patryk.exception.ArgsValidationException;
import com.chamera.patryk.exception.FileValidationException;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for validating application startup arguments and input file paths.
 * This class provides static methods to ensure that the command-line arguments
 * meet the expected criteria (e.g., correct number of arguments) and that
 * the specified file paths are valid, existent, readable, and of the correct type.
 */
public class InputValidator {

    /**
     * The expected number of command-line arguments for the application.
     */
    public final static int EXPECTED_ARGS_COUNT = 2;


    /**
     * Validates command-line arguments.
     * Checks for the correct number of arguments and validates each file path.
     *
     * @param args Command-line arguments (orders file path, payment methods file path).
     * @throws ArgsValidationException If argument count is incorrect.
     * @throws FileValidationException If file paths are invalid or files are inaccessible.
     */
    public static void validateArgs(String[] args) throws ArgsValidationException, FileValidationException {
        if (args == null || args.length != EXPECTED_ARGS_COUNT) {
            throw new ArgsValidationException("Invalid number of arguments. Expected " + EXPECTED_ARGS_COUNT +
                    ", got " + (args == null ? 0 : args.length) + ".");
        }
        String ordersFilePath = args[0];
        String paymentMethodsFilePath = args[1];

        validateSingleFile(ordersFilePath, "Orders");
        validateSingleFile(paymentMethodsFilePath, "Payment methods");
    }


    /**
     * Validates a single file path.
     * Checks for existence, type (regular file), readability, and .json extension.
     *
     * @param filePath Path to the file.
     * @param description File description for error messages (e.g., "Orders").
     * @throws FileValidationException If validation fails.
     */
    private static void validateSingleFile(String filePath, String description) throws FileValidationException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new FileValidationException(description + " file path cannot be null or empty.");
        }
        Path path;
        try {
            path = Paths.get(filePath);
        }
        catch (InvalidPathException e) {
            throw new FileValidationException(description + " file path is invalid: '" + filePath + "'. Reason: " + e.getMessage());
        }

        if (!Files.exists(path)) {
            throw new FileValidationException(description + " file not found at: " + filePath);
        }
        if (!Files.isRegularFile(path)) {
            throw new FileValidationException(description + " path does not point to a regular file: " + filePath);
        }
        if (!Files.isReadable(path)) {
            throw new FileValidationException(description + " file is not readable: " + filePath);
        }

        if(!filePath.toLowerCase().endsWith(".json")) {
            throw new FileValidationException(description + " file is expected to have a .json extension: " + filePath);
        }

    }
}
