package com.chamera.patryk;


import com.chamera.patryk.exception.ArgsValidationException;
import com.chamera.patryk.exception.FileValidationException;
import com.chamera.patryk.exception.ProcessingException;

import java.io.IOException;

public class Main {

    // java -jar target/app.jar src/main/resources/orders.json src/main/resources/paymentmethods.json
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
            System.err.println("Unexpected error occured: " + e.getMessage());
            System.exit(1);
        }


    }
}