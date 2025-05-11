package com.chamera.patryk;


import com.chamera.patryk.exception.ArgsValidationException;
import com.chamera.patryk.exception.FileValidationException;
import com.chamera.patryk.exception.ProcessingException;

import java.io.IOException;

/**
 * Main entry point for the payment optimization application.
 * This class is responsible for initiating the application execution
 * and handling top-level exceptions.
 */
public class Main {

    // Example command:
    // java -jar target/app.jar src/main/resources/orders.json src/main/resources/paymentmethods.json

    /**
     * The main method that starts the application.
     * It creates an instance of {@link ApplicationRunner} and invokes its run method.
     * Handles various types of exceptions that might occur during the application's lifecycle,
     * prints an appropriate error message to standard error, and exits with a non-zero status code.
     * If the application runs successfully, it exits with status code 0.
     *
     * @param args Command-line arguments passed to the application, typically paths to
     * the orders and payment methods JSON files.
     */
    public static void main(String[] args) {
        ApplicationRunner applicationRunner = new ApplicationRunner();
        try {
            applicationRunner.run(args);
            System.exit(0);
        } catch (ArgsValidationException | FileValidationException e) {
            System.err.println("Configuration error: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("File access/parsing error: " + e.getMessage());
            System.exit(1);
        } catch (ProcessingException e) {
            System.err.println("Processing exception: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error occurred: " + e.getMessage());
            System.exit(1);
        }


    }
}