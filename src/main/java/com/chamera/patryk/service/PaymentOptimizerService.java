package com.chamera.patryk.service;

import com.chamera.patryk.model.Order;
import com.chamera.patryk.model.PaymentMethod;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class PaymentOptimizerService {

    private static final String POINTS_ID_STRING = "PUNKTY";
    private static final BigDecimal PARTIAL_POINTS_ORDER_DISCOUNT_PERCENTAGE = new BigDecimal("0.10");
    private static final BigDecimal MIN_POINTS_PERCENTAGE_FOR_PARTIAL_DISCOUNT = new BigDecimal("0.10");

    @Getter
    @ToString
    private static class PaymentOption {

        BigDecimal calculatedDiscountAmount;
        BigDecimal pointsUsedAmount;
        Map<String, BigDecimal> amountsToChargeByMethod;

        public PaymentOption(BigDecimal calculatedDiscountAmount, BigDecimal pointsUsedAmount, Map<String, BigDecimal> amountsToChargeByMethod) {
            this.calculatedDiscountAmount = calculatedDiscountAmount;
            this.pointsUsedAmount = pointsUsedAmount;
            this.amountsToChargeByMethod = amountsToChargeByMethod;
        }
    }

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


    private BigDecimal calculateMaxTheoreticalDiscount(Order order, Map<String, PaymentMethod> paymentMethodMap) {
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




    private void addFullCardPaymentOptions(Order order, Map<String, PaymentMethod> paymentMethodMap, Map<String, BigDecimal> remainingLimits, List<PaymentOption> possibleOptions) {
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


    private void addFullPointsOptions(Order order, Map<String, PaymentMethod> paymentMethodMap, Map<String, BigDecimal> remainingLimits,List<PaymentOption> possibleOptions) {
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

    private void addPartialPointsOption(Order order, Map<String, PaymentMethod> paymentMethodMap, Map<String, BigDecimal> remainingLimits,List<PaymentOption> possibleOptions) {
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

        BigDecimal orderDiscountAmount = orderValue.multiply(PARTIAL_POINTS_ORDER_DISCOUNT_PERCENTAGE);
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

    private void addCardPaymentWithoutPromotionOption(Order order, Map<String, PaymentMethod> paymentMethodMap, Map<String, BigDecimal> remainingLimits,List<PaymentOption> possibleOptions) {
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



    private void applyPaymentOption(PaymentOption bestOption, Map<String, BigDecimal> remainingLimits, Map<String, BigDecimal> totalSpentByMethod) {
        for (Map.Entry<String, BigDecimal> entry : bestOption.getAmountsToChargeByMethod().entrySet()) {
            String methodId = entry.getKey();
            BigDecimal amountToChargeByMethod = entry.getValue();

            BigDecimal currentLimit = remainingLimits.get(methodId);
            if (currentLimit == null || currentLimit.compareTo(amountToChargeByMethod) < 0) {
                throw new IllegalStateException("Error while aplying payment limit");
            }

            remainingLimits.put(methodId, currentLimit.subtract(amountToChargeByMethod));

            totalSpentByMethod.put(methodId, totalSpentByMethod.getOrDefault(methodId, BigDecimal.ZERO).add(amountToChargeByMethod));
        }
    }





}
