package com.chamera.patryk.service;

import com.chamera.patryk.model.Order;
import com.chamera.patryk.model.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PaymentOptimizerServiceTest {

    private PaymentOptimizerService paymentOptimizerService;

    private static final String POINTS_ID_STRING = "PUNKTY";
    private static final BigDecimal PARTIAL_POINTS_ORDER_DISCOUNT_PERCENTAGE = new BigDecimal("0.10");
    private static final BigDecimal MIN_POINTS_PERCENTAGE_FOR_PARTIAL_DISCOUNT = new BigDecimal("0.10");


    @BeforeEach
    void setUp() {
        paymentOptimizerService = new PaymentOptimizerService();
    }


    private static BigDecimal bd(String val) {
        return new BigDecimal(val).setScale(2, RoundingMode.HALF_UP);
    }

    @Test
    @DisplayName("Should return 0.00 discount when no payment methods are available")
    void calculateMaxTheoreticalDiscount_noPaymentMethods() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.emptyList());
        Map<String, PaymentMethod> paymentMethodMap = Collections.emptyMap();

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("0.00"), discount);
    }

    @Test
    @DisplayName("Should return 0.00 discount for order with value 0.00")
    void calculateMaxTheoreticalDiscount_orderValueZero() {
        Order order = new Order("ORDER1", bd("0.00"), Collections.singletonList("CARD_A"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 10, bd("100.00")));
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 15, bd("100.00")));

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("0.00"), discount);
    }

    @Test
    @DisplayName("Should consider only partial points discount if PUNKTY limit is low but sufficient for partial")
    void calculateMaxTheoreticalDiscount_pointsAvailableOnlyForPartialDiscount() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.emptyList());
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 5, bd("15.00")));

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("10.00"), discount);
    }

    @Test
    @DisplayName("Should use full points discount if PUNKTY limit is sufficient and gives max discount")
    void calculateMaxTheoreticalDiscount_fullPointsDiscountIsMax() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.emptyList());
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 15, bd("90.00")));

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("15.00"), discount);
    }

    @Test
    @DisplayName("Should use partial points discount if PUNKTY limit is sufficient for partial but not for full payment, and partial discount is better than potential full with low points discount")
    void calculateMaxTheoreticalDiscount_partialPointsBetterThanInsufficientFullPoints() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.emptyList());
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 5, bd("80.00")));

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("10.00"), discount);
    }


    @Test
    @DisplayName("Should return 0.00 if only promotional card exists but its limit is too low")
    void calculateMaxTheoreticalDiscount_promoCardLimitTooLow() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.singletonList("CARD_A"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 10, bd("80.00")));

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("0.00"), discount);
    }

    @Test
    @DisplayName("Should use promotional card discount if its limit is sufficient and it's the best option")
    void calculateMaxTheoreticalDiscount_promoCardIsBest() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.singletonList("CARD_A"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 20, bd("90.00")));
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 15, bd("90.00")));


        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("20.00"), discount);
    }

    @Test
    @DisplayName("Promo card gives better discount than full points and partial points")
    void calculateMaxTheoreticalDiscount_cardBeatsFullAndPartialPoints() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.singletonList("CARD_SUPER"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_SUPER", new PaymentMethod("CARD_SUPER", 25, bd("100.00")));
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 15, bd("100.00")));

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("25.00"), discount);
    }

    @Test
    @DisplayName("Full points give better discount than promo card and partial points")
    void calculateMaxTheoreticalDiscount_fullPointsBeatsCardAndPartialPoints() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.singletonList("CARD_OK"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_OK", new PaymentMethod("CARD_OK", 12, bd("100.00")));
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 20, bd("100.00")));

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("20.00"), discount);
    }

    @Test
    @DisplayName("Partial points (10% discount) give better discount than promo card and full points")
    void calculateMaxTheoreticalDiscount_partialPointsBeatsCardAndFullPoints() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.singletonList("CARD_WEAK"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_WEAK", new PaymentMethod("CARD_WEAK", 5, bd("100.00")));

        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 8, bd("100.00")));

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("10.00"), discount);
    }

    @Test
    @DisplayName("Should pick the best discount from multiple promotional cards")
    void calculateMaxTheoreticalDiscount_multiplePromoCards_bestSelected() {
        Order order = new Order("ORDER1", bd("100.00"), Arrays.asList("CARD_A", "CARD_B", "CARD_C"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 10, bd("100.00")));
        paymentMethodMap.put("CARD_B", new PaymentMethod("CARD_B", 20, bd("100.00")));
        paymentMethodMap.put("CARD_C", new PaymentMethod("CARD_C", 5, bd("5.00")));
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 15, bd("100.00")));
        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("20.00"), discount);
    }

    @Test
    @DisplayName("Should correctly calculate discount when order has no promotions (null list)")
    void calculateMaxTheoreticalDiscount_orderHasNullPromotions() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 15, bd("100.00")));

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("15.00"), discount);
    }

    @Test
    @DisplayName("Should correctly calculate discount when order has empty promotions list")
    void calculateMaxTheoreticalDiscount_orderHasEmptyPromotions() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.emptyList());
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 15, bd("100.00")));

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("15.00"), discount);
    }

    @Test
    @DisplayName("Should ignore promotion if payment method for it does not exist in map")
    void calculateMaxTheoreticalDiscount_promoMethodNotInMap() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.singletonList("MISSING_CARD"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 15, bd("100.00")));

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("15.00"), discount);
    }

    @Test
    @DisplayName("Should return 0.00 discount if PUNKTY method does not exist and no card promotions")
    void calculateMaxTheoreticalDiscount_noPointsMethodAndNoCardPromotions() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.emptyList());
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 10, bd("100.00")));

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("0.00"), discount);
    }

    @Test
    @DisplayName("Should correctly handle PUNKTY in order promotions list (should be ignored for card promotions)")
    void calculateMaxTheoreticalDiscount_pointsInOrderPromotions() {
        Order order = new Order("ORDER1", bd("100.00"), Arrays.asList("CARD_A", POINTS_ID_STRING));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 10, bd("100.00")));
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 20, bd("100.00")));

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(bd("20.00"), discount);
    }


    static Stream<Arguments> roundingAndEdgeValueScenarios() {
        return Stream.of(
                Arguments.of(bd("99.99"), 15, bd("100.00"), 10, bd("100.00"), bd("15.00")),
                Arguments.of(bd("33.33"), 10, bd("30.00"), 5, bd("30.00"), bd("3.33")),
                Arguments.of(bd("66.67"), 8, bd("65.00"), 12, bd("60.00"), bd("8.00"))
        );
    }

    @ParameterizedTest
    @MethodSource("roundingAndEdgeValueScenarios")
    @DisplayName("Should correctly calculate max theoretical discount with various rounding scenarios")
    void calculateMaxTheoreticalDiscount_roundingTests(
            BigDecimal orderValue, int pointsDiscountPerc, BigDecimal pointsLimit,
            int cardDiscountPerc, BigDecimal cardLimit, BigDecimal expectedMaxDiscount) {

        Order order = new Order("TestOrder", orderValue, Collections.singletonList("PROMO_CARD"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, pointsDiscountPerc, pointsLimit));
        paymentMethodMap.put("PROMO_CARD", new PaymentMethod("PROMO_CARD", cardDiscountPerc, cardLimit));

        BigDecimal discount = paymentOptimizerService.calculateMaxTheoreticalDiscount(order, paymentMethodMap);
        assertEquals(expectedMaxDiscount, discount);
    }

    ////////////////////////////////////////////////


    @Test
    @DisplayName("[addFullCardPaymentOptions] Should not add options if order has null promotions")
    void addFullCardPaymentOptions_nullOrderPromotions() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 10, bd("100.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("100.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullCardPaymentOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertTrue(possibleOptions.isEmpty());
    }

    @Test
    @DisplayName("[addFullCardPaymentOptions] Should not add options if order has empty promotions list")
    void addFullCardPaymentOptions_emptyOrderPromotions() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.emptyList());
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 10, bd("100.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("100.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullCardPaymentOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertTrue(possibleOptions.isEmpty());
    }

    @Test
    @DisplayName("[addFullCardPaymentOptions] Should not add option if promotion ID from order is not in paymentMethodMap")
    void addFullCardPaymentOptions_promotionIdNotInMap() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.singletonList("MISSING_CARD"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullCardPaymentOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertTrue(possibleOptions.isEmpty());
    }

    @Test
    @DisplayName("[addFullCardPaymentOptions] Should ignore 'PUNKTY' if it's in order promotions")
    void addFullCardPaymentOptions_ignoresPointsInPromotions() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.singletonList(POINTS_ID_STRING));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 15, bd("100.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("100.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullCardPaymentOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertTrue(possibleOptions.isEmpty());
    }

    @Test
    @DisplayName("[addFullCardPaymentOptions] Should add option for a valid card promotion with sufficient limit")
    void addFullCardPaymentOptions_validCardSufficientLimit() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.singletonList("CARD_A"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 10, bd("100.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("90.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullCardPaymentOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(bd("10.00"), option.getCalculatedDiscountAmount());
        assertEquals(0, bd("0.00").compareTo(option.getPointsUsedAmount()));
        assertEquals(1, option.getAmountsToChargeByMethod().size());
        assertEquals(bd("90.00"), option.getAmountsToChargeByMethod().get("CARD_A"));
    }

    @Test
    @DisplayName("[addFullCardPaymentOptions] Should not add option for a valid card promotion with insufficient limit")
    void addFullCardPaymentOptions_validCardInsufficientLimit() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.singletonList("CARD_A"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 10, bd("100.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("89.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullCardPaymentOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertTrue(possibleOptions.isEmpty());
    }

    @Test
    @DisplayName("[addFullCardPaymentOptions] Should add options for multiple valid cards with sufficient limits")
    void addFullCardPaymentOptions_multipleValidCardsSufficientLimits() {
        Order order = new Order("ORDER1", bd("200.00"), Arrays.asList("CARD_A", "CARD_B"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 10, bd("200.00")));
        paymentMethodMap.put("CARD_B", new PaymentMethod("CARD_B", 20, bd("200.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("180.00"));
        remainingLimits.put("CARD_B", bd("160.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullCardPaymentOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(2, possibleOptions.size());

        PaymentOptimizerService.PaymentOption optionA = possibleOptions.stream()
                .filter(opt -> opt.getAmountsToChargeByMethod().containsKey("CARD_A"))
                .findFirst().orElse(null);
        assertNotNull(optionA);
        assertEquals(bd("20.00"), optionA.getCalculatedDiscountAmount());
        assertEquals(bd("180.00"), optionA.getAmountsToChargeByMethod().get("CARD_A"));

        PaymentOptimizerService.PaymentOption optionB = possibleOptions.stream()
                .filter(opt -> opt.getAmountsToChargeByMethod().containsKey("CARD_B"))
                .findFirst().orElse(null);
        assertNotNull(optionB);
        assertEquals(bd("40.00"), optionB.getCalculatedDiscountAmount());
        assertEquals(bd("160.00"), optionB.getAmountsToChargeByMethod().get("CARD_B"));
    }

    @Test
    @DisplayName("[addFullCardPaymentOptions] Should only add options for cards with sufficient limits from multiple promotions")
    void addFullCardPaymentOptions_mixedLimits() {
        Order order = new Order("ORDER1", bd("100.00"), Arrays.asList("CARD_A", "CARD_B", "CARD_C"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 10, bd("100.00")));
        paymentMethodMap.put("CARD_B", new PaymentMethod("CARD_B", 5, bd("100.00")));
        paymentMethodMap.put("CARD_C", new PaymentMethod("CARD_C", 20, bd("100.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("90.00"));
        remainingLimits.put("CARD_B", bd("94.00"));
        remainingLimits.put("CARD_C", bd("85.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullCardPaymentOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(2, possibleOptions.size());
        assertTrue(possibleOptions.stream().anyMatch(opt -> opt.getAmountsToChargeByMethod().containsKey("CARD_A")));
        assertTrue(possibleOptions.stream().anyMatch(opt -> opt.getAmountsToChargeByMethod().containsKey("CARD_C")));
        assertFalse(possibleOptions.stream().anyMatch(opt -> opt.getAmountsToChargeByMethod().containsKey("CARD_B")));
    }

    @Test
    @DisplayName("[addFullCardPaymentOptions] Should handle order value of 0.00 correctly")
    void addFullCardPaymentOptions_orderValueZero() {
        Order order = new Order("ORDER_ZERO", bd("0.00"), Collections.singletonList("CARD_A"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 10, bd("50.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("10.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullCardPaymentOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(bd("0.00"), option.getCalculatedDiscountAmount());
        assertEquals(0, bd("0.00").compareTo(option.getPointsUsedAmount()));
        assertEquals(bd("0.00"), option.getAmountsToChargeByMethod().get("CARD_A"));
    }

    @Test
    @DisplayName("[addFullCardPaymentOptions] Should correctly calculate discount and amount to pay with rounding")
    void addFullCardPaymentOptions_roundingChecks() {
        Order order = new Order("ORDER_ROUND", bd("99.99"), Collections.singletonList("CARD_ROUND"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_ROUND", new PaymentMethod("CARD_ROUND", 10, bd("100.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_ROUND", bd("90.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullCardPaymentOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(bd("10.00"), option.getCalculatedDiscountAmount());
        assertEquals(bd("89.99"), option.getAmountsToChargeByMethod().get("CARD_ROUND"));
    }

    @Test
    @DisplayName("[addFullCardPaymentOptions] Should use default limit of 0 if card not in remainingLimits")
    void addFullCardPaymentOptions_cardNotInRemainingLimits() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.singletonList("CARD_A"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 10, bd("100.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullCardPaymentOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertTrue(possibleOptions.isEmpty(), "Option should not be added if limit is effectively 0 and amount to pay > 0");

        Order orderZero = new Order("ORDER_ZERO", bd("0.00"), Collections.singletonList("CARD_A"));
        paymentOptimizerService.addFullCardPaymentOptions(orderZero, paymentMethodMap, remainingLimits, possibleOptions);
        assertEquals(1, possibleOptions.size(), "Option should be added for zero value order even with effective zero limit");
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(bd("0.00"), option.getAmountsToChargeByMethod().get("CARD_A"));
    }



    //////////////////////////////////////////

    @Test
    @DisplayName("[addFullPointsOptions] Should not add option if 'PUNKTY' method does not exist in map")
    void addFullPointsOptions_pointsMethodNotInMap() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullPointsOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertTrue(possibleOptions.isEmpty());
    }

    @Test
    @DisplayName("[addFullPointsOptions] Should add option if 'PUNKTY' method exists and limit is sufficient")
    void addFullPointsOptions_pointsExistSufficientLimit() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 15, bd("200.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("90.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullPointsOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(0, bd("15.00").compareTo(option.getCalculatedDiscountAmount()), "Calculated discount should be 15.00");
        assertEquals(0, bd("85.00").compareTo(option.getPointsUsedAmount()), "Points used amount should be 85.00");
        assertEquals(1, option.getAmountsToChargeByMethod().size());
        assertEquals(0, bd("85.00").compareTo(option.getAmountsToChargeByMethod().get(POINTS_ID_STRING)), "Amount charged to PUNKTY should be 85.00");
    }

    @Test
    @DisplayName("[addFullPointsOptions] Should not add option if 'PUNKTY' method exists but limit is insufficient")
    void addFullPointsOptions_pointsExistInsufficientLimit() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 15, bd("200.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("80.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullPointsOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertTrue(possibleOptions.isEmpty());
    }

    @Test
    @DisplayName("[addFullPointsOptions] Should handle order value of 0.00 correctly")
    void addFullPointsOptions_orderValueZero() {
        Order order = new Order("ORDER_ZERO", bd("0.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 20, bd("50.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("10.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullPointsOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(0, bd("0.00").compareTo(option.getCalculatedDiscountAmount()), "Calculated discount should be 0.00");
        assertEquals(0, bd("0.00").compareTo(option.getPointsUsedAmount()), "Points used amount should be 0.00");
        assertEquals(0, bd("0.00").compareTo(option.getAmountsToChargeByMethod().get(POINTS_ID_STRING)), "Amount charged to PUNKTY should be 0.00");
    }

    @Test
    @DisplayName("[addFullPointsOptions] Should correctly calculate discount and amount to pay with rounding")
    void addFullPointsOptions_roundingChecks() {
        Order order = new Order("ORDER_ROUND", bd("99.99"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 10, bd("100.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("90.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullPointsOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(0, bd("10.00").compareTo(option.getCalculatedDiscountAmount()), "Calculated discount should be 10.00");
        assertEquals(0, bd("89.99").compareTo(option.getPointsUsedAmount()), "Points used amount should be 89.99");
        assertEquals(0, bd("89.99").compareTo(option.getAmountsToChargeByMethod().get(POINTS_ID_STRING)), "Amount charged to PUNKTY should be 89.99");
    }

    @Test
    @DisplayName("[addFullPointsOptions] Should use default limit of 0 for 'PUNKTY' if not in remainingLimits")
    void addFullPointsOptions_pointsNotInRemainingLimits() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 15, bd("200.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullPointsOptions(order, paymentMethodMap, remainingLimits, possibleOptions);
        assertTrue(possibleOptions.isEmpty(), "Option should not be added if PUNKTY limit is effectively 0 and amount to pay > 0");

        Order orderZero = new Order("ORDER_ZERO", bd("0.00"), null);
        List<PaymentOptimizerService.PaymentOption> possibleOptionsZeroOrder = new ArrayList<>();
        paymentOptimizerService.addFullPointsOptions(orderZero, paymentMethodMap, remainingLimits, possibleOptionsZeroOrder);

        assertEquals(1, possibleOptionsZeroOrder.size(), "Option should be added for zero value order even with effective zero PUNKTY limit");
        PaymentOptimizerService.PaymentOption option = possibleOptionsZeroOrder.get(0);
        assertEquals(0, bd("0.00").compareTo(option.getAmountsToChargeByMethod().get(POINTS_ID_STRING)), "Amount charged to PUNKTY should be 0.00");
    }

    @Test
    @DisplayName("[addFullPointsOptions] Should correctly handle 0% discount for PUNKTY")
    void addFullPointsOptions_zeroPercentPointsDiscount() {
        Order order = new Order("ORDER1", bd("120.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 0, bd("150.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("130.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addFullPointsOptions(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(0, bd("0.00").compareTo(option.getCalculatedDiscountAmount()), "Calculated discount should be 0.00 for 0% points discount");
        assertEquals(0, bd("120.00").compareTo(option.getPointsUsedAmount()), "Points used amount should be 120.00");
        assertEquals(0, bd("120.00").compareTo(option.getAmountsToChargeByMethod().get(POINTS_ID_STRING)), "Amount charged to PUNKTY should be 120.00");
    }


    //////////////////////////////////////

    @Test
    @DisplayName("[addPartialPointsOption] Should not add option if 'PUNKTY' method does not exist")
    void addPartialPointsOption_pointsMethodNotInMap() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addPartialPointsOption(order, paymentMethodMap, remainingLimits, possibleOptions);
        assertTrue(possibleOptions.isEmpty());
    }

    @Test
    @DisplayName("[addPartialPointsOption] Should not add option if available points are less than 10% of order value")
    void addPartialPointsOption_notEnoughPointsForMinRequirement() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 0, bd("50.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("9.99"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addPartialPointsOption(order, paymentMethodMap, remainingLimits, possibleOptions);
        assertTrue(possibleOptions.isEmpty());
    }

    @Test
    @DisplayName("[addPartialPointsOption] Should add option with only points if points cover full amount after 10% discount")
    void addPartialPointsOption_pointsCoverAllAfterDiscount() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 0, bd("200.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("100.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addPartialPointsOption(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(0, bd("10.00").compareTo(option.getCalculatedDiscountAmount()), "Discount should be 10% of order value");
        assertEquals(0, bd("90.00").compareTo(option.getPointsUsedAmount()), "Points used should cover the discounted amount");
        assertEquals(1, option.getAmountsToChargeByMethod().size());
        assertEquals(0, bd("90.00").compareTo(option.getAmountsToChargeByMethod().get(POINTS_ID_STRING)), "Charge should be on PUNKTY");
        assertNull(option.getAmountsToChargeByMethod().get("ANY_CARD"), "No card should be charged");
    }

    @Test
    @DisplayName("[addPartialPointsOption] Should add option with points and preferred card (no promo, lowest limit)")
    void addPartialPointsOption_pointsAndPreferredCardNoPromo() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.singletonList("PROMO_CARD_HIGH_LIMIT"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 0, bd("100.00")));
        paymentMethodMap.put("CARD_NO_PROMO_LOW_LIMIT", new PaymentMethod("CARD_NO_PROMO_LOW_LIMIT", 0, bd("40.00")));
        paymentMethodMap.put("CARD_NO_PROMO_HIGH_LIMIT", new PaymentMethod("CARD_NO_PROMO_HIGH_LIMIT", 0, bd("100.00")));
        paymentMethodMap.put("PROMO_CARD_HIGH_LIMIT", new PaymentMethod("PROMO_CARD_HIGH_LIMIT", 5, bd("100.00")));


        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("50.00"));
        remainingLimits.put("CARD_NO_PROMO_LOW_LIMIT", bd("40.00"));
        remainingLimits.put("CARD_NO_PROMO_HIGH_LIMIT", bd("100.00"));
        remainingLimits.put("PROMO_CARD_HIGH_LIMIT", bd("100.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addPartialPointsOption(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(0, bd("10.00").compareTo(option.getCalculatedDiscountAmount()));
        assertEquals(0, bd("50.00").compareTo(option.getPointsUsedAmount()));
        assertEquals(2, option.getAmountsToChargeByMethod().size());
        assertEquals(0, bd("50.00").compareTo(option.getAmountsToChargeByMethod().get(POINTS_ID_STRING)));
        assertEquals(0, bd("40.00").compareTo(option.getAmountsToChargeByMethod().get("CARD_NO_PROMO_LOW_LIMIT")), "Should pick card with no promo and lowest sufficient limit");
    }

    @Test
    @DisplayName("[addPartialPointsOption] Should add option with points and preferred card (promo, lowest discount then limit)")
    void addPartialPointsOption_pointsAndPreferredCardWithPromo() {
        Order order = new Order("ORDER1", bd("100.00"), Arrays.asList("CARD_PROMO1", "CARD_PROMO2", "CARD_PROMO3"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 0, bd("100.00")));
        paymentMethodMap.put("CARD_PROMO1", new PaymentMethod("CARD_PROMO1", 5, bd("60.00")));
        paymentMethodMap.put("CARD_PROMO2", new PaymentMethod("CARD_PROMO2", 10, bd("70.00")));
        paymentMethodMap.put("CARD_PROMO3", new PaymentMethod("CARD_PROMO3", 5, bd("80.00")));

        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("30.00"));
        remainingLimits.put("CARD_PROMO1", bd("60.00"));
        remainingLimits.put("CARD_PROMO2", bd("70.00"));
        remainingLimits.put("CARD_PROMO3", bd("80.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addPartialPointsOption(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(0, bd("10.00").compareTo(option.getCalculatedDiscountAmount()));
        assertEquals(0, bd("30.00").compareTo(option.getPointsUsedAmount()));
        assertEquals(2, option.getAmountsToChargeByMethod().size());
        assertEquals(0, bd("30.00").compareTo(option.getAmountsToChargeByMethod().get(POINTS_ID_STRING)));
        assertEquals(0, bd("60.00").compareTo(option.getAmountsToChargeByMethod().get("CARD_PROMO1")), "Should pick promo card with lowest discount, then lowest sufficient limit");
    }

    @Test
    @DisplayName("[addPartialPointsOption] Should not add option if no card has sufficient limit for remainder")
    void addPartialPointsOption_noCardSufficientForRemainder() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 0, bd("100.00")));
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 0, bd("60.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("20.00"));
        remainingLimits.put("CARD_A", bd("60.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addPartialPointsOption(order, paymentMethodMap, remainingLimits, possibleOptions);
        assertTrue(possibleOptions.isEmpty());
    }

    @Test
    @DisplayName("[addPartialPointsOption] Should correctly handle order value of 0.00")
    void addPartialPointsOption_orderValueZero() {
        Order order = new Order("ORDER_ZERO", bd("0.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 0, bd("50.00")));
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 0, bd("10.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("10.00"));
        remainingLimits.put("CARD_A", bd("10.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addPartialPointsOption(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(0, bd("0.00").compareTo(option.getCalculatedDiscountAmount()));
        assertEquals(0, bd("0.00").compareTo(option.getPointsUsedAmount()));
        assertEquals(1, option.getAmountsToChargeByMethod().size());
        assertEquals(0, bd("0.00").compareTo(option.getAmountsToChargeByMethod().get(POINTS_ID_STRING)));
    }

    @Test
    @DisplayName("[addPartialPointsOption] Should spend all available points if less than discounted order value, then pay rest by card")
    void addPartialPointsOption_spendAllAvailablePointsLessThanDiscountedValue() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 0, bd("100.00")));
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 0, bd("50.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("40.00"));
        remainingLimits.put("CARD_A", bd("50.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addPartialPointsOption(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(0, bd("10.00").compareTo(option.getCalculatedDiscountAmount()));
        assertEquals(0, bd("40.00").compareTo(option.getPointsUsedAmount()));
        assertEquals(2, option.getAmountsToChargeByMethod().size());
        assertEquals(0, bd("40.00").compareTo(option.getAmountsToChargeByMethod().get(POINTS_ID_STRING)));
        assertEquals(0, bd("50.00").compareTo(option.getAmountsToChargeByMethod().get("CARD_A")));
    }

    @Test
    @DisplayName("[addPartialPointsOption] Should handle rounding for minPointsRequired correctly")
    void addPartialPointsOption_roundingForMinPointsRequired() {
        Order order = new Order("ORDER_ROUND", bd("99.95"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 0, bd("100.00")));
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 0, bd("100.00")));

        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("100.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        remainingLimits.put(POINTS_ID_STRING, bd("9.99"));
        paymentOptimizerService.addPartialPointsOption(order, paymentMethodMap, remainingLimits, possibleOptions);
        assertTrue(possibleOptions.isEmpty(), "Should not add option if points are less than rounded min required");

        possibleOptions.clear();
        remainingLimits.put(POINTS_ID_STRING, bd("10.00"));
        paymentOptimizerService.addPartialPointsOption(order, paymentMethodMap, remainingLimits, possibleOptions);
        assertEquals(1, possibleOptions.size(), "Should add option if points meet rounded min required");
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(0, bd("10.00").compareTo(option.getCalculatedDiscountAmount()));
        assertEquals(0, bd("10.00").compareTo(option.getPointsUsedAmount()));
        assertEquals(0, bd("79.95").compareTo(option.getAmountsToChargeByMethod().get("CARD_A")));
    }

    @Test
    @DisplayName("[addPartialPointsOption] Card selection prefers non-promo card even if promo card has lower discount/limit")
    void addPartialPointsOption_cardSelectionPrefersNonPromo() {
        Order order = new Order("ORDER1", bd("100.00"), Collections.singletonList("PROMO_LOW_DISCOUNT_LOW_LIMIT"));
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 0, bd("100.00")));
        paymentMethodMap.put("NON_PROMO_HIGHER_LIMIT", new PaymentMethod("NON_PROMO_HIGHER_LIMIT", 0, bd("80.00")));
        paymentMethodMap.put("PROMO_LOW_DISCOUNT_LOW_LIMIT", new PaymentMethod("PROMO_LOW_DISCOUNT_LOW_LIMIT", 2, bd("70.00")));

        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("20.00"));
        remainingLimits.put("NON_PROMO_HIGHER_LIMIT", bd("80.00"));
        remainingLimits.put("PROMO_LOW_DISCOUNT_LOW_LIMIT", bd("70.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addPartialPointsOption(order, paymentMethodMap, remainingLimits, possibleOptions);
        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertTrue(option.getAmountsToChargeByMethod().containsKey("NON_PROMO_HIGHER_LIMIT"), "Should select non-promo card");
        assertEquals(0, bd("70.00").compareTo(option.getAmountsToChargeByMethod().get("NON_PROMO_HIGHER_LIMIT")));
    }


    /////////////////////////////////////

    @Test
    @DisplayName("[addCardPaymentWithoutPromotionOption] Should not add options if no cards (only PUNKTY) in paymentMethodMap")
    void addCardPaymentWithoutPromotionOption_noCardsOnlyPoints() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 10, bd("100.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("100.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addCardPaymentWithoutPromotionOption(order, paymentMethodMap, remainingLimits, possibleOptions);
        assertTrue(possibleOptions.isEmpty());
    }

    @Test
    @DisplayName("[addCardPaymentWithoutPromotionOption] Should not add options if paymentMethodMap is empty")
    void addCardPaymentWithoutPromotionOption_emptyPaymentMethodMap() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = Collections.emptyMap();
        Map<String, BigDecimal> remainingLimits = Collections.emptyMap();
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addCardPaymentWithoutPromotionOption(order, paymentMethodMap, remainingLimits, possibleOptions);
        assertTrue(possibleOptions.isEmpty());
    }

    @Test
    @DisplayName("[addCardPaymentWithoutPromotionOption] Should add option for one card with sufficient limit")
    void addCardPaymentWithoutPromotionOption_oneCardSufficientLimit() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 0, bd("150.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("120.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addCardPaymentWithoutPromotionOption(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(0, BigDecimal.ZERO.compareTo(option.getCalculatedDiscountAmount()), "Discount should be zero");
        assertEquals(0, BigDecimal.ZERO.compareTo(option.getPointsUsedAmount()), "Points used should be zero");
        assertEquals(1, option.getAmountsToChargeByMethod().size());
        assertEquals(0, bd("100.00").compareTo(option.getAmountsToChargeByMethod().get("CARD_A")), "Amount charged to CARD_A should be order value");
    }

    @Test
    @DisplayName("[addCardPaymentWithoutPromotionOption] Should not add option for card with insufficient limit")
    void addCardPaymentWithoutPromotionOption_oneCardInsufficientLimit() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 0, bd("80.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("80.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addCardPaymentWithoutPromotionOption(order, paymentMethodMap, remainingLimits, possibleOptions);
        assertTrue(possibleOptions.isEmpty());
    }

    @Test
    @DisplayName("[addCardPaymentWithoutPromotionOption] Should add options for multiple cards with sufficient limits")
    void addCardPaymentWithoutPromotionOption_multipleCardsSufficientLimits() {
        Order order = new Order("ORDER1", bd("50.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 0, bd("60.00")));
        paymentMethodMap.put("CARD_B", new PaymentMethod("CARD_B", 0, bd("70.00")));
        paymentMethodMap.put(POINTS_ID_STRING, new PaymentMethod(POINTS_ID_STRING, 10, bd("10.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("60.00"));
        remainingLimits.put("CARD_B", bd("70.00"));
        remainingLimits.put(POINTS_ID_STRING, bd("10.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addCardPaymentWithoutPromotionOption(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(2, possibleOptions.size());
        assertTrue(possibleOptions.stream().anyMatch(opt -> opt.getAmountsToChargeByMethod().containsKey("CARD_A") && bd("50.00").compareTo(opt.getAmountsToChargeByMethod().get("CARD_A")) == 0));
        assertTrue(possibleOptions.stream().anyMatch(opt -> opt.getAmountsToChargeByMethod().containsKey("CARD_B") && bd("50.00").compareTo(opt.getAmountsToChargeByMethod().get("CARD_B")) == 0));

        for (PaymentOptimizerService.PaymentOption option : possibleOptions) {
            assertEquals(0, BigDecimal.ZERO.compareTo(option.getCalculatedDiscountAmount()));
            assertEquals(0, BigDecimal.ZERO.compareTo(option.getPointsUsedAmount()));
        }
    }

    @Test
    @DisplayName("[addCardPaymentWithoutPromotionOption] Should add options only for cards with sufficient limits")
    void addCardPaymentWithoutPromotionOption_mixedLimits() {
        Order order = new Order("ORDER1", bd("75.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 0, bd("80.00")));
        paymentMethodMap.put("CARD_B", new PaymentMethod("CARD_B", 0, bd("70.00")));
        paymentMethodMap.put("CARD_C", new PaymentMethod("CARD_C", 0, bd("100.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("80.00"));
        remainingLimits.put("CARD_B", bd("70.00"));
        remainingLimits.put("CARD_C", bd("100.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addCardPaymentWithoutPromotionOption(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(2, possibleOptions.size());
        assertTrue(possibleOptions.stream().anyMatch(opt -> opt.getAmountsToChargeByMethod().containsKey("CARD_A")));
        assertTrue(possibleOptions.stream().anyMatch(opt -> opt.getAmountsToChargeByMethod().containsKey("CARD_C")));
        assertFalse(possibleOptions.stream().anyMatch(opt -> opt.getAmountsToChargeByMethod().containsKey("CARD_B")));
    }

    @Test
    @DisplayName("[addCardPaymentWithoutPromotionOption] Should handle order value of 0.00 correctly")
    void addCardPaymentWithoutPromotionOption_orderValueZero() {
        Order order = new Order("ORDER_ZERO", bd("0.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 0, bd("10.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("5.00"));
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addCardPaymentWithoutPromotionOption(order, paymentMethodMap, remainingLimits, possibleOptions);

        assertEquals(1, possibleOptions.size());
        PaymentOptimizerService.PaymentOption option = possibleOptions.get(0);
        assertEquals(0, BigDecimal.ZERO.compareTo(option.getCalculatedDiscountAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(option.getPointsUsedAmount()));
        assertEquals(0, bd("0.00").compareTo(option.getAmountsToChargeByMethod().get("CARD_A")));
    }

    @Test
    @DisplayName("[addCardPaymentWithoutPromotionOption] Should use default limit of 0 if card not in remainingLimits")
    void addCardPaymentWithoutPromotionOption_cardNotInRemainingLimits() {
        Order order = new Order("ORDER1", bd("100.00"), null);
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        paymentMethodMap.put("CARD_A", new PaymentMethod("CARD_A", 0, bd("150.00")));
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        List<PaymentOptimizerService.PaymentOption> possibleOptions = new ArrayList<>();

        paymentOptimizerService.addCardPaymentWithoutPromotionOption(order, paymentMethodMap, remainingLimits, possibleOptions);
        assertTrue(possibleOptions.isEmpty(), "Option should not be added if limit is effectively 0 and order value > 0");

        Order orderZero = new Order("ORDER_ZERO", bd("0.00"), null);
        List<PaymentOptimizerService.PaymentOption> zeroOptions = new ArrayList<>();
        paymentOptimizerService.addCardPaymentWithoutPromotionOption(orderZero, paymentMethodMap, remainingLimits, zeroOptions);
        assertEquals(1, zeroOptions.size(), "Option should be added for zero value order even with effective zero limit for the card");
        PaymentOptimizerService.PaymentOption option = zeroOptions.get(0);
        assertEquals(0, bd("0.00").compareTo(option.getAmountsToChargeByMethod().get("CARD_A")));
    }



    /////////////////////////



    //pomocnicza metoda to testow dla applyPaymentOption metody
    private PaymentOptimizerService.PaymentOption createPaymentOption(Map<String, BigDecimal> amountsToCharge) {
        return new PaymentOptimizerService.PaymentOption(BigDecimal.ZERO, BigDecimal.ZERO, amountsToCharge);
    }

    @Test
    @DisplayName("[applyPaymentOption] Should correctly update limits and total spent for single card payment")
    void applyPaymentOption_singleCardPayment() {
        Map<String, BigDecimal> amountsToCharge = new HashMap<>();
        amountsToCharge.put("CARD_A", bd("50.00"));
        PaymentOptimizerService.PaymentOption option = createPaymentOption(amountsToCharge);

        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("100.00"));
        remainingLimits.put("PUNKTY", bd("200.00"));

        Map<String, BigDecimal> totalSpentByMethod = new HashMap<>();

        paymentOptimizerService.applyPaymentOption(option, remainingLimits, totalSpentByMethod);

        assertEquals(0, bd("50.00").compareTo(remainingLimits.get("CARD_A")), "CARD_A limit should be reduced by 50.00");
        assertEquals(0, bd("200.00").compareTo(remainingLimits.get("PUNKTY")), "PUNKTY limit should remain unchanged");
        assertEquals(0, bd("50.00").compareTo(totalSpentByMethod.get("CARD_A")), "Total spent for CARD_A should be 50.00");
        assertNull(totalSpentByMethod.get("PUNKTY"), "Total spent for PUNKTY should be null as it was not used");
    }

    @Test
    @DisplayName("[applyPaymentOption] Should correctly update limits and total spent for single points payment")
    void applyPaymentOption_singlePointsPayment() {
        Map<String, BigDecimal> amountsToCharge = new HashMap<>();
        amountsToCharge.put(POINTS_ID_STRING, bd("75.00"));
        PaymentOptimizerService.PaymentOption option = createPaymentOption(amountsToCharge);

        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("100.00"));
        remainingLimits.put("CARD_A", bd("50.00"));
        Map<String, BigDecimal> totalSpentByMethod = new HashMap<>();

        paymentOptimizerService.applyPaymentOption(option, remainingLimits, totalSpentByMethod);

        assertEquals(0, bd("25.00").compareTo(remainingLimits.get(POINTS_ID_STRING)), "PUNKTY limit should be reduced by 75.00");
        assertEquals(0, bd("50.00").compareTo(remainingLimits.get("CARD_A")), "CARD_A limit should remain unchanged");
        assertEquals(0, bd("75.00").compareTo(totalSpentByMethod.get(POINTS_ID_STRING)), "Total spent for PUNKTY should be 75.00");
        assertNull(totalSpentByMethod.get("CARD_A"));
    }

    @Test
    @DisplayName("[applyPaymentOption] Should correctly update limits and total spent for mixed payment (points and card)")
    void applyPaymentOption_mixedPayment() {
        Map<String, BigDecimal> amountsToCharge = new HashMap<>();
        amountsToCharge.put(POINTS_ID_STRING, bd("30.00"));
        amountsToCharge.put("CARD_B", bd("70.00"));
        PaymentOptimizerService.PaymentOption option = createPaymentOption(amountsToCharge);

        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put(POINTS_ID_STRING, bd("50.00"));
        remainingLimits.put("CARD_B", bd("100.00"));
        remainingLimits.put("CARD_A", bd("20.00"));

        Map<String, BigDecimal> totalSpentByMethod = new HashMap<>();
        totalSpentByMethod.put("CARD_B", bd("10.00"));

        paymentOptimizerService.applyPaymentOption(option, remainingLimits, totalSpentByMethod);

        assertEquals(0, bd("20.00").compareTo(remainingLimits.get(POINTS_ID_STRING)), "PUNKTY limit should be reduced");
        assertEquals(0, bd("30.00").compareTo(remainingLimits.get("CARD_B")), "CARD_B limit should be reduced");
        assertEquals(0, bd("20.00").compareTo(remainingLimits.get("CARD_A")), "CARD_A limit should remain unchanged");

        assertEquals(0, bd("30.00").compareTo(totalSpentByMethod.get(POINTS_ID_STRING)), "Total spent for PUNKTY should be 30.00");
        assertEquals(0, bd("80.00").compareTo(totalSpentByMethod.get("CARD_B")), "Total spent for CARD_B should be 10.00 + 70.00 = 80.00");
        assertNull(totalSpentByMethod.get("CARD_A"));
    }

    @Test
    @DisplayName("[applyPaymentOption] Should do nothing if PaymentOption has empty amountsToChargeByMethod")
    void applyPaymentOption_emptyAmountsToCharge() {
        PaymentOptimizerService.PaymentOption option = createPaymentOption(Collections.emptyMap());

        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("100.00"));
        Map<String, BigDecimal> initialLimitsCopy = new HashMap<>(remainingLimits);

        Map<String, BigDecimal> totalSpentByMethod = new HashMap<>();
        Map<String, BigDecimal> initialTotalSpentCopy = new HashMap<>(totalSpentByMethod);

        paymentOptimizerService.applyPaymentOption(option, remainingLimits, totalSpentByMethod);

        assertEquals(initialLimitsCopy, remainingLimits, "remainingLimits should not change for empty option");
        assertEquals(initialTotalSpentCopy, totalSpentByMethod, "totalSpentByMethod should not change for empty option");
    }

    @Test
    @DisplayName("[applyPaymentOption] Should handle zero amount charge for a method correctly")
    void applyPaymentOption_zeroAmountCharge() {
        Map<String, BigDecimal> amountsToCharge = new HashMap<>();
        amountsToCharge.put("CARD_A", bd("0.00"));
        PaymentOptimizerService.PaymentOption option = createPaymentOption(amountsToCharge);

        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("100.00"));
        Map<String, BigDecimal> totalSpentByMethod = new HashMap<>();

        paymentOptimizerService.applyPaymentOption(option, remainingLimits, totalSpentByMethod);

        assertEquals(0, bd("100.00").compareTo(remainingLimits.get("CARD_A")), "CARD_A limit should not change for zero amount");
        assertEquals(0, bd("0.00").compareTo(totalSpentByMethod.get("CARD_A")), "Total spent for CARD_A should be 0.00");
    }

    @Test
    @DisplayName("[applyPaymentOption] Should throw IllegalStateException if method in option not in remainingLimits")
    void applyPaymentOption_methodNotInRemainingLimits_throwsException() {
        Map<String, BigDecimal> amountsToCharge = new HashMap<>();
        amountsToCharge.put("MISSING_CARD", bd("50.00"));
        PaymentOptimizerService.PaymentOption option = createPaymentOption(amountsToCharge);

        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        Map<String, BigDecimal> totalSpentByMethod = new HashMap<>();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            paymentOptimizerService.applyPaymentOption(option, remainingLimits, totalSpentByMethod);
        });
        assertEquals("Error while applying payment limit", exception.getMessage());
    }

    @Test
    @DisplayName("[applyPaymentOption] Should throw IllegalStateException if remaining limit is less than amount to charge")
    void applyPaymentOption_insufficientLimit_throwsException() {
        Map<String, BigDecimal> amountsToCharge = new HashMap<>();
        amountsToCharge.put("CARD_A", bd("100.00"));
        PaymentOptimizerService.PaymentOption option = createPaymentOption(amountsToCharge);

        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("50.00"));
        Map<String, BigDecimal> totalSpentByMethod = new HashMap<>();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            paymentOptimizerService.applyPaymentOption(option, remainingLimits, totalSpentByMethod);
        });
        assertEquals("Error while applying payment limit", exception.getMessage());
    }

    @Test
    @DisplayName("[applyPaymentOption] Should correctly accumulate total spent for multiple applications on same method")
    void applyPaymentOption_multipleApplicationsSameMethod() {
        Map<String, BigDecimal> amounts1 = new HashMap<>();
        amounts1.put("CARD_A", bd("30.00"));
        PaymentOptimizerService.PaymentOption option1 = createPaymentOption(amounts1);

        Map<String, BigDecimal> amounts2 = new HashMap<>();
        amounts2.put("CARD_A", bd("40.00"));
        PaymentOptimizerService.PaymentOption option2 = createPaymentOption(amounts2);

        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("CARD_A", bd("100.00"));
        Map<String, BigDecimal> totalSpentByMethod = new HashMap<>();

        paymentOptimizerService.applyPaymentOption(option1, remainingLimits, totalSpentByMethod);
        assertEquals(0, bd("70.00").compareTo(remainingLimits.get("CARD_A")), "Limit after first application");
        assertEquals(0, bd("30.00").compareTo(totalSpentByMethod.get("CARD_A")), "Total spent after first application");

        paymentOptimizerService.applyPaymentOption(option2, remainingLimits, totalSpentByMethod);
        assertEquals(0, bd("30.00").compareTo(remainingLimits.get("CARD_A")), "Limit after second application");
        assertEquals(0, bd("70.00").compareTo(totalSpentByMethod.get("CARD_A")), "Total spent should accumulate");
    }

    @Test
    @DisplayName("[applyPaymentOption] Should correctly initialize totalSpent for a new method")
    void applyPaymentOption_initializeTotalSpentForNewMethod() {
        Map<String, BigDecimal> amountsToCharge = new HashMap<>();
        amountsToCharge.put("NEW_CARD", bd("25.00"));
        PaymentOptimizerService.PaymentOption option = createPaymentOption(amountsToCharge);

        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        remainingLimits.put("NEW_CARD", bd("50.00"));
        Map<String, BigDecimal> totalSpentByMethod = new HashMap<>();

        paymentOptimizerService.applyPaymentOption(option, remainingLimits, totalSpentByMethod);

        assertEquals(0, bd("25.00").compareTo(remainingLimits.get("NEW_CARD")));
        assertNotNull(totalSpentByMethod.get("NEW_CARD"), "Total spent should have an entry for NEW_CARD");
        assertEquals(0, bd("25.00").compareTo(totalSpentByMethod.get("NEW_CARD")), "Total spent for NEW_CARD should be 25.00");
    }




    //////////////////////////////////


    @Test
    @DisplayName("[optimizePayments] Should return empty map for empty orders list")
    void optimizePayments_emptyOrdersList() {
        List<Order> orders = Collections.emptyList();
        List<PaymentMethod> paymentMethods = Arrays.asList(
                new PaymentMethod("CARD_A", 10, bd("100.00")),
                new PaymentMethod(POINTS_ID_STRING, 15, bd("50.00"))
        );

        Map<String, BigDecimal> result = paymentOptimizerService.optimizePayments(orders, paymentMethods);
        assertTrue(result.isEmpty(), "Result map should be empty for no orders");
    }

    @Test
    @DisplayName("[optimizePayments] Should throw RuntimeException if no payment methods provided for non-empty orders")
    void optimizePayments_emptyPaymentMethods_throwsExceptionForNonEmptyOrders() {
        List<Order> orders = Collections.singletonList(new Order("ORDER1", bd("10.00"), null));
        List<PaymentMethod> paymentMethods = Collections.emptyList();

        Exception exception = assertThrows(RuntimeException.class, () -> {
            paymentOptimizerService.optimizePayments(orders, paymentMethods);
        });
        assertTrue(exception.getMessage().contains("No possible payment option found for order ORDER1"));
    }

    @Test
    @DisplayName("[optimizePayments] Single order, partial points option is chosen over promo card due to points preference with equal discount")
    void optimizePayments_singleOrder_fullPromoCardPayment() {
        Order order1 = new Order("ORDER1", bd("100.00"), Collections.singletonList("PROMO_CARD"));
        List<Order> orders = Collections.singletonList(order1);

        List<PaymentMethod> paymentMethods = Arrays.asList(
                new PaymentMethod("PROMO_CARD", 10, bd("100.00")),
                new PaymentMethod(POINTS_ID_STRING, 5, bd("100.00"))
        );

        Map<String, BigDecimal> result = paymentOptimizerService.optimizePayments(orders, paymentMethods);

        assertEquals(1, result.size(), "Result map should contain one entry");
        assertNotNull(result.get(POINTS_ID_STRING), "PUNKTY should be the payment method");
        assertEquals(0, bd("90.00").compareTo(result.get(POINTS_ID_STRING)), "Should pay 90.00 with PUNKTY");
        assertNull(result.get("PROMO_CARD"), "PROMO_CARD should not be used");
    }

    @Test
    @DisplayName("[optimizePayments] Single order, full payment by points is best option")
    void optimizePayments_singleOrder_fullPointsPaymentBest() {
        Order order1 = new Order("ORDER1", bd("100.00"), Collections.singletonList("PROMO_CARD"));
        List<Order> orders = Collections.singletonList(order1);

        List<PaymentMethod> paymentMethods = Arrays.asList(
                new PaymentMethod("PROMO_CARD", 10, bd("100.00")),
                new PaymentMethod(POINTS_ID_STRING, 15, bd("100.00"))
        );

        Map<String, BigDecimal> result = paymentOptimizerService.optimizePayments(orders, paymentMethods);

        assertEquals(1, result.size());
        assertEquals(0, bd("85.00").compareTo(result.get(POINTS_ID_STRING)), "Should pay 85.00 with PUNKTY");
        assertNull(result.get("PROMO_CARD"));
    }

    @Test
    @DisplayName("[optimizePayments] Single order, partial points payment (effectively full points after 10% disc) is best option")
    void optimizePayments_singleOrder_partialPointsPaymentBest() {
        Order order1 = new Order("ORDER1", bd("100.00"), null);
        List<Order> orders = Collections.singletonList(order1);

        List<PaymentMethod> paymentMethods = Arrays.asList(
                new PaymentMethod("CARD_A", 0, bd("100.00")),
                new PaymentMethod(POINTS_ID_STRING, 5, bd("100.00"))
        );

        Map<String, BigDecimal> result = paymentOptimizerService.optimizePayments(orders, paymentMethods);

        assertEquals(1, result.size(), "Result map should contain one entry for PUNKTY");
        assertNotNull(result.get(POINTS_ID_STRING), "PUNKTY should be the payment method used");
        assertEquals(0, bd("90.00").compareTo(result.get(POINTS_ID_STRING)), "Should use 90.00 PUNKTY");
        assertNull(result.get("CARD_A"), "CARD_A should not be used");
    }

    @Test
    @DisplayName("[optimizePayments] Single order, card payment without promotion is only option")
    void optimizePayments_singleOrder_cardWithoutPromoOnly() {
        Order order1 = new Order("ORDER1", bd("100.00"), null);
        List<Order> orders = Collections.singletonList(order1);

        List<PaymentMethod> paymentMethods = Arrays.asList(
                new PaymentMethod("CARD_A", 0, bd("100.00")),
                new PaymentMethod(POINTS_ID_STRING, 10, bd("5.00"))
        );

        Map<String, BigDecimal> result = paymentOptimizerService.optimizePayments(orders, paymentMethods);

        assertEquals(1, result.size());
        assertEquals(0, bd("100.00").compareTo(result.get("CARD_A")), "Should pay 100.00 with CARD_A");
        assertNull(result.get(POINTS_ID_STRING));
    }

    @Test
    @DisplayName("[optimizePayments] Example from PDF - actual logic trace")
    void optimizePayments_exampleFromPdf_actualLogicTrace() {
        Order order1 = new Order("ORDER1", bd("100.00"), Collections.singletonList("mZysk"));
        Order order2 = new Order("ORDER2", bd("200.00"), Collections.singletonList("BosBankrut"));
        Order order3 = new Order("ORDER3", bd("150.00"), Arrays.asList("mZysk", "BosBankrut"));
        Order order4 = new Order("ORDER4", bd("50.00"), null);
        List<Order> orders = Arrays.asList(order1, order2, order3, order4);


        List<PaymentMethod> paymentMethods = Arrays.asList(
                new PaymentMethod(POINTS_ID_STRING, 15, bd("100.00")),
                new PaymentMethod("mZysk", 10, bd("180.00")),
                new PaymentMethod("BosBankrut", 5, bd("200.00"))
        );

        Map<String, BigDecimal> result = paymentOptimizerService.optimizePayments(orders, paymentMethods);

        assertNotNull(result.get("mZysk"), "mZysk should be in result");
        assertEquals(0, bd("170.00").compareTo(result.get("mZysk")), "mZysk total spent incorrect");

        assertNotNull(result.get("BosBankrut"), "BosBankrut should be in result");
        assertEquals(0, bd("192.50").compareTo(result.get("BosBankrut")), "BosBankrut total spent incorrect");

        assertNotNull(result.get(POINTS_ID_STRING), "PUNKTY should be in result");
        assertEquals(0, bd("100.00").compareTo(result.get(POINTS_ID_STRING)), "PUNKTY total spent incorrect");

        assertEquals(3, result.size(), "Result map should contain 3 entries");
    }


    @Test
    @DisplayName("[optimizePayments] Should throw RuntimeException if an order cannot be paid")
    void optimizePayments_cannotPayOrder_throwsException() {
        Order order1 = new Order("ORDER1", bd("100.00"), null);
        List<Order> orders = Collections.singletonList(order1);
        List<PaymentMethod> paymentMethods = Collections.singletonList(
                new PaymentMethod("CARD_A", 0, bd("10.00"))
        );

        Exception exception = assertThrows(RuntimeException.class, () -> {
            paymentOptimizerService.optimizePayments(orders, paymentMethods);
        });
        assertTrue(exception.getMessage().contains("No possible payment option found for order ORDER1"));
    }

    @Test
    @DisplayName("[optimizePayments] Order with value 0.00 should be processed without spending")
    void optimizePayments_zeroValueOrder() {
        Order order1 = new Order("ORDER_ZERO", bd("0.00"), null);
        Order order2 = new Order("ORDER_NORMAL", bd("50.00"), null);
        List<Order> orders = Arrays.asList(order1, order2);

        List<PaymentMethod> paymentMethods = Arrays.asList(
                new PaymentMethod("CARD_A", 0, bd("50.00")),
                new PaymentMethod(POINTS_ID_STRING, 10, bd("10.00"))
        );

        Map<String, BigDecimal> result = paymentOptimizerService.optimizePayments(orders, paymentMethods);

        assertNotNull(result.get(POINTS_ID_STRING), "PUNKTY should be in the result map");
        assertEquals(0, bd("10.00").compareTo(result.get(POINTS_ID_STRING)), "Spent PUNKTY should be 10.00");
        assertNotNull(result.get("CARD_A"), "CARD_A should be in the result map");
        assertEquals(0, bd("35.00").compareTo(result.get("CARD_A")), "Spent CARD_A should be 35.00");
        assertEquals(2, result.size(), "Result map should contain exactly two entries");
    }

    @Test
    @DisplayName("[optimizePayments] Prioritize discount then points usage when choosing best option")
    void optimizePayments_prioritizeDiscountThenPoints() {
        Order order1 = new Order("ORDER1", bd("100.00"), Collections.singletonList("PROMO_CARD_C"));
        List<Order> orders = Collections.singletonList(order1);

        List<PaymentMethod> paymentMethods = Arrays.asList(
                new PaymentMethod(POINTS_ID_STRING, 0, bd("50.00")),
                new PaymentMethod("CARD_X", 0, bd("40.00")),
                new PaymentMethod("PUNKTY_B_LIKE", 10, bd("100.00")),
                new PaymentMethod("PROMO_CARD_C", 12, bd("100.00"))
        );

        List<PaymentMethod> testMethods = Arrays.asList(
                new PaymentMethod(POINTS_ID_STRING, 10, bd("100.00")),
                new PaymentMethod("CARD_C", 12, bd("100.00")),
                new PaymentMethod("CARD_X", 0, bd("40.00"))
        );
        Order orderForSort = new Order("ORDER_SORT", bd("100.00"), Collections.singletonList("CARD_C"));


        Map<String, BigDecimal> result = paymentOptimizerService.optimizePayments(Collections.singletonList(orderForSort), testMethods);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("CARD_C"));
        assertEquals(0, bd("88.00").compareTo(result.get("CARD_C")));
    }

    @Test
    @DisplayName("[optimizePayments] Preference for points when discounts are equal")
    void optimizePayments_preferPointsWhenDiscountsEqual() {
        Order order1 = new Order("ORDER1", bd("100.00"), Collections.singletonList("CARD_A"));
        List<Order> orders = Collections.singletonList(order1);

        List<PaymentMethod> paymentMethods = Arrays.asList(
                new PaymentMethod("CARD_A", 10, bd("100.00")),

                new PaymentMethod(POINTS_ID_STRING, 10, bd("100.00"))
        );

        Map<String, BigDecimal> result = paymentOptimizerService.optimizePayments(orders, paymentMethods);

        assertEquals(1, result.size());
        assertTrue(result.containsKey(POINTS_ID_STRING));
        assertEquals(0, bd("90.00").compareTo(result.get(POINTS_ID_STRING)));
        assertNull(result.get("CARD_A"));
    }

    @Test
    @DisplayName("[optimizePayments] Complex scenario with multiple orders and limited resources, testing sorting and resource depletion")
    void optimizePayments_complexScenario_limitedResources() {
        Order orderA = new Order("ORDER_A", bd("100.00"), Collections.singletonList("PROMO_CARD_15"));
        Order orderC = new Order("ORDER_C", bd("120.00"), Collections.singletonList("PROMO_CARD_10"));
        Order orderB = new Order("ORDER_B", bd("80.00"), null);
        List<Order> orders = Arrays.asList(orderA, orderC, orderB);

        List<PaymentMethod> paymentMethods = Arrays.asList(
                new PaymentMethod(POINTS_ID_STRING, 5, bd("70.00")),
                new PaymentMethod("PROMO_CARD_15", 15, bd("90.00")),
                new PaymentMethod("PROMO_CARD_10", 10, bd("110.00")),
                new PaymentMethod("GENERIC_CARD", 0, bd("200.00"))
        );

        Map<String, BigDecimal> result = paymentOptimizerService.optimizePayments(orders, paymentMethods);

        assertNotNull(result.get("PROMO_CARD_15"), "PROMO_CARD_15 should be in result");
        assertEquals(0, bd("85.00").compareTo(result.get("PROMO_CARD_15")), "PROMO_CARD_15 total spent incorrect");

        assertNotNull(result.get(POINTS_ID_STRING), "PUNKTY should be in result");
        assertEquals(0, bd("70.00").compareTo(result.get(POINTS_ID_STRING)), "PUNKTY total spent incorrect");

        assertNotNull(result.get("GENERIC_CARD"), "GENERIC_CARD should be in result");
        assertEquals(0, bd("118.00").compareTo(result.get("GENERIC_CARD")), "GENERIC_CARD total spent incorrect");
        assertNull(result.get("PROMO_CARD_10"), "PROMO_CARD_10 should not have been used if GENERIC_CARD was chosen for ORDER_B remainder");
        assertEquals(3, result.size(), "Result map should contain 3 entries if GENERIC_CARD used for ORDER_B");
    }





}