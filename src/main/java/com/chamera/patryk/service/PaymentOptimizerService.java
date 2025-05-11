package com.chamera.patryk.service;

import com.chamera.patryk.model.Order;
import com.chamera.patryk.model.PaymentMethod;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Service responsible for optimizing payment methods for a list of orders.
 * The main goal is to maximize the total discount obtained by strategically selecting
 * payment methods for each order, while ensuring all orders are fully paid.
 * It prioritizes using loyalty points if doing so does not reduce the overall discount.
 */
public class PaymentOptimizerService {

    private static final String POINTS_ID_STRING = "PUNKTY";
    private static final BigDecimal PARTIAL_POINTS_ORDER_DISCOUNT_PERCENTAGE = new BigDecimal("0.10");
    private static final BigDecimal MIN_POINTS_PERCENTAGE_FOR_PARTIAL_DISCOUNT = new BigDecimal("0.10");


    /**
     * Represents a potential payment option for a single order.
     * It stores the calculated discount, amount of points used, and the breakdown
     * of amounts to be charged to various payment methods.
     */
    @Getter
    @ToString
    protected static class PaymentOption {

        BigDecimal calculatedDiscountAmount;
        BigDecimal pointsUsedAmount;
        Map<String, BigDecimal> amountsToChargeByMethod;

        /**
         * Constructs a PaymentOption.
         * @param calculatedDiscountAmount The discount achieved.
         * @param pointsUsedAmount         The amount of points used.
         * @param amountsToChargeByMethod  Map of payment method IDs to amounts charged.
         */
        public PaymentOption(BigDecimal calculatedDiscountAmount, BigDecimal pointsUsedAmount, Map<String, BigDecimal> amountsToChargeByMethod) {
            this.calculatedDiscountAmount = calculatedDiscountAmount;
            this.pointsUsedAmount = pointsUsedAmount;
            this.amountsToChargeByMethod = amountsToChargeByMethod;
        }
    }


    /**
     * Main method to find the optimal payment distribution for a list of orders.
     * It sorts orders by their highest theoretical discount to tackle high-value discounts first.
     * For each order, it explores different payment scenarios (full card, full points, partial points, etc.),
     * choosing the one that offers the best discount, or uses more points if discounts are equal.
     *
     * @param orders List of orders to process.
     * @param paymentMethods Available payment methods.
     * @return Map of payment method ID to total amount spent with that method.
     * @throws RuntimeException If an order cannot be fully paid.
     */
    public Map<String, BigDecimal> optimizePayments(List<Order> orders, List<PaymentMethod> paymentMethods) {
        Map<String, PaymentMethod> paymentMethodMap = new HashMap<>();
        Map<String, BigDecimal> remainingLimits = new HashMap<>();
        for (PaymentMethod paymentMethod : paymentMethods) {
            paymentMethodMap.put(paymentMethod.getId(), paymentMethod);
            remainingLimits.put(paymentMethod.getId(), paymentMethod.getLimit());
        }

        List<Order> sortedOrders = new ArrayList<>(orders);
        sortedOrders.sort((order1, order2) -> {
            BigDecimal maxDiscount1 = calculateMaxTheoreticalDiscount(order1, paymentMethodMap);
            BigDecimal maxDiscount2 = calculateMaxTheoreticalDiscount(order2, paymentMethodMap);
            return maxDiscount2.compareTo(maxDiscount1); //malejaco
        });

        Map<String, BigDecimal> totalSpentByMethod = new HashMap<>();

        for (Order order : sortedOrders) {
            List<PaymentOption> possibleOptions = new ArrayList<>();

            addFullCardPaymentOptions(order, paymentMethodMap, remainingLimits, possibleOptions);
            addFullPointsOptions(order, paymentMethodMap, remainingLimits, possibleOptions);
            addPartialPointsOption(order, paymentMethodMap, remainingLimits, possibleOptions); //opcja gdzie min 10% placone punktami, to daje rabat 10%
            addCardPaymentWithoutPromotionOption(order, paymentMethodMap, remainingLimits, possibleOptions);

            if (possibleOptions.isEmpty()) {
                throw new RuntimeException("No possible payment option found for order " + order.getId() +
                        " All orders must be fully paid.");
            }

            // 1)najwiekszy rabat 2)najwiecej puntkow uzytych
            possibleOptions.sort(((o1, o2) -> {
                int discountComparison = o2.getCalculatedDiscountAmount().compareTo(o1.getCalculatedDiscountAmount());
                if (discountComparison != 0) {
                    return discountComparison;
                }
                return o2.getPointsUsedAmount().compareTo(o1.getPointsUsedAmount());
            }));

            PaymentOption bestOption = possibleOptions.get(0);

            applyPaymentOption(bestOption, remainingLimits, totalSpentByMethod);
        }
        return totalSpentByMethod;

    }


    /**
     * Estimates the maximum discount an order could achieve in isolation.
     * Used as a heuristic for sorting orders. Considers:
     * 1. Full payment by points with points-specific discount.
     * 2. Full payment by an eligible promotional card.
     * 3. Partial payment by points (min 10% of value) for a 10% order discount.
     *
     * @param order The order to evaluate.
     * @param paymentMethodMap Available payment methods.
     * @return The highest theoretical discount for this order.
     */
    protected BigDecimal calculateMaxTheoreticalDiscount(Order order, Map<String,PaymentMethod> paymentMethodMap) {
        BigDecimal orderValue = order.getValue();
        BigDecimal maxTheoreticalDiscount = BigDecimal.ZERO;

         //pelna platnosc punktami
        PaymentMethod pointsDetails = paymentMethodMap.get(POINTS_ID_STRING);
        if (pointsDetails != null) {
            BigDecimal pointsDiscountPercentage = BigDecimal.valueOf(pointsDetails.getDiscount())
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            BigDecimal discountAmountFromFullPoints = orderValue.multiply(pointsDiscountPercentage);
            BigDecimal amountToPayWithPoints = orderValue.subtract(discountAmountFromFullPoints);

            if (pointsDetails.getLimit().compareTo(amountToPayWithPoints) >= 0) {
                if (maxTheoreticalDiscount.compareTo(discountAmountFromFullPoints) < 0) {
                    maxTheoreticalDiscount = discountAmountFromFullPoints;
                }
            }
        }

        //pelna platnosc kartami
        if (order.getPromotions() != null) {
            for (String promotionId : order.getPromotions()) {
                PaymentMethod cardDetails = paymentMethodMap.get(promotionId);
                if (cardDetails != null && !cardDetails.getId().equals(POINTS_ID_STRING)) {
                    BigDecimal cardDiscountPercentage = BigDecimal.valueOf(cardDetails.getDiscount())
                            .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
                    BigDecimal discountAmountFromCard = orderValue.multiply(cardDiscountPercentage);
                    BigDecimal amountToPayWithCard = orderValue.subtract(discountAmountFromCard);
                    if (cardDetails.getLimit().compareTo(amountToPayWithCard) >= 0) {
                        if (maxTheoreticalDiscount.compareTo(discountAmountFromCard) < 0) {
                            maxTheoreticalDiscount = discountAmountFromCard;
                        }
                    }
                }
            }
        }

        // czesciowa platnosc punktami (min 10% wartosxi)
        PaymentMethod pointsForPartialPayment = paymentMethodMap.get(POINTS_ID_STRING);
        if (pointsForPartialPayment != null) {
            BigDecimal minPointsValueNeeded = orderValue.multiply(MIN_POINTS_PERCENTAGE_FOR_PARTIAL_DISCOUNT);
            if (pointsForPartialPayment.getLimit().compareTo(minPointsValueNeeded) >= 0) {
                BigDecimal discountAmountFromPartialPoints = orderValue.multiply(PARTIAL_POINTS_ORDER_DISCOUNT_PERCENTAGE);
                if (maxTheoreticalDiscount.compareTo(discountAmountFromPartialPoints) < 0) {
                    maxTheoreticalDiscount = discountAmountFromPartialPoints;
                }
            }
        }

        return maxTheoreticalDiscount.setScale(2, RoundingMode.HALF_UP);
    }



