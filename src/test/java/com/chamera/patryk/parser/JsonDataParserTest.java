package com.chamera.patryk.parser;

import com.chamera.patryk.model.Order;
import com.chamera.patryk.model.PaymentMethod;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonDataParserTest {

    private JsonDataParser jsonDataParser;
    private String testOrdersResourcesPath = "src/test/resources/orders/";
    private String testPaymentMethodsResourcesPath = "src/test/resources/paymentmethods/";


    @BeforeEach
    void setUp() {
        jsonDataParser = new JsonDataParser();
    }

    private String getOrderResourcePath(String fileName) {
        File file = new File(testOrdersResourcesPath + fileName);
        return file.getAbsolutePath();
    }

    private String getPaymentMethodResourcePath(String fileName) {
        File file = new File(testPaymentMethodsResourcesPath + fileName);
        return file.getAbsolutePath();
    }

    @Test
    void parseOrders_shouldParseValidOrdersFileCorrectly() throws IOException {
        String filePath = getOrderResourcePath("valid_orders.json");
        List<Order> orders = jsonDataParser.parseOrders(filePath);

        assertNotNull(orders);
        assertEquals(3, orders.size());

        Order order1 = orders.get(0);
        assertEquals("ORDER1", order1.getId());
        assertEquals(new BigDecimal("100.00"), order1.getValue());
        assertEquals(Arrays.asList("PROMO1", "PROMO2"), order1.getPromotions());

        Order order2 = orders.get(1);
        assertEquals("ORDER2", order2.getId());
        assertEquals(new BigDecimal("250.55"), order2.getValue());
        assertNull(order2.getPromotions());

        Order order3 = orders.get(2);
        assertEquals("ORDER3", order3.getId());
        assertEquals(new BigDecimal("75.20"), order3.getValue());
        assertEquals(Collections.emptyList(), order3.getPromotions());
    }

    @Test
    void parseOrders_shouldReturnEmptyListForEmptyJsonArray() throws IOException {
        String filePath = getOrderResourcePath("empty_orders_list.json");
        List<Order> orders = jsonDataParser.parseOrders(filePath);

        assertNotNull(orders);
        assertTrue(orders.isEmpty());
    }

    @Test
    void parseOrders_shouldParseSingleOrderCorrectly() throws IOException {
        String filePath = getOrderResourcePath("single_order.json");
        List<Order> orders = jsonDataParser.parseOrders(filePath);

        assertNotNull(orders);
        assertEquals(1, orders.size());

        Order order = orders.get(0);
        assertEquals("SINGLE_ORDER", order.getId());
        assertEquals(new BigDecimal("99.99"), order.getValue());
        assertEquals(Collections.singletonList("SINGLE_PROMO"), order.getPromotions());
    }

    @Test
    void parseOrders_shouldThrowFileNotFoundExceptionForNonExistentFile(@TempDir Path tempDir) {
        Path nonExistentFilePath = tempDir.resolve("non_existent_orders.json");
        String filePath = nonExistentFilePath.toString();

        assertThrows(FileNotFoundException.class, () -> {
            jsonDataParser.parseOrders(filePath);
        });
    }

    @Test
    void parseOrders_shouldThrowJsonProcessingExceptionForInvalidJsonStructure() {
        String filePath = getOrderResourcePath("invalid_structure_orders.json");
        assertThrows(JsonProcessingException.class, () -> {
            jsonDataParser.parseOrders(filePath);
        });
    }

    @Test
    void parseOrders_shouldIgnoreExtraFieldsWhenFailOnUnknownPropertiesIsFalse() throws IOException {
        String filePath = getOrderResourcePath("extra_fields_order.json");
        List<Order> orders = jsonDataParser.parseOrders(filePath);

        assertNotNull(orders);
        assertEquals(1, orders.size());

        Order order = orders.get(0);
        assertEquals("ORDER_EXTRA", order.getId());
        assertEquals(new BigDecimal("50.00"), order.getValue());
        assertEquals(Collections.singletonList("PROMO_X"), order.getPromotions());
    }

    @Test
    void parseOrders_shouldSetMissingFieldsToNull() throws IOException {
        String filePath = getOrderResourcePath("missing_value_field_order.json");
        List<Order> orders = jsonDataParser.parseOrders(filePath);

        assertNotNull(orders);
        assertEquals(1, orders.size());

        Order order = orders.get(0);
        assertEquals("ORDER_MISSING_VALUE", order.getId());
        assertNull(order.getValue());
        assertNull(order.getPromotions());
    }

    @Test
    void parseOrders_shouldThrowJsonProcessingExceptionForInvalidDataType() {
        String filePath = getOrderResourcePath("invalid_data_type_order.json");
        assertThrows(JsonProcessingException.class, () -> {
            jsonDataParser.parseOrders(filePath);
        });
    }

    @Test
    void parseOrders_shouldHandleOrderWithEmptyPromotionsList() throws IOException {
        String filePath = getOrderResourcePath("valid_orders.json");
        List<Order> orders = jsonDataParser.parseOrders(filePath);
        Order order3 = orders.stream().filter(o -> "ORDER3".equals(o.getId())).findFirst().orElse(null);

        assertNotNull(order3);
        assertNotNull(order3.getPromotions());
        assertTrue(order3.getPromotions().isEmpty());
    }

    @Test
    void parseOrders_shouldHandleOrderWithoutPromotionsField() throws IOException {
        String filePath = getOrderResourcePath("valid_orders.json");
        List<Order> orders = jsonDataParser.parseOrders(filePath);
        Order order2 = orders.stream().filter(o -> "ORDER2".equals(o.getId())).findFirst().orElse(null);

        assertNotNull(order2);
        assertNull(order2.getPromotions());
    }

    @Test
    void parsePaymentMethods_shouldParseValidFileCorrectly() throws IOException {
        String filePath = getPaymentMethodResourcePath("valid_payment_methods.json");
        List<PaymentMethod> paymentMethods = jsonDataParser.parsePaymentMethods(filePath);

        assertNotNull(paymentMethods);
        assertEquals(3, paymentMethods.size());

        PaymentMethod pm1 = paymentMethods.get(0);
        assertEquals("PUNKTY", pm1.getId());
        assertEquals(15, pm1.getDiscount());
        assertEquals(new BigDecimal("100.00"), pm1.getLimit());

        PaymentMethod pm2 = paymentMethods.get(1);
        assertEquals("mZysk", pm2.getId());
        assertEquals(10, pm2.getDiscount());
        assertEquals(new BigDecimal("180.50"), pm2.getLimit());

        PaymentMethod pm3 = paymentMethods.get(2);
        assertEquals("BosBankrut", pm3.getId());
        assertEquals(5, pm3.getDiscount());
        assertEquals(new BigDecimal("200.00"), pm3.getLimit());
    }

    @Test
    void parsePaymentMethods_shouldReturnEmptyListForEmptyJsonArray() throws IOException {
        String filePath = getPaymentMethodResourcePath("empty_payment_methods_list.json");
        List<PaymentMethod> paymentMethods = jsonDataParser.parsePaymentMethods(filePath);

        assertNotNull(paymentMethods);
        assertTrue(paymentMethods.isEmpty());
    }

    @Test
    void parsePaymentMethods_shouldParseSingleMethodCorrectly() throws IOException {
        String filePath = getPaymentMethodResourcePath("single_payment_method.json");
        List<PaymentMethod> paymentMethods = jsonDataParser.parsePaymentMethods(filePath);

        assertNotNull(paymentMethods);
        assertEquals(1, paymentMethods.size());

        PaymentMethod pm = paymentMethods.get(0);
        assertEquals("SuperKarta", pm.getId());
        assertEquals(20, pm.getDiscount());
        assertEquals(new BigDecimal("500.75"), pm.getLimit());
    }

    @Test
    void parsePaymentMethods_shouldThrowFileNotFoundExceptionForNonExistentFile(@TempDir Path tempDir) {
        Path nonExistentFilePath = tempDir.resolve("non_existent_payments.json");
        String filePath = nonExistentFilePath.toString();

        assertThrows(FileNotFoundException.class, () -> {
            jsonDataParser.parsePaymentMethods(filePath);
        });
    }

    @Test
    void parsePaymentMethods_shouldThrowJsonProcessingExceptionForInvalidJsonStructure() {
        String filePath = getPaymentMethodResourcePath("invalid_structure_payment_methods.json");
        assertThrows(JsonProcessingException.class, () -> {
            jsonDataParser.parsePaymentMethods(filePath);
        });
    }

    @Test
    void parsePaymentMethods_shouldIgnoreExtraFields() throws IOException {
        String filePath = getPaymentMethodResourcePath("extra_fields_payment_method.json");
        List<PaymentMethod> paymentMethods = jsonDataParser.parsePaymentMethods(filePath);

        assertNotNull(paymentMethods);
        assertEquals(1, paymentMethods.size());

        PaymentMethod pm = paymentMethods.get(0);
        assertEquals("KARTA_Z_BONUSEM", pm.getId());
        assertEquals(7, pm.getDiscount());
        assertEquals(new BigDecimal("70.00"), pm.getLimit());
    }

    @Test
    void parsePaymentMethods_shouldSetMissingFieldsToDefaultOrNull() throws IOException {
        String filePath = getPaymentMethodResourcePath("missing_fields_payment_method.json");
        List<PaymentMethod> paymentMethods = jsonDataParser.parsePaymentMethods(filePath);

        assertNotNull(paymentMethods);
        assertEquals(1, paymentMethods.size());

        PaymentMethod pm = paymentMethods.get(0);
        assertEquals("KARTA_BRAKI", pm.getId());
        assertEquals(0, pm.getDiscount());
        assertNull(pm.getLimit());
    }

    @Test
    void parsePaymentMethods_shouldThrowJsonProcessingExceptionForInvalidDataType() {
        String filePath = getPaymentMethodResourcePath("invalid_data_type_payment_method.json");
        assertThrows(JsonProcessingException.class, () -> {
            jsonDataParser.parsePaymentMethods(filePath);
        });
    }

    @Test
    void parsePaymentMethods_shouldThrowJsonProcessingExceptionForInvalidLimitFormat() {
        String filePath = getPaymentMethodResourcePath("payment_method_invalid_limit_format.json");
        assertThrows(JsonProcessingException.class, () -> {
            jsonDataParser.parsePaymentMethods(filePath);
        });
    }
}