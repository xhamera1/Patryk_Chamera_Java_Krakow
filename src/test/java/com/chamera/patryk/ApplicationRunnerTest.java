package com.chamera.patryk;

import com.chamera.patryk.config.InputValidator;
import com.chamera.patryk.exception.ArgsValidationException;
import com.chamera.patryk.exception.FileValidationException;
import com.chamera.patryk.exception.ProcessingException;
import com.chamera.patryk.parser.JsonDataParser;
import com.chamera.patryk.service.PaymentOptimizerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;


import static org.junit.jupiter.api.Assertions.*;

//testy integracyjne dla aplikacji

@ExtendWith(MockitoExtension.class)
class ApplicationRunnerTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    private ApplicationRunner applicationRunner;

    @TempDir
    Path tempDir;

    private MockedStatic<InputValidator> mockedInputValidator;


    @Mock
    private JsonDataParser mockJsonDataParser;
    @Mock
    private PaymentOptimizerService mockPaymentOptimizerService;


    private static BigDecimal bd(String val) {
        return new BigDecimal(val).setScale(2, RoundingMode.HALF_UP);
    }

    @BeforeEach
    void setUp() {
        applicationRunner = new ApplicationRunner();
        System.setOut(new PrintStream(outContent));
        mockedInputValidator = Mockito.mockStatic(InputValidator.class);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        mockedInputValidator.close();
    }

    @Test
    @DisplayName("run should successfully process valid inputs and print results")
    void run_happyPath_printsResults() throws Exception {
        Path ordersFile = Files.createFile(tempDir.resolve("orders_happy.json"));
        Path paymentMethodsFile = Files.createFile(tempDir.resolve("payments_happy.json"));
        String[] args = {ordersFile.toString(), paymentMethodsFile.toString()};

        Files.writeString(ordersFile, "[{\"id\":\"O1\",\"value\":\"10.00\"}]");
        Files.writeString(paymentMethodsFile, "[{\"id\":\"CARD_X\",\"discount\":0,\"limit\":\"20.00\"}, {\"id\":\"PUNKTY\",\"discount\":0,\"limit\":\"5.00\"}]");

        mockedInputValidator.when(() -> InputValidator.validateArgs(args)).thenAnswer(invocation -> null);

        applicationRunner.run(args);

        String output = outContent.toString().replace("\r\n", "\n").trim();
        assertTrue(output.contains("PUNKTY 5.00"), "Output should contain PUNKTY spending");
        assertTrue(output.contains("CARD_X 4.00"), "Output should contain CARD_X spending");
        assertEquals(2, output.lines().count(), "Output should have 2 lines");
    }

    @Test
    @DisplayName("run should rethrow ArgsValidationException from InputValidator")
    void run_rethrowsArgsValidationExceptionFromValidator() throws ArgsValidationException, FileValidationException {
        String[] invalidArgs = {"one_arg_only.json"};
        ArgsValidationException expectedException = new ArgsValidationException("Test ArgsValidationException from validator");

        mockedInputValidator.when(() -> InputValidator.validateArgs(invalidArgs)).thenThrow(expectedException);

        ArgsValidationException thrown = assertThrows(ArgsValidationException.class, () -> {
            applicationRunner.run(invalidArgs);
        });
        assertSame(expectedException, thrown, "Should rethrow the exact ArgsValidationException");
    }

    @Test
    @DisplayName("run should rethrow FileValidationException from InputValidator")
    void run_rethrowsFileValidationExceptionFromValidator() throws ArgsValidationException, FileValidationException {
        String[] argsWithInvalidFile = {"non_existent_orders.json", "payments.json"};
        FileValidationException expectedException = new FileValidationException("Test FileValidationException from validator");

        mockedInputValidator.when(() -> InputValidator.validateArgs(argsWithInvalidFile)).thenThrow(expectedException);

        FileValidationException thrown = assertThrows(FileValidationException.class, () -> {
            applicationRunner.run(argsWithInvalidFile);
        });
        assertSame(expectedException, thrown, "Should rethrow the exact FileValidationException");
    }

    @Test
    @DisplayName("run should throw ProcessingException if orders list is empty after parsing")
    void run_throwsProcessingExceptionForEmptyOrdersList() throws Exception {
        Path ordersFile = Files.createFile(tempDir.resolve("empty_orders.json"));
        Path paymentMethodsFile = Files.createFile(tempDir.resolve("valid_payments.json"));
        String[] args = {ordersFile.toString(), paymentMethodsFile.toString()};

        Files.writeString(ordersFile, "[]");
        Files.writeString(paymentMethodsFile, "[{\"id\":\"PUNKTY\",\"discount\":10,\"limit\":\"100.00\"}]");

        mockedInputValidator.when(() -> InputValidator.validateArgs(args)).thenAnswer(invocation -> null);

        ProcessingException thrown = assertThrows(ProcessingException.class, () -> {
            applicationRunner.run(args);
        });
        assertTrue(thrown.getMessage().startsWith("No orders were loaded from the file"));
    }

    @Test
    @DisplayName("run should throw ProcessingException if payment methods list is empty after parsing")
    void run_throwsProcessingExceptionForEmptyPaymentMethodsList() throws Exception {
        Path ordersFile = Files.createFile(tempDir.resolve("valid_orders.json"));
        Path paymentMethodsFile = Files.createFile(tempDir.resolve("empty_payments.json"));
        String[] args = {ordersFile.toString(), paymentMethodsFile.toString()};

        Files.writeString(ordersFile, "[{\"id\":\"O1\",\"value\":\"10.00\"}]");
        Files.writeString(paymentMethodsFile, "[]");

        mockedInputValidator.when(() -> InputValidator.validateArgs(args)).thenAnswer(invocation -> null);

        ProcessingException thrown = assertThrows(ProcessingException.class, () -> {
            applicationRunner.run(args);
        });
        assertTrue(thrown.getMessage().startsWith("No payment methods (including 'PUNKTY') were loaded"));
    }

    @Test
    @DisplayName("run should rethrow RuntimeException from PaymentOptimizerService")
    void run_rethrowsRuntimeExceptionFromOptimizerService() throws Exception {
        Path ordersFile = Files.createFile(tempDir.resolve("orders_for_optimizer_exception.json"));
        Path paymentMethodsFile = Files.createFile(tempDir.resolve("payments_for_optimizer_exception.json"));
        String[] args = {ordersFile.toString(), paymentMethodsFile.toString()};

        Files.writeString(ordersFile, "[{\"id\":\"UNPAYABLE_ORDER\",\"value\":\"1000.00\"}]");
        Files.writeString(paymentMethodsFile, "[{\"id\":\"PUNKTY\",\"discount\":10,\"limit\":\"10.00\"}]");

        mockedInputValidator.when(() -> InputValidator.validateArgs(args)).thenAnswer(invocation -> null);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            applicationRunner.run(args);
        });
        assertTrue(thrown.getMessage().contains("No possible payment option found for order UNPAYABLE_ORDER"));
    }
}