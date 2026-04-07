package com.dms.user_service.controller;

import com.dms.user_service.dto.*;
import com.dms.user_service.entity.User;
import com.dms.user_service.service.UserService;
import com.dms.user_service.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserUIController {

    private final UserService userService;
    private final RestTemplate restTemplate;

    @Value("${product.service.url:http://localhost:8081}")
    private String productServiceUrl;

    @Value("${order.service.url:http://localhost:8083}")
    private String orderServiceUrl;

    @Value("${audit.service.url:http://localhost:8084}")
    private String auditServiceUrl;

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    // POST /users/login is handled by Spring Security, so this method is removed.

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest request, Model model, RedirectAttributes redirectAttributes) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("error", "Passwords do not match");
            return "register";
        }
        try {
            userService.registerUser(request);
            redirectAttributes.addFlashAttribute("message", "Registration successful");
            return "redirect:/users/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute ChangePasswordRequest request, Model model, RedirectAttributes redirectAttributes) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            model.addAttribute("error", "New passwords do not match");
            return "change-password";
        }
        try {
            String message = userService.changePassword(request);
            redirectAttributes.addFlashAttribute("message", message);
            return "redirect:/users";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "change-password";
        }
    }

    @GetMapping
    public String rootDashboard(Principal principal) {
        if (principal == null) {
            return "redirect:/users/login";
        }

        User currentUser = userService.getUserByUsername(principal.getName());
        if (currentUser.getRole() == User.Role.ADMIN) {
            return "redirect:/users/admin/dashboard";
        } else if (currentUser.getRole() == User.Role.DISTRIBUTOR) {
            return "redirect:/users/distributor/dashboard";
        }
        return "redirect:/users/login";
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("distributors", userService.getDistributors());
        return "admin-dashboard";
    }

    @GetMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminProductList(Model model) {
        ProductDto[] products = restTemplate.getForObject(productServiceUrl + "/api/products", ProductDto[].class);
        model.addAttribute("products", Arrays.asList(products));
        return "admin-products";
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOrderList(Model model) {
        OrderDto[] orders = restTemplate.getForObject(orderServiceUrl + "/api/orders", OrderDto[].class);
        model.addAttribute("orders", Arrays.asList(orders));
        return "admin-orders";
    }

    @GetMapping("/admin/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAuditList(Model model) {
        AuditEventDto[] events = restTemplate.getForObject(auditServiceUrl + "/api/audit/events", AuditEventDto[].class);
        model.addAttribute("events", Arrays.asList(events));
        return "admin-audit";
    }

    @GetMapping("/distributor/dashboard")
    @PreAuthorize("hasRole('DISTRIBUTOR')")
    public String distributorDashboard(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        model.addAttribute("currentUser", currentUser);
        return "distributor-dashboard";
    }

    @GetMapping("/distributor/products")
    @PreAuthorize("hasRole('DISTRIBUTOR')")
    public String distributorProductList(Model model) {
        ProductDto[] products = restTemplate.getForObject(productServiceUrl + "/api/products/active", ProductDto[].class);
        model.addAttribute("products", Arrays.asList(products));
        return "distributor-products";
    }

    @GetMapping("/distributor/orders")
    @PreAuthorize("hasRole('DISTRIBUTOR')")
    public String distributorOrderList(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        OrderDto[] orders = restTemplate.getForObject(orderServiceUrl + "/api/orders/distributor/" + currentUser.getId(), OrderDto[].class);
        model.addAttribute("orders", Arrays.asList(orders));
        return "distributor-orders";
    }

    @GetMapping("/distributor/order/create")
    @PreAuthorize("hasRole('DISTRIBUTOR')")
    public String createOrderForm(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        OrderCreateDto dto = new OrderCreateDto();
        dto.setDistributorId(currentUser.getId());
        model.addAttribute("orderCreateDto", dto);
        return "distributor-create-order";
    }

    @PostMapping("/distributor/order/create")
    @PreAuthorize("hasRole('DISTRIBUTOR')")
    public String createOrderSubmit(@ModelAttribute OrderCreateDto orderCreateDto, RedirectAttributes redirectAttributes) {
        restTemplate.postForObject(orderServiceUrl + "/api/orders", orderCreateDto, OrderDto.class);
        redirectAttributes.addFlashAttribute("message", "Order placed successfully");
        return "redirect:/users/distributor/orders";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRIBUTOR')")
    public String getUserById(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "user-detail";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            String message = userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("message", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users/admin/dashboard";
    }

    @GetMapping("/logout")
    public String logout(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "Logged out successfully");
        return "redirect:/users/login";
    }
}