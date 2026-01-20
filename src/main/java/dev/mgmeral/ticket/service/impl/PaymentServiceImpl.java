package dev.mgmeral.ticket.service.impl;

import dev.mgmeral.ticket.entity.Payment;
import dev.mgmeral.ticket.enums.PaymentStatus;
import dev.mgmeral.ticket.model.PaymentAuthorizeRequest;
import dev.mgmeral.ticket.model.PaymentAuthorizeResponse;
import dev.mgmeral.ticket.repository.PaymentRepository;
import dev.mgmeral.ticket.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PaymentAuthorizeResponse authorize(PaymentAuthorizeRequest request) {
        Instant now = Instant.now();
        BigDecimal amount = request.amount();

        PaymentStatus status = mockDecision(amount);
        String paymentRef = UUID.randomUUID().toString();

        log.info("payment.authorize.start paymentRef={} amount={} decision={}",
                paymentRef, amount, status);

        Payment payment = Payment.builder()
                .paymentRef(paymentRef)
                .status(status)
                .amount(amount)
                .build();

        Payment saved = paymentRepository.save(payment);

        log.info("payment.authorize.ok paymentRef={} paymentId={} status={} amount={} at={}",
                saved.getPaymentRef(), saved.getId(), saved.getStatus(), saved.getAmount(), now);

        return new PaymentAuthorizeResponse(paymentRef, status, amount, now);
    }

    private PaymentStatus mockDecision(BigDecimal amount) {
        long totalCents = amount.abs()
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();

        PaymentStatus status = (totalCents == 7) ? PaymentStatus.DECLINED : PaymentStatus.AUTHORIZED;

        log.debug("payment.mockDecision amount={} totalCents={} status={}", amount, totalCents, status);

        return status;
    }
}
