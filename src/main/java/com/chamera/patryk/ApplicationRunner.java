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

public class ApplicationRunner {

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
