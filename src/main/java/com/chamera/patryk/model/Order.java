package com.chamera.patryk.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

/**
 * Represents a customer order with its value and applicable promotions.
 * This class is a plain data object, primarily used for storing order information
 * parsed from input files.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private String id;
    private BigDecimal value;
    private List<String> promotions;

}
