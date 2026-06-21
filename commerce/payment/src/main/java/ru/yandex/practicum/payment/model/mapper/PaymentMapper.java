package ru.yandex.practicum.payment.model.mapper;

import lombok.experimental.UtilityClass;
import ru.yandex.practicum.interaction_api.model.payment.dto.PaymentDto;
import ru.yandex.practicum.payment.model.entity.Payment;

@UtilityClass
public class PaymentMapper {

    public static PaymentDto toDto(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentDto.builder()
                .paymentId(payment.getPaymentId())
                .totalPayment(payment.getTotalPayment())
                .totalProduct(payment.getTotalProduct())
                .deliveryTotal(payment.getDeliveryTotal())
                .feeTotal(payment.getFeeTotal())
                .status(payment.getStatus())
                .build();
    }
}