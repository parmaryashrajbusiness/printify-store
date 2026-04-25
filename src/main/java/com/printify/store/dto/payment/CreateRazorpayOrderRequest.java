package com.printify.store.dto.payment;

import com.printify.store.dto.order.CheckoutRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRazorpayOrderRequest {
    @Valid
    @NotNull
    private CheckoutRequest checkout;
}