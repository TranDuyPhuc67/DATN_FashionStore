package Fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import Fashionstore.entities.Order;
import Fashionstore.repositories.OrderRepository;


@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderRepository orderRepo;

    // Trang theo dõi đơn hàng
    @GetMapping("/tracking/{id}")
    public String tracking(@PathVariable("id") String id, Model model) {
        Order order = orderRepo.findById(id).orElseThrow();
        model.addAttribute("order", order);
        return "home/order_tracking";
    }
    
    @GetMapping("/admin")
    public String adminPage(Model model) {
        model.addAttribute("orders", orderRepo.findAll());
        return "admin/order_admin";
    }

    @PostMapping("/update")
    public String updateStatus(@RequestParam String id, @RequestParam int status) {
        Order order = orderRepo.findById(id).orElseThrow();
        order.setStatus(status);
        orderRepo.save(order);
        return "redirect:/order/admin";
    }
}