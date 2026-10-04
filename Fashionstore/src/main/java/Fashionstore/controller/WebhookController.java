package Fashionstore.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import Fashionstore.repositories.OrderRepository;

@RestController
@RequestMapping("/api/sepay")
public class WebhookController {

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping("/webhook")
    public String webhook(@RequestBody Map<String, Object> body) {

        System.out.println("========== WEBHOOK ==========");
        body.forEach((k, v) -> System.out.println(k + " = " + v));

        try {

            Object contentObj = body.get("content");

            if (contentObj == null) {
                System.out.println("Không có content.");
                return "OK";
            }

            String content = contentObj.toString().trim();

            System.out.println("Content gốc: " + content);

            // Chuẩn hóa:
            // ORDERxxxx
            // ORDER-xxxx
            // ORDER xxxx
            content = content
                    .replace("ORDER-", "ORDER")
                    .replace("ORDER ", "ORDER")
                    .trim();

            if (!content.startsWith("ORDER")) {
                System.out.println("Sai định dạng nội dung chuyển khoản.");
                return "OK";
            }

            String orderId = content.substring("ORDER".length()).trim();

         // Nếu UUID không có dấu -, chuyển về đúng định dạng UUID
         if (orderId.length() == 32) {

             orderId =
                     orderId.substring(0, 8) + "-" +
                     orderId.substring(8, 12) + "-" +
                     orderId.substring(12, 16) + "-" +
                     orderId.substring(16, 20) + "-" +
                     orderId.substring(20);

         }

         System.out.println("Order ID = " + orderId);

         int updated = orderRepository.markPaid(orderId);

            if (updated > 0) {
                System.out.println("Đã cập nhật trạng thái thanh toán.");
            } else {
                System.out.println("Không tìm thấy đơn hàng hoặc đơn đã thanh toán.");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return "OK";
    }

}