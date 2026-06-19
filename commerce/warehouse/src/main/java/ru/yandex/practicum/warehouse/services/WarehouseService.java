package ru.yandex.practicum.warehouse.services;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.interaction_api.model.shopping_cart.dto.ShoppingCartDto;
import ru.yandex.practicum.interaction_api.model.warehouse.dto.AddressDto;
import ru.yandex.practicum.interaction_api.model.warehouse.dto.BookedProductsDto;
import ru.yandex.practicum.interaction_api.model.warehouse.dto.ProductInWarehouseDto;
import ru.yandex.practicum.interaction_api.model.warehouse.dto.request.AddProductToWarehouseRequest;
import ru.yandex.practicum.interaction_api.model.warehouse.dto.request.NewProductInWarehouseRequest;

@Service
public interface WarehouseService {

    ProductInWarehouseDto addNewProduct(NewProductInWarehouseRequest newProductInWarehouseRequest);

    BookedProductsDto checkQuantityForCart(ShoppingCartDto shoppingCart);

    void acceptProduct(AddProductToWarehouseRequest request);

    AddressDto getAddress();
}