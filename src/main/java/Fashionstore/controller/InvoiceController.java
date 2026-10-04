package Fashionstore.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import Fashionstore.entities.Order;
import Fashionstore.repositories.OrderRepository;

@Controller
public class InvoiceController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/invoice/{id}")
    public String invoice(
            @PathVariable("id") String id,
            Model model) {

        Order order = orderRepository.findById(id).orElse(null);

        if (order == null) {
            model.addAttribute("msg", "Không tìm thấy hóa đơn.");
            return "error";
        }

        String bankId = "970422";
        String accountNo = "0367371943";
        String accountName = "TRAN DUY PHUC";

        // Nội dung chuyển khoản phải giống Webhook
        String addInfo = "ORDER-" + order.getOrderId();

        String qrUrl =
                "https://img.vietqr.io/image/"
                + bankId + "-"
                + accountNo
                + "-compact2.png"
                + "?amount=" + Math.round(order.getTotal())
                + "&addInfo=" + URLEncoder.encode(addInfo, StandardCharsets.UTF_8)
                + "&accountName=" + URLEncoder.encode(accountName, StandardCharsets.UTF_8);

        model.addAttribute("order", order);
        model.addAttribute("items", order.getDetails());
        model.addAttribute("qrUrl", qrUrl);

        return "home/invoice";
    }

}