package Fashionstore.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import Fashionstore.entities.Order;
import Fashionstore.repositories.OrderRepository;

@RestController
public class PaymentController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/api/payment/status/{id}")
    public Map<String, Object> checkPayment(
            @PathVariable("id") String id) {

        Order order = orderRepository.findById(id).orElse(null);

        Map<String, Object> result = new HashMap<>();

        if (order == null) {

            result.put("success", false);
            result.put("status", -1);

            return result;
        }

        result.put("success", true);
        result.put("status", order.getStatus());

        return result;
    }

}