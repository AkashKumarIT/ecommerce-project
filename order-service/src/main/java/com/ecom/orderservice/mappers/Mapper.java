package com.ecom.orderservice.mappers;

import com.ecom.orderservice.dto.OrderLineItemsDto;
import com.ecom.orderservice.model.OrderLineItems;
import org.springframework.stereotype.Component;

@Component
public class Mapper {
    public OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSku(orderLineItemsDto.getSku());
        return orderLineItems;
    }
}
