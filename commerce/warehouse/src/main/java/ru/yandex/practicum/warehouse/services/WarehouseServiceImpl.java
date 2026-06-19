package ru.yandex.practicum.warehouse.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.error_handler.exception.warehouse.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.error_handler.exception.warehouse.ProductInWarehouseNotFoundException;
import ru.yandex.practicum.error_handler.exception.warehouse.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.interaction_api.model.shopping_cart.dto.ShoppingCartDto;
import ru.yandex.practicum.interaction_api.model.warehouse.dto.AddressDto;
import ru.yandex.practicum.interaction_api.model.warehouse.dto.BookedProductsDto;
import ru.yandex.practicum.interaction_api.model.warehouse.dto.DimensionDto;
import ru.yandex.practicum.interaction_api.model.warehouse.dto.ProductInWarehouseDto;
import ru.yandex.practicum.interaction_api.model.warehouse.dto.request.AddProductToWarehouseRequest;
import ru.yandex.practicum.interaction_api.model.warehouse.dto.request.NewProductInWarehouseRequest;
import ru.yandex.practicum.warehouse.WarehouseApplication;
import ru.yandex.practicum.warehouse.entity.ProductInWarehouseDao;
import ru.yandex.practicum.warehouse.mapper.ProductInWarehouseMapper;
import ru.yandex.practicum.warehouse.repositories.WarehouseRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    public AddressDto getAddress() {
        return WarehouseApplication.getRandomAddress();
    }

    @Override
    public ProductInWarehouseDto addNewProduct(NewProductInWarehouseRequest newProduct) {
        if (isProductInWarehouse(newProduct.getProductId())) {
            throw new SpecifiedProductAlreadyInWarehouseException("Продукт с id " + newProduct.getProductId() + " уже добавлен на склад!");
        }

        return ProductInWarehouseMapper.toDto(warehouseRepository.save(ProductInWarehouseMapper.toEntity(newProduct)));
    }

    @Override
    public void acceptProduct(AddProductToWarehouseRequest request) {

        ProductInWarehouseDao productInWarehouseDao = getProductInWarehouse(request.getProductId());
        productInWarehouseDao.setQuantity(productInWarehouseDao.getQuantity()+request.getQuantity());

        warehouseRepository.save(productInWarehouseDao);

        log.info("Продукт с id {} в количестве {} принят на склад!", productInWarehouseDao.getProductId(), productInWarehouseDao.getQuantity());
    }

    @Override
    public BookedProductsDto checkQuantityForCart(ShoppingCartDto shoppingCart) {
        BookedProductsDto bookedProductsDto = BookedProductsDto.builder().build();

        shoppingCart.getProducts().forEach((productId, quantity) -> {
            ProductInWarehouseDao productInWarehouseDao = getProductInWarehouse(productId);

            if (quantity > productInWarehouseDao.getQuantity()) {
                throw new ProductInShoppingCartLowQuantityInWarehouse("Товара с id " + productId + " в корзине больше, чем доступно на складе!");
            }

            bookedProductsDto.setDeliveryWeight(bookedProductsDto.getDeliveryWeight()+ productInWarehouseDao.getWeight());
            bookedProductsDto.setDeliveryVolume(bookedProductsDto.getDeliveryVolume()+calculateVolume(productInWarehouseDao));
        });

        return bookedProductsDto;
    }

    private boolean isProductInWarehouse(UUID productId) {
        return warehouseRepository.existsById(productId);
    }

    private Double calculateVolume(ProductInWarehouseDao product) {
        DimensionDto dimension = product.getDimension();
        return dimension.getHeight()*dimension.getDepth()*dimension.getWidth();
    }

    private ProductInWarehouseDao getProductInWarehouse(UUID productId) {
        return warehouseRepository.findById(productId)
                .orElseThrow(() -> new ProductInWarehouseNotFoundException("Продукт с id " + productId + " не найден на складе!"));
    }
}