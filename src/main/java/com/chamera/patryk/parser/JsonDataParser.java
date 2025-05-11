package com.chamera.patryk.parser;

import com.chamera.patryk.model.Order;
import com.chamera.patryk.model.PaymentMethod;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Parses JSON input files into corresponding Java objects.
 * Uses Jackson ObjectMapper for deserialization of orders and payment methods.
 */
public class JsonDataParser {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a new {@code JsonDataParser} and initializes the ObjectMapper.
     * The ObjectMapper is configured to not fail on unknown properties,
     * allowing for more flexible JSON input structures.
     */
    public JsonDataParser() {
        objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }


    /**
     * Parses a JSON file containing a list of orders.
     *
     * @param filePath The path to the orders JSON file.
     * @return A list of {@link Order} objects parsed from the file.
     * @throws IOException If an I/O error occurs during file reading or parsing.
     */
    public List<Order> parseOrders(String filePath) throws IOException {
        File ordersFile = new File(filePath);
        return objectMapper.readValue(ordersFile, new TypeReference<List<Order>>() {});
    }


    /**
     * Parses a JSON file containing a list of payment methods.
     *
     * @param filePath The path to the payment methods JSON file.
     * @return A list of {@link PaymentMethod} objects parsed from the file.
     * @throws IOException If an I/O error occurs during file reading or parsing.
     */
    public List<PaymentMethod> parsePaymentMethods(String filePath) throws IOException {
        File paymentMethodFile = new File(filePath);
        return objectMapper.readValue(paymentMethodFile, new TypeReference<List<PaymentMethod>>() {});
    }

}
