package com.chamera.patryk.parser;

import com.chamera.patryk.model.Order;
import com.chamera.patryk.model.PaymentMethod;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JsonDataParser {

    private final ObjectMapper objectMapper;

    public JsonDataParser() {
        objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    public List<Order> parseOrders(String filePath) throws IOException {
        File ordersFile = new File(filePath);
        return objectMapper.readValue(ordersFile, new TypeReference<List<Order>>() {});
    }

    public List<PaymentMethod> parsePaymentMethods(String filePath) throws IOException {
        File paymentMethodFile = new File(filePath);
        return objectMapper.readValue(paymentMethodFile, new TypeReference<List<PaymentMethod>>() {});
    }

}
