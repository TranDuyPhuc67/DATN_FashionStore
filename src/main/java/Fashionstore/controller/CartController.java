package Fashionstore.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import Fashionstore.entities.Account;
import Fashionstore.entities.CartItem;
import Fashionstore.entities.Order;
import Fashionstore.entities.Product;
import Fashionstore.repositories.AccountRepository;
import Fashionstore.repositories.CartItemRepository;
import Fashionstore.repositories.ProductRepository;
import Fashionstore.services.CartService;

@Controller
public class CartController {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private CartItemRepository cartItemRepo;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepo;

    @GetMapping("/cart")
    public String viewCart(Principal principal, Model model) {

        if (principal == null) {
            return "redirect:/login";
        }

        Account acc = accountRepo.findByUsername(principal.getName())
                .orElseThrow();

        List<CartItem> cartItems = cartItemRepo.findByAccount(acc);

        double totalCart = cartItems.stream()
                .mapToDouble(CartItem::getTotal)
                .sum();

        model.addAttribute("cartitems", cartItems);
        model.addAttribute("totalCart", totalCart);

        return "home/shopingcart";
    }

    @PostMapping("/addcart")
    public String addCart(
            @RequestParam("productId") String productId,
            @RequestParam(defaultValue = "1") int quantity,
            Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        Account acc = accountRepo.findByUsername(principal.getName())
                .orElseThrow();

        Product product = productRepo.findById(productId)
                .orElseThrow();

        Optional<CartItem> existing =
                cartItemRepo.findByAccountAndProduct(acc, product);

        if (existing.isPresent()) {

            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepo.save(item);

        } else {

            CartItem item = new CartItem();
            item.setAccount(acc);
            item.setProduct(product);
            item.setQuantity(quantity);

            cartItemRepo.save(item);
        }

        return "redirect:/cart";
    }

    @GetMapping("/removeCart/{id}")
    public String removeCart(
            @PathVariable("id") String productId,
            Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        cartService.removeCart(principal.getName(), productId);

        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(
            Order order,
            Principal principal,
            Model model) {

        if (principal == null) {
            return "redirect:/login";
        }

        Account acc = accountRepo.findByUsername(principal.getName())
                .orElseThrow();

        List<CartItem> cartItems =
                cartItemRepo.findByAccount(acc);

        if (cartItems.isEmpty()) {

            model.addAttribute("msg", "Giỏ hàng trống!");

            double totalCart = 0;

            model.addAttribute("cartitems", cartItems);
            model.addAttribute("totalCart", totalCart);

            return "home/shopingcart";
        }

        double total = cartItems.stream()
                .mapToDouble(CartItem::getTotal)
                .sum();

        order.setTotal(total);
        order.setOrderDate(LocalDateTime.now());
        order.setReceivedate(LocalDateTime.now().plusDays(3));

        // 0 = Chờ thanh toán
        order.setStatus(0);

        order.setAccount(acc);

        /*
         * Nếu Order.java đang dùng:
         *
         * @GeneratedValue(strategy = GenerationType.UUID)
         *
         * thì KHÔNG gọi order.setOrderId(...)
         */

        Order savedOrder = cartService.insertOrder(order, cartItems);

        return "redirect:/invoice/" + savedOrder.getOrderId();
    }

}