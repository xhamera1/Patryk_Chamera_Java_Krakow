package com.chamera.patryk.config;

import com.chamera.patryk.exception.ArgsValidationException;
import com.chamera.patryk.exception.FileValidationException;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class InputValidator {

    public final static int EXPECTED_ARGS_COUNT = 2;

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
