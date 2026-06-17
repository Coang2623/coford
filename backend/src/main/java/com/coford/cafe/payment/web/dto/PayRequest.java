package com.coford.cafe.payment.web.dto;

import com.coford.cafe.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PayRequest(@NotNull PaymentMethod method) {
}
