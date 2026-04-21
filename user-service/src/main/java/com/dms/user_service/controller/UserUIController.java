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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
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
        request.setRole(User.Role.DISTRIBUTOR);
        try {
            userService.registerUser(request);
            String message = "Registration successful. Your distributor account is pending approval by an administrator.";
            redirectAttributes.addFlashAttribute("message", message);
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
        model.addAttribute("distributors", userService.getDistributorViews());
        model.addAttribute("pendingDistributors", userService.getPendingDistributorViews());
        return "admin-dashboard";
    }

    @PostMapping("/admin/distributors/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public String approveDistributor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.approveDistributor(id);
            redirectAttributes.addFlashAttribute("message", "Distributor account approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users/admin/dashboard";
    }

    @PostMapping("/admin/distributors/{id}/deny")
    @PreAuthorize("hasRole('ADMIN')")
    public String denyDistributor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.denyDistributor(id);
            redirectAttributes.addFlashAttribute("message", "Distributor account denied and removed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users/admin/dashboard";
    }

    @GetMapping("/admin/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminProfileForm(@AuthenticationPrincipal CustomUserDetails currentUser,
                                   Model model,
                                   @ModelAttribute("message") String message,
                                   @ModelAttribute("error") String error) {
        User user = userService.getUserById(currentUser.getId());
        UserProfileDto profile = new UserProfileDto();
        profile.setUsername(user.getUsername());
        profile.setFullName(user.getFullName());
        profile.setEmail(user.getEmail());
        model.addAttribute("profile", profile);
        model.addAttribute("message", message);
        model.addAttribute("error", error);
        return "admin-profile";
    }

    @GetMapping("/admin/add-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAddForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "admin-add-admin";
    }

    @PostMapping("/admin/add-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAddSubmit(@Valid @ModelAttribute RegisterRequest request,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("error", "Passwords do not match");
            return "admin-add-admin";
        }
        try {
            request.setRole(User.Role.ADMIN);
            userService.registerUser(request);
            redirectAttributes.addFlashAttribute("message", "Admin user created successfully.");
            return "redirect:/users/admin/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "admin-add-admin";
        }
    }

    @PostMapping("/admin/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminProfileSubmit(@AuthenticationPrincipal CustomUserDetails currentUser,
                                     @ModelAttribute UserProfileDto profile,
                                     RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserProfile(currentUser.getId(), profile);
            redirectAttributes.addFlashAttribute("message", "Profile updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users/admin/profile";
    }

    @GetMapping("/admin/distributors/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editDistributorForm(@PathVariable Long id, Model model) {
        DistributorViewDto distributor = userService.getDistributorViewByUserId(id);
        DistributorUpdateDto dto = new DistributorUpdateDto();
        dto.setDistributorId(distributor.getDistributorId());
        dto.setName(distributor.getName());
        dto.setEmail(distributor.getEmail());
        dto.setCity(distributor.getCity());
        dto.setContact(distributor.getContact());
        model.addAttribute("distributor", dto);
        model.addAttribute("username", distributor.getUsername());
        model.addAttribute("distributorUserId", distributor.getUserId());
        return "admin-edit-distributor";
    }

    @PostMapping("/admin/distributors/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editDistributorSubmit(@PathVariable Long id,
                                        @ModelAttribute DistributorUpdateDto distributor,
                                        RedirectAttributes redirectAttributes) {
        try {
            userService.updateDistributorDetails(id, distributor);
            redirectAttributes.addFlashAttribute("message", "Distributor updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users/admin/distributors/" + id + "/edit";
    }

    @GetMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminProductList(Model model, @ModelAttribute("message") String message, @ModelAttribute("error") String error) {
        ProductDto[] products = restTemplate.getForObject(productServiceUrl + "/api/products", ProductDto[].class);
        model.addAttribute("products", Arrays.asList(products));
        model.addAttribute("message", message);
        model.addAttribute("error", error);
        return "admin-products";
    }

    @GetMapping("/admin/products/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminProductAddForm(Model model) {
        model.addAttribute("productDto", new ProductDto());
        model.addAttribute("formTitle", "Add Product");
        model.addAttribute("formAction", "/users/admin/products/add");
        return "admin-product-form";
    }

    @PostMapping("/admin/products/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminProductAddSubmit(@ModelAttribute ProductDto productDto, RedirectAttributes redirectAttributes) {
        try {
            restTemplate.postForObject(productServiceUrl + "/api/products", productDto, ProductDto.class);
            redirectAttributes.addFlashAttribute("message", "Product added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Unable to add product: " + e.getMessage());
        }
        return "redirect:/users/admin/products";
    }

    @GetMapping("/admin/products/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminProductEditForm(@PathVariable Long id, Model model) {
        ProductDto product = restTemplate.getForObject(productServiceUrl + "/api/products/" + id, ProductDto.class);
        model.addAttribute("productDto", product);
        model.addAttribute("formTitle", "Edit Product");
        model.addAttribute("formAction", "/users/admin/products/" + id + "/edit");
        return "admin-product-form";
    }

    @PostMapping("/admin/products/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminProductEditSubmit(@PathVariable Long id, @ModelAttribute ProductDto productDto, RedirectAttributes redirectAttributes) {
        try {
            restTemplate.put(productServiceUrl + "/api/products/" + id, productDto);
            redirectAttributes.addFlashAttribute("message", "Product updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Unable to update product: " + e.getMessage());
        }
        return "redirect:/users/admin/products";
    }

    @PostMapping("/admin/products/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminProductDelete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            restTemplate.delete(productServiceUrl + "/api/products/" + id);
            redirectAttributes.addFlashAttribute("message", "Product deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Unable to delete product: " + e.getMessage());
        }
        return "redirect:/users/admin/products";
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOrderList(@RequestParam(value = "distributorId", required = false) Long distributorId,
                                 Model model,
                                 @ModelAttribute("message") String message,
                                 @ModelAttribute("error") String error) {
        OrderDto[] orders;
        if (distributorId != null) {
            orders = restTemplate.getForObject(orderServiceUrl + "/api/orders/distributor/" + distributorId, OrderDto[].class);
            model.addAttribute("selectedDistributorId", distributorId);
        } else {
            orders = restTemplate.getForObject(orderServiceUrl + "/api/orders", OrderDto[].class);
        }
        model.addAttribute("orders", orders != null ? Arrays.asList(orders) : new ArrayList<>());
        model.addAttribute("distributors", userService.getDistributorViews());
        model.addAttribute("message", message);
        model.addAttribute("error", error);
        return "admin-orders";
    }

    @GetMapping("/admin/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAuditList(@RequestParam(value = "targetType", required = false) String targetType,
                                 @RequestParam(value = "targetId", required = false) String targetId,
                                 Model model) {
        String url = auditServiceUrl + "/api/audit/events";
        boolean hasType = targetType != null && !targetType.isBlank();
        boolean hasId = targetId != null && !targetId.isBlank();

        if (hasType || hasId) {
            url += "?";
            if (hasType) {
                url += "targetType=" + targetType;
                model.addAttribute("selectedType", targetType);
            } else {
                model.addAttribute("selectedType", "ALL");
            }
            if (hasId) {
                if (hasType) {
                    url += "&";
                }
                url += "targetId=" + targetId;
                model.addAttribute("selectedId", targetId);
            }
        } else {
            model.addAttribute("selectedType", "ALL");
        }

        AuditEventDto[] events = restTemplate.getForObject(url, AuditEventDto[].class);
        model.addAttribute("events", events != null ? Arrays.asList(events) : new ArrayList<>());
        return "admin-audit";
    }

    @GetMapping("/distributor/dashboard")
    @PreAuthorize("hasRole('DISTRIBUTOR')")
    public String distributorDashboard(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        DistributorViewDto distributor = userService.getDistributorViewByUserId(currentUser.getId());
        model.addAttribute("distributor", distributor);
        return "distributor-dashboard";
    }

    @GetMapping("/distributor/profile")
    @PreAuthorize("hasRole('DISTRIBUTOR')")
    public String distributorProfileForm(@AuthenticationPrincipal CustomUserDetails currentUser,
                                         Model model,
                                         @ModelAttribute("message") String message,
                                         @ModelAttribute("error") String error) {
        DistributorViewDto distributor = userService.getDistributorViewByUserId(currentUser.getId());
        DistributorProfileDto profile = new DistributorProfileDto();
        profile.setDistributorId(distributor.getDistributorId());
        profile.setUsername(distributor.getUsername());
        profile.setName(distributor.getName());
        profile.setEmail(distributor.getEmail());
        profile.setCity(distributor.getCity());
        profile.setContact(distributor.getContact());
        model.addAttribute("profile", profile);
        model.addAttribute("message", message);
        model.addAttribute("error", error);
        return "distributor-profile";
    }

    @PostMapping("/distributor/profile")
    @PreAuthorize("hasRole('DISTRIBUTOR')")
    public String distributorProfileSubmit(@AuthenticationPrincipal CustomUserDetails currentUser,
                                           @ModelAttribute DistributorProfileDto profile,
                                           RedirectAttributes redirectAttributes) {
        try {
            userService.updateDistributorProfile(currentUser.getId(), profile);
            redirectAttributes.addFlashAttribute("message", "Profile updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users/distributor/profile";
    }

    @GetMapping("/distributor/products")
    @PreAuthorize("hasRole('DISTRIBUTOR')")
    public String distributorProductList(Model model,
                                         @ModelAttribute("message") String message,
                                         @ModelAttribute("error") String error) {
        ProductDto[] products = restTemplate.getForObject(productServiceUrl + "/api/products/active", ProductDto[].class);
        model.addAttribute("products", Arrays.asList(products));
        model.addAttribute("message", message);
        model.addAttribute("error", error);
        return "distributor-products";
    }

    @PostMapping("/distributor/orders/{id}/delete")
    @PreAuthorize("hasRole('DISTRIBUTOR')")
    public String deleteDistributorOrder(@PathVariable Long id,
                                         @AuthenticationPrincipal CustomUserDetails currentUser,
                                         RedirectAttributes redirectAttributes) {
        OrderDto order = restTemplate.getForObject(orderServiceUrl + "/api/orders/" + id, OrderDto.class);
        if (order == null || !currentUser.getId().equals(order.getDistributorId())) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete this order.");
            return "redirect:/users/distributor/orders";
        }
        restTemplate.delete(orderServiceUrl + "/api/orders/" + id);
        redirectAttributes.addFlashAttribute("message", "Order deleted successfully.");
        return "redirect:/users/distributor/orders";
    }

    @PostMapping("/admin/orders/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteAdminOrder(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        restTemplate.delete(orderServiceUrl + "/api/orders/" + id);
        redirectAttributes.addFlashAttribute("message", "Order deleted successfully.");
        return "redirect:/users/admin/orders";
    }

    @PostMapping("/admin/orders/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public String approveAdminOrder(@PathVariable Long id,
                                    @RequestParam(value = "adminMessage", required = false) String adminMessage,
                                    RedirectAttributes redirectAttributes) {
        try {
            OrderDecisionDto decision = new OrderDecisionDto();
            decision.setStatus("APPROVED");
            decision.setAdminMessage(adminMessage);
            restTemplate.postForObject(orderServiceUrl + "/api/orders/" + id + "/decision", decision, OrderDto.class);
            redirectAttributes.addFlashAttribute("message", "Order approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Unable to approve order: " + e.getMessage());
        }
        return "redirect:/users/admin/orders";
    }

    @PostMapping("/admin/orders/{id}/deny")
    @PreAuthorize("hasRole('ADMIN')")
    public String denyAdminOrder(@PathVariable Long id,
                                 @RequestParam(value = "adminMessage", required = false) String adminMessage,
                                 RedirectAttributes redirectAttributes) {
        try {
            OrderDecisionDto decision = new OrderDecisionDto();
            decision.setStatus("DENIED");
            decision.setAdminMessage(adminMessage);
            restTemplate.postForObject(orderServiceUrl + "/api/orders/" + id + "/decision", decision, OrderDto.class);
            redirectAttributes.addFlashAttribute("message", "Order denied successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Unable to deny order: " + e.getMessage());
        }
        return "redirect:/users/admin/orders";
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
    public String createOrderForm(@AuthenticationPrincipal CustomUserDetails currentUser,
                                  @RequestParam(value = "productId", required = false) Long productId,
                                  Model model) {
        OrderCreateDto dto = new OrderCreateDto();
        dto.setDistributorId(currentUser.getId());
        model.addAttribute("orderCreateDto", dto);
        ProductDto[] products = restTemplate.getForObject(productServiceUrl + "/api/products/active", ProductDto[].class);
        model.addAttribute("products", Arrays.asList(products));
        model.addAttribute("selectedProductId", productId);
        return "distributor-create-order";
    }

    @PostMapping("/distributor/order/create")
    @PreAuthorize("hasRole('DISTRIBUTOR')")
    public String createOrderSubmit(@ModelAttribute OrderCreateDto orderCreateDto,
                                    @RequestParam(value = "productIds", required = false) List<Long> productIds,
                                    @RequestParam(value = "quantities", required = false) List<Integer> quantities,
                                    RedirectAttributes redirectAttributes) {
        if (productIds == null || productIds.isEmpty() || quantities == null || quantities.isEmpty() || productIds.size() != quantities.size()) {
            redirectAttributes.addFlashAttribute("error", "Please select at least one product with quantity.");
            return "redirect:/users/distributor/order/create";
        }
        if (orderCreateDto.getCustomerName() == null || orderCreateDto.getCustomerName().isBlank()
                || orderCreateDto.getCustomerPhone() == null || orderCreateDto.getCustomerPhone().isBlank()
                || orderCreateDto.getShippingAddress() == null || orderCreateDto.getShippingAddress().isBlank()
                || orderCreateDto.getShippingCity() == null || orderCreateDto.getShippingCity().isBlank()
                || orderCreateDto.getShippingPostalCode() == null || orderCreateDto.getShippingPostalCode().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Please fill in all shipping details.");
            return "redirect:/users/distributor/order/create";
        }

        orderCreateDto.setStatus("PENDING_APPROVAL");
        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            Integer quantity = quantities.get(i);
            if (productId == null || quantity == null || quantity < 1) {
                continue;
            }
            OrderCreateDto itemOrder = new OrderCreateDto();
            itemOrder.setDistributorId(orderCreateDto.getDistributorId());
            itemOrder.setProductId(productId);
            itemOrder.setQuantity(quantity);
            itemOrder.setCustomerName(orderCreateDto.getCustomerName());
            itemOrder.setCustomerPhone(orderCreateDto.getCustomerPhone());
            itemOrder.setShippingAddress(orderCreateDto.getShippingAddress());
            itemOrder.setShippingCity(orderCreateDto.getShippingCity());
            itemOrder.setShippingPostalCode(orderCreateDto.getShippingPostalCode());
            itemOrder.setCustomMessage(orderCreateDto.getCustomMessage());
            itemOrder.setFulfillmentTime(orderCreateDto.getFulfillmentTime());
            itemOrder.setStatus("PENDING_APPROVAL");
            restTemplate.postForObject(orderServiceUrl + "/api/orders", itemOrder, OrderDto.class);
        }

        redirectAttributes.addFlashAttribute("message", "Order placed and sent for admin approval.");
        return "redirect:/users/distributor/orders";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRIBUTOR')")
    public String getUserById(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails currentUser,
                              Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("currentUser", currentUser);
        String backUrl = currentUser.getRole() == User.Role.ADMIN ? "/users/admin/dashboard" : "/users/distributor/dashboard";
        model.addAttribute("backUrl", backUrl);
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