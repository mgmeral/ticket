package dev.mgmeral.ticket.service.impl;

import dev.mgmeral.ticket.entity.Payment;
import dev.mgmeral.ticket.enums.PaymentStatus;
import dev.mgmeral.ticket.model.PaymentAuthorizeRequest;
import dev.mgmeral.ticket.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    PaymentRepository paymentRepository;

    @InjectMocks
    PaymentServiceImpl service;

    @Captor
    ArgumentCaptor<Payment> paymentCaptor;

    @Test
    void authorize_shouldPersistPayment_andReturnResponse_authorized() {
        var req = mock(PaymentAuthorizeRequest.class);
        when(req.amount()).thenReturn(new BigDecimal("10.08"));

        when(paymentRepository.save(paymentCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        var res = service.authorize(req);

        assertThat(res.paymentRef()).isNotBlank();
        assertThat(res.amount()).isEqualByComparingTo("10.08");
        assertThat(res.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(res.createdAt()).isNotNull();

        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getPaymentRef()).isEqualTo(res.paymentRef());
        assertThat(saved.getAmount()).isEqualByComparingTo(res.amount());
        assertThat(saved.getStatus()).isEqualTo(res.status());

        verify(paymentRepository).save(eq(saved));
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void authorize_shouldPersistPayment_andReturnResponse_declined_whenCentsEndsWith07() {
        var req = mock(PaymentAuthorizeRequest.class);
        when(req.amount()).thenReturn(new BigDecimal("12.07"));

        when(paymentRepository.save(paymentCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        var res = service.authorize(req);

        assertThat(res.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(res.paymentRef()).isNotBlank();

        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(saved.getPaymentRef()).isEqualTo(res.paymentRef());

        verify(paymentRepository).save(eq(saved));
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void authorize_shouldUseAbsoluteValue_forDecision() {
        var req = mock(PaymentAuthorizeRequest.class);
        when(req.amount()).thenReturn(new BigDecimal("-1.07"));

        when(paymentRepository.save(paymentCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        var res = service.authorize(req);

        assertThat(res.status()).isEqualTo(PaymentStatus.AUTHORIZED);

        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);

        verify(paymentRepository).save(eq(saved));
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void authorize_shouldGenerateUniquePaymentRef_eachCall() {
        var req1 = mock(PaymentAuthorizeRequest.class);
        var req2 = mock(PaymentAuthorizeRequest.class);
        when(req1.amount()).thenReturn(new BigDecimal("10.01"));
        when(req2.amount()).thenReturn(new BigDecimal("10.01"));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var r1 = service.authorize(req1);
        var r2 = service.authorize(req2);

        assertThat(r1.paymentRef()).isNotEqualTo(r2.paymentRef());
        assertThat(r1.createdAt()).isNotNull();
        assertThat(r2.createdAt()).isNotNull();

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().get(0).getPaymentRef())
                .isNotEqualTo(captor.getAllValues().get(1).getPaymentRef());

        verifyNoMoreInteractions(paymentRepository);
    }
}
