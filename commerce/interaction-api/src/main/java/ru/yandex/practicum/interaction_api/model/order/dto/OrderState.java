package ru.yandex.practicum.interaction_api.model.order.dto;

public enum OrderState {
    NEW,
    DELIVERED,
    ASSEMBLED,
    PAID,
    COMPLETED,
    DELIVERY_FAILED,
    ASSEMBLY_FAILED,
    PAYMENT_FAILED,
    PRODUCT_RETURNED
}