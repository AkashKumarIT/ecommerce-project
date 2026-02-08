package com.ecom.inventory_service.mapper;

import com.ecom.events.order.OrderCreatedEvent;
import com.ecom.inventory_service.dto.InventoryReservationRequest;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {
    public InventoryReservationRequest mapToReservation(OrderCreatedEvent event){
        InventoryReservationRequest request = new InventoryReservationRequest();
        request.setOrderId(event.getOrderId());

        var items = event.getItems().stream().map(eventItem -> {
            InventoryReservationRequest.Item item = new InventoryReservationRequest.Item();
            item.setSku(eventItem.getSku());
            item.setQty(eventItem.getQty());
            return item;
        }).toList();

        request.setItems(items);
        return request;
    }
}
