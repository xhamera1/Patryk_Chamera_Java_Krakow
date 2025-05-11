package com.chamera.patryk.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;


/**
 * Represents a payment method available to the customer.
 * It includes details about the discount offered (if any) and the spending limit
 * associated with this method.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

    private String id;
    private int discount;
    private BigDecimal limit;
}
