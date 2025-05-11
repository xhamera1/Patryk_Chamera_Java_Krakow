package com.chamera.patryk.config;

import com.chamera.patryk.exception.ArgsValidationException;
import com.chamera.patryk.exception.FileValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {

    @TempDir
    Path tempDir;


    @Test
    void validateArgs_shouldPassWithValidArguments() throws IOException {
        Path ordersFile = Files.createFile(tempDir.resolve("orders.json"));
        Path paymentMethodsFile = Files.createFile(tempDir.resolve("paymentMethods.json"));

        String[] args = {ordersFile.toString(), paymentMethodsFile.toString()};

        assertDoesNotThrow(() -> InputValidator.validateArgs(args));
    }

    @Test
    void validateArgs_shouldThrowArgsValidationExceptionForNullArgs() {
        ArgsValidationException exception = assertThrows(ArgsValidationException.class, () -> {
            InputValidator.validateArgs(null);
        });
        assertTrue(exception.getMessage().contains("Invalid number of arguments. Expected 2, got 0."));
    }

    @Test
    void validateArgs_shouldThrowArgsValidationExceptionForTooFewArgs_Zero() {
        String[] args = {};
        ArgsValidationException exception = assertThrows(ArgsValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Invalid number of arguments. Expected 2, got 0."));
    }

    @Test
    void validateArgs_shouldThrowArgsValidationExceptionForTooFewArgs_One() {
        String[] args = {"one_arg.json"};
        ArgsValidationException exception = assertThrows(ArgsValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Invalid number of arguments. Expected 2, got 1."));
    }

    @Test
    void validateArgs_shouldThrowArgsValidationExceptionForTooManyArgs() {
        String[] args = {"one.json", "two.json", "three.json"};
        ArgsValidationException exception = assertThrows(ArgsValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Invalid number of arguments. Expected 2, got 3."));
    }


    @Test
    void validateArgs_shouldThrowFileValidationExceptionForNullFirstArgPath() throws IOException {
        Path validPaymentMethodsFile = Files.createFile(tempDir.resolve("paymentMethods.json"));
        String[] args = {null, validPaymentMethodsFile.toString()};

        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Orders file path cannot be null or empty."));
    }

    @Test
    void validateArgs_shouldThrowFileValidationExceptionForEmptyFirstArgPath() throws IOException {
        Path validPaymentMethodsFile = Files.createFile(tempDir.resolve("paymentMethods.json"));
        String[] args = {"", validPaymentMethodsFile.toString()};

        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Orders file path cannot be null or empty."));
    }

    @Test
    void validateArgs_shouldThrowFileValidationExceptionForBlankFirstArgPath() throws IOException {
        Path validPaymentMethodsFile = Files.createFile(tempDir.resolve("paymentMethods.json"));
        String[] args = {"   ", validPaymentMethodsFile.toString()};

        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Orders file path cannot be null or empty."));
    }

    @Test
    void validateArgs_shouldThrowFileValidationExceptionForInvalidFirstArgPathSyntax() throws IOException {
        Path validPaymentMethodsFile = Files.createFile(tempDir.resolve("paymentMethods.json"));
        String invalidPath = "invalid\0path.json";
        String[] args = {invalidPath, validPaymentMethodsFile.toString()};

        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Orders file path is invalid: '" + invalidPath + "'"));
    }

    @Test
    void validateArgs_shouldThrowFileValidationExceptionWhenFirstArgFileDoesNotExist() throws IOException {
        Path validPaymentMethodsFile = Files.createFile(tempDir.resolve("paymentMethods.json"));
        String nonExistentOrdersFile = tempDir.resolve("non_existent_orders.json").toString();
        String[] args = {nonExistentOrdersFile, validPaymentMethodsFile.toString()};

        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Orders file not found at: " + nonExistentOrdersFile));
    }

    @Test
    void validateArgs_shouldThrowFileValidationExceptionWhenFirstArgIsDirectory() throws IOException {
        Path ordersDirectory = Files.createDirectory(tempDir.resolve("orders_dir.json"));
        Path validPaymentMethodsFile = Files.createFile(tempDir.resolve("paymentMethods.json"));
        String[] args = {ordersDirectory.toString(), validPaymentMethodsFile.toString()};

        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Orders path does not point to a regular file: " + ordersDirectory));
    }

    @Test
    void validateArgs_shouldThrowFileValidationExceptionWhenFirstArgIsNotJson() throws IOException {
        Path ordersFileTxt = Files.createFile(tempDir.resolve("orders.txt"));
        Path validPaymentMethodsFile = Files.createFile(tempDir.resolve("paymentMethods.json"));
        String[] args = {ordersFileTxt.toString(), validPaymentMethodsFile.toString()};

        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Orders file is expected to have a .json extension: " + ordersFileTxt));
    }


    @Test
    void validateArgs_shouldThrowFileValidationExceptionForNullSecondArgPath() throws IOException {
        Path validOrdersFile = Files.createFile(tempDir.resolve("orders.json"));
        String[] args = {validOrdersFile.toString(), null};

        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Payment methods file path cannot be null or empty."));
    }

    @Test
    void validateArgs_shouldThrowFileValidationExceptionForEmptySecondArgPath() throws IOException {
        Path validOrdersFile = Files.createFile(tempDir.resolve("orders.json"));
        String[] args = {validOrdersFile.toString(), ""};

        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Payment methods file path cannot be null or empty."));
    }

    @Test
    void validateArgs_shouldThrowFileValidationExceptionWhenSecondArgFileDoesNotExist() throws IOException {
        Path validOrdersFile = Files.createFile(tempDir.resolve("orders.json"));
        String nonExistentPaymentMethodsFile = tempDir.resolve("non_existent_payments.json").toString();
        String[] args = {validOrdersFile.toString(), nonExistentPaymentMethodsFile};

        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Payment methods file not found at: " + nonExistentPaymentMethodsFile));
    }

    @Test
    void validateArgs_shouldThrowFileValidationExceptionWhenSecondArgIsDirectory() throws IOException {
        Path validOrdersFile = Files.createFile(tempDir.resolve("orders.json"));
        Path paymentMethodsDirectory = Files.createDirectory(tempDir.resolve("payments_dir.json"));
        String[] args = {validOrdersFile.toString(), paymentMethodsDirectory.toString()};

        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Payment methods path does not point to a regular file: " + paymentMethodsDirectory));
    }

    @Test
    void validateArgs_shouldThrowFileValidationExceptionWhenSecondArgIsNotJson() throws IOException {
        Path validOrdersFile = Files.createFile(tempDir.resolve("orders.json"));
        Path paymentMethodsFileTxt = Files.createFile(tempDir.resolve("payments.txt"));
        String[] args = {validOrdersFile.toString(), paymentMethodsFileTxt.toString()};

        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            InputValidator.validateArgs(args);
        });
        assertTrue(exception.getMessage().contains("Payment methods file is expected to have a .json extension: " + paymentMethodsFileTxt));
    }

}