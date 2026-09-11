package com.vaadin.demo.nordicsupply.data;

import java.math.BigDecimal;

/** One proposed row of a bulk change: the product, how it reads on screen, its current price and the proposed one. */
public record Proposal(Integer productId, String label, BigDecimal current, BigDecimal proposed) {}
