package com.chamera.patryk.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

    private String id;
    private int discount;
    private BigDecimal limit;
}