    /**
     * Adds options for paying fully with a promotional card.
     * Iterates through order's promotions, if a card has enough limit after its discount, an option is added.
     *
     * @param order Current order.
     * @param paymentMethodMap All payment methods.
     * @param remainingLimits Current limits of methods.
     * @param possibleOptions List to add valid options.
     */
    protected void addFullCardPaymentOptions(Order order, Map<String, PaymentMethod> paymentMethodMap, Map<String, BigDecimal> remainingLimits, List<PaymentOption> possibleOptions) {
        BigDecimal orderValue = order.getValue();

        if (order.getPromotions() == null || order.getPromotions().isEmpty()) {
            return;
        }

        for (String promotionId : order.getPromotions()) {
            PaymentMethod paymentMethod = paymentMethodMap.get(promotionId);
            if (paymentMethod == null || paymentMethod.getId().equals(POINTS_ID_STRING)) {
                continue;
            }

            BigDecimal discountPercentage = BigDecimal.valueOf(paymentMethod.getDiscount()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            BigDecimal discountAmount = orderValue.multiply(discountPercentage).setScale(2, RoundingMode.HALF_UP);
            BigDecimal amountToPay = orderValue.subtract(discountAmount);

            BigDecimal paymentMethodLimit = remainingLimits.getOrDefault(paymentMethod.getId(), BigDecimal.ZERO);

            if (paymentMethodLimit.compareTo(amountToPay) >= 0) {
                Map<String, BigDecimal> usedMethods = new HashMap<>();
                usedMethods.put(paymentMethod.getId(), amountToPay); //tylko kwota z karty, nie ma punktow w tej metodzie
                possibleOptions.add(new PaymentOption(discountAmount, BigDecimal.ZERO, usedMethods));
            }
        }
    }


    /**
     * Adds an option for paying fully with loyalty points ("PUNKTY").
     * Considers the discount associated with the PUNKTY method itself.
     *
     * @param order Current order.
     * @param paymentMethodMap All payment methods.
     * @param remainingLimits Current limits of methods.
     * @param possibleOptions List to add valid options.
     */
    protected void addFullPointsOptions(Order order, Map<String, PaymentMethod> paymentMethodMap, Map<String, BigDecimal> remainingLimits,List<PaymentOption> possibleOptions) {
        BigDecimal orderValue = order.getValue();
        PaymentMethod method = paymentMethodMap.get(POINTS_ID_STRING);

        if (method == null) {
            return;
        }

        BigDecimal pointsDiscountPercentage = BigDecimal.valueOf(method.getDiscount()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal discountAmount = orderValue.multiply(pointsDiscountPercentage).setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountToPayWithPoints = orderValue.subtract(discountAmount);
        BigDecimal pointsLimit = remainingLimits.getOrDefault(method.getId(), BigDecimal.ZERO);

        if (pointsLimit.compareTo(amountToPayWithPoints) >= 0) {
            Map<String, BigDecimal> usedMethods = new HashMap<>();
            usedMethods.put(POINTS_ID_STRING, amountToPayWithPoints);  // tylko punkty zadnej karty nie uzyto tu
            possibleOptions.add(new PaymentOption(discountAmount, amountToPayWithPoints, usedMethods));
        }
    }


    /**
     * Adds option for partial points payment (min 10% of order value for 10% total discount),
     * with remainder paid by a selected card. Card selection prioritizes non-promotional cards
     * by lowest sufficient limit, then promotional cards by lowest discount/limit.
     *
     * @param order Current order.
     * @param paymentMethodMap All payment methods.
     * @param remainingLimits Current limits of methods.
     * @param possibleOptions List to add valid options.
     */
    protected void addPartialPointsOption(Order order, Map<String, PaymentMethod> paymentMethodMap, Map<String, BigDecimal> remainingLimits,List<PaymentOption> possibleOptions) {
        BigDecimal orderValue = order.getValue();
        PaymentMethod pointsMethod = paymentMethodMap.get(POINTS_ID_STRING);

        if (pointsMethod == null) {
            return;
        }

        BigDecimal minPointsRequired = orderValue.multiply(MIN_POINTS_PERCENTAGE_FOR_PARTIAL_DISCOUNT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pointsAvailable = remainingLimits.getOrDefault(POINTS_ID_STRING, BigDecimal.ZERO);

        if (pointsAvailable.compareTo(minPointsRequired) < 0) {
            return;
        }

        BigDecimal orderDiscountAmount = orderValue.multiply(PARTIAL_POINTS_ORDER_DISCOUNT_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal orderValueAfter10PercentageDiscount = orderValue.subtract(orderDiscountAmount);

        BigDecimal actualPointsToSpend;
        if (pointsAvailable.compareTo(orderValueAfter10PercentageDiscount) >= 0) {
            actualPointsToSpend = orderValueAfter10PercentageDiscount;
        } else {
            actualPointsToSpend = pointsAvailable;
        }

        BigDecimal amountLeftToPayByCard = orderValueAfter10PercentageDiscount.subtract(actualPointsToSpend);

        if (amountLeftToPayByCard.compareTo(BigDecimal.ZERO) < 0) {
            amountLeftToPayByCard = BigDecimal.ZERO;
        }

        if (amountLeftToPayByCard.compareTo(BigDecimal.ZERO) == 0) {
            Map<String, BigDecimal> usedMethods = new HashMap<>();
            usedMethods.put(POINTS_ID_STRING, actualPointsToSpend);
            possibleOptions.add(new PaymentOption(orderDiscountAmount, actualPointsToSpend, usedMethods));
            return;
        }

        //logika wyboru karty
        PaymentMethod selectedCardToPayRemainder = null;
        List<PaymentMethod> candidateCardsGroup1 = new ArrayList<>(); // bez promocji
        List<PaymentMethod> candidateCardsGroup2 = new ArrayList<>(); // z

        for (PaymentMethod card : paymentMethodMap.values()) {
            if (card.getId().equals(POINTS_ID_STRING)) {
                continue;
            }
            BigDecimal cardLimit = remainingLimits.getOrDefault(card.getId(), BigDecimal.ZERO);

            if (cardLimit.compareTo(amountLeftToPayByCard) >= 0) {
                boolean isOnOrderPromotion = false;
                if (order.getPromotions() != null && order.getPromotions().contains(card.getId())) {
                    isOnOrderPromotion = true;
                }

                if (isOnOrderPromotion) {
                    candidateCardsGroup2.add(card);
                }
                else {
                    candidateCardsGroup1.add(card);
                }
            }
        }
        if (!candidateCardsGroup1.isEmpty()) {
            candidateCardsGroup1.sort(Comparator.comparing(card -> remainingLimits.getOrDefault(card.getId(), BigDecimal.ZERO)));
            selectedCardToPayRemainder = candidateCardsGroup1.get(0);
        }

        if (selectedCardToPayRemainder == null && !candidateCardsGroup2.isEmpty()) {
            candidateCardsGroup2.sort(Comparator
                    .comparingInt(PaymentMethod::getDiscount) // rosnaco po rabacie (chchemy wybrac ta ktora ma najmniejszy rabat bo i tak sie nie liczy tu)
                    .thenComparing(card -> remainingLimits.getOrDefault(card.getId(), BigDecimal.ZERO)) //rosnaco po limicie
            );
            selectedCardToPayRemainder = candidateCardsGroup2.get(0);
        }

        if (selectedCardToPayRemainder != null) {
            Map<String, BigDecimal> usedMethods = new HashMap<>();
            usedMethods.put(POINTS_ID_STRING, actualPointsToSpend);
            usedMethods.put(selectedCardToPayRemainder.getId(), amountLeftToPayByCard);
            possibleOptions.add(new PaymentOption(orderDiscountAmount, actualPointsToSpend, usedMethods));
        }
    }


    /**
     * Adds options for paying fully with any card, without applying its specific promotion.
     * This is a fallback if other discounted options are unavailable.
     *
     * @param order Current order.
     * @param paymentMethodMap All payment methods.
     * @param remainingLimits Current limits of methods.
     * @param possibleOptions List to add valid options.
     */
    protected void addCardPaymentWithoutPromotionOption(Order order, Map<String, PaymentMethod> paymentMethodMap, Map<String, BigDecimal> remainingLimits,List<PaymentOption> possibleOptions) {
        BigDecimal orderValue = order.getValue();

        for (PaymentMethod card : paymentMethodMap.values()) {
            if (card.getId().equals(POINTS_ID_STRING)) {
                continue;
            }
            BigDecimal cardLimit = remainingLimits.getOrDefault(card.getId(), BigDecimal.ZERO);
            if (cardLimit.compareTo(orderValue) >= 0) {
                Map<String, BigDecimal> usedMethods = new HashMap<>();
                usedMethods.put(card.getId(), orderValue);
                possibleOptions.add(new PaymentOption(BigDecimal.ZERO, BigDecimal.ZERO, usedMethods));
            }

        }
    }



    /**
     * Applies the chosen payment option by updating limits and total spent amounts.
     *
     * @param bestOption The payment option to apply.
     * @param remainingLimits Map of method limits to be updated.
     * @param totalSpentByMethod Map of total spent per method to be updated.
     * @throws IllegalStateException If a method's limit is found to be insufficient during application (logic error).
     */
    protected void applyPaymentOption(PaymentOption bestOption, Map<String, BigDecimal> remainingLimits, Map<String, BigDecimal> totalSpentByMethod) {
        for (Map.Entry<String, BigDecimal> entry : bestOption.getAmountsToChargeByMethod().entrySet()) {
            String methodId = entry.getKey();
            BigDecimal amountToChargeByMethod = entry.getValue();

            BigDecimal currentLimit = remainingLimits.get(methodId);
            if (currentLimit == null || currentLimit.compareTo(amountToChargeByMethod) < 0) {
                throw new IllegalStateException("Error while applying payment limit");
            }

            remainingLimits.put(methodId, currentLimit.subtract(amountToChargeByMethod));

            totalSpentByMethod.put(methodId, totalSpentByMethod.getOrDefault(methodId, BigDecimal.ZERO).add(amountToChargeByMethod));
        }
    }




}
