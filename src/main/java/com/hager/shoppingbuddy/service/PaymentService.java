package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${shoppingbuddy.stripe.secretkey}")
    private String stripeSecretKey;

    @Value("${shoppingbuddy.stripe.publickey}")
    private String stripePublicKey;

}
