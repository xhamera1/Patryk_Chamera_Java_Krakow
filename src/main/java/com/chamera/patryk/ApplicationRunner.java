package com.chamera.patryk;

import com.chamera.patryk.config.InputValidator;
import com.chamera.patryk.exception.ArgsValidationException;
import com.chamera.patryk.exception.FileValidationException;
import com.chamera.patryk.exception.ProcessingException;
import com.chamera.patryk.model.Order;
import com.chamera.patryk.model.PaymentMethod;
import com.chamera.patryk.parser.JsonDataParser;
import com.chamera.patryk.service.PaymentOptimizerService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * The main application flow.
 * This class is responsible for validating input, parsing data,
 * invoking the payment optimization logic, and printing the results.
 */
public class ApplicationRunner {


    /**
     * Executes the primary logic of the payment optimization application.
     * The process involves:
     * 1. Validating command-line arguments (paths to orders and payment methods files).
     * 2. Parsing the orders and payment methods from the specified JSON files.
     * 3. Performing basic checks to ensure that data was loaded (e.g., orders list is not empty).
     * 4. Invoking the {@link PaymentOptimizerService} to determine the optimal payment distribution.
     * 5. Printing the resulting total amounts spent per payment method to standard output.
     *
     * @param args Command-line arguments: expected to be two strings representing
     * the file path for orders and the file path for payment methods.
     * @throws ArgsValidationException If command-line arguments are invalid.
     * @throws FileValidationException If input files are invalid or inaccessible.
     * @throws IOException If an I/O error occurs during file parsing.
     * @throws ProcessingException If an error occurs during data processing,
     * such as empty orders or payment methods lists.
     */
    public void run(String[] args) throws ArgsValidationException, FileValidationException, IOException, ProcessingException {
        InputValidator.validateArgs(args);

        String ordersFilename = args[0];
        String paymentMethodFilename = args[1];

        JsonDataParser jsonDataParser = new JsonDataParser();
        List<Order> orders = jsonDataParser.parseOrders(ordersFilename);
        List<PaymentMethod> paymentMethods = jsonDataParser.parsePaymentMethods(paymentMethodFilename);

        if (orders.isEmpty()) {
            throw new ProcessingException("No orders were loaded from the file '" + ordersFilename + "' or the file was empty. Cannot proceed with payment optimization.");
        }

        if (paymentMethods.isEmpty()) {
            throw new ProcessingException("No payment methods (including 'PUNKTY') were loaded from the file '" + paymentMethodFilename + "'. Cannot pay for orders.");
        }

        PaymentOptimizerService paymentOptimizerService = new PaymentOptimizerService();
        Map<String, BigDecimal> totalSpentByMethods = paymentOptimizerService.optimizePayments(orders, paymentMethods);

        for (Map.Entry<String, BigDecimal> entry : totalSpentByMethods.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }







    }
}
