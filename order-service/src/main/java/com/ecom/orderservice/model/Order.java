package com.ecom.orderservice.model;



import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "t_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID cartId;

    private String userId;

    private String orderNumber; // Unique ID (UUID)

    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderLineItems> orderLineItemsList;

    // Status maintain karna zaroori hai (PENDING, PLACED)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
}