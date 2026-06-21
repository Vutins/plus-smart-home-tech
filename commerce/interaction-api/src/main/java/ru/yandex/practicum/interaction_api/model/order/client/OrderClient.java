package ru.yandex.practicum.interaction_api.model.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.interaction_api.model.order.dto.OrderDto;

import java.util.UUID;

@FeignClient(name = "order")
public interface OrderClient {

    String BASE_URL = "/api/v1/order";

    @PostMapping(BASE_URL+"/get/payment")
    OrderDto getOrderByPayment(@RequestBody UUID paymentId);

    @PostMapping(BASE_URL+"/get/delivery")
    OrderDto getOrderByDelivery(@RequestBody UUID deliveryId);

    @PostMapping(BASE_URL+"/payment")
    void paymentOrder(@RequestBody UUID orderId);

    @PostMapping(BASE_URL+"/payment/failed")
    void failedPaymentOrder(@RequestBody UUID orderId);

    @PostMapping(BASE_URL+"/delivery")
    void deliveryOrder(@RequestBody UUID orderId);

    @PostMapping(BASE_URL+"/delivery/failed")
    void failedDeliveryOrder(@RequestBody UUID orderId);

    @PostMapping(BASE_URL+"/assembly")
    void assemblyOrder(@RequestBody UUID orderId);
}