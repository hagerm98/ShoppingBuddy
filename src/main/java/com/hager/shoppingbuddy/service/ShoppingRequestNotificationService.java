package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.entity.ShoppingRequest;
import com.hager.shoppingbuddy.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingRequestNotificationService {

    private final EmailService emailService;

    @Value("${shoppingbuddy.base-url}")
    private String baseUrl;

    public void notifyShoppingRequestCreated(ShoppingRequest shoppingRequest) {
        try {
            User customer = shoppingRequest.getCustomer().getUser();
            String subject = "Shopping Request Created - #" + shoppingRequest.getId();
            String emailBody = buildShoppingRequestCreatedEmail(shoppingRequest, customer);

            emailService.send(customer.getEmail(), subject, emailBody);
            log.info("Shopping request created notification sent to customer: {}", customer.getEmail());
        } catch (Exception e) {
            log.error("Failed to send shopping request created notification: {}", e.getMessage());
        }
    }

    public void notifyShoppingRequestAccepted(ShoppingRequest shoppingRequest) {
        try {
            User customer = shoppingRequest.getCustomer().getUser();
            User shopper = shoppingRequest.getShopper().getUser();

            String customerSubject = "Your Shopping Request Has Been Accepted - #" + shoppingRequest.getId();
            String customerEmailBody = buildShoppingRequestAcceptedCustomerEmail(shoppingRequest, customer, shopper);
            emailService.send(customer.getEmail(), customerSubject, customerEmailBody);

            String shopperSubject = "Shopping Request Accepted - #" + shoppingRequest.getId();
            String shopperEmailBody = buildShoppingRequestAcceptedShopperEmail(shoppingRequest, shopper, customer);
            emailService.send(shopper.getEmail(), shopperSubject, shopperEmailBody);

            log.info("Shopping request accepted notifications sent for request: {}", shoppingRequest.getId());
        } catch (Exception e) {
            log.error("Failed to send shopping request accepted notifications: {}", e.getMessage());
        }
    }

    public void notifyShoppingStarted(ShoppingRequest shoppingRequest) {
        try {
            User customer = shoppingRequest.getCustomer().getUser();
            User shopper = shoppingRequest.getShopper().getUser();

            String subject = "Shopping Started for Your Request - #" + shoppingRequest.getId();
            String emailBody = buildShoppingStartedEmail(shoppingRequest, customer, shopper);

            emailService.send(customer.getEmail(), subject, emailBody);
            log.info("Shopping started notification sent to customer: {}", customer.getEmail());
        } catch (Exception e) {
            log.error("Failed to send shopping started notification: {}", e.getMessage());
        }
    }

    public void notifyShoppingCompleted(ShoppingRequest shoppingRequest) {
        try {
            User customer = shoppingRequest.getCustomer().getUser();
            User shopper = shoppingRequest.getShopper().getUser();

            String customerSubject = "Your Shopping Has Been Completed - #" + shoppingRequest.getId();
            String customerEmailBody = buildShoppingCompletedCustomerEmail(shoppingRequest, customer, shopper);
            emailService.send(customer.getEmail(), customerSubject, customerEmailBody);

            String shopperSubject = "Shopping Completed - #" + shoppingRequest.getId();
            String shopperEmailBody = buildShoppingCompletedShopperEmail(shoppingRequest, shopper, customer);
            emailService.send(shopper.getEmail(), shopperSubject, shopperEmailBody);

            log.info("Shopping completed notifications sent for request: {}", shoppingRequest.getId());
        } catch (Exception e) {
            log.error("Failed to send shopping completed notifications: {}", e.getMessage());
        }
    }

    public void notifyShoppingRequestCancelled(ShoppingRequest shoppingRequest, String cancelledBy) {
        try {
            User customer = shoppingRequest.getCustomer().getUser();
            User shopper = shoppingRequest.getShopper() != null ? shoppingRequest.getShopper().getUser() : null;

            if (!customer.getEmail().equals(cancelledBy)) {
                String subject = "Shopping Request Cancelled - #" + shoppingRequest.getId();
                String emailBody = buildShoppingRequestCancelledEmail(shoppingRequest, customer, true, shopper);
                emailService.send(customer.getEmail(), subject, emailBody);
            }

            if (shopper != null && !shopper.getEmail().equals(cancelledBy)) {
                String subject = "Shopping Request Cancelled - #" + shoppingRequest.getId();
                String emailBody = buildShoppingRequestCancelledEmail(shoppingRequest, shopper, false, customer);
                emailService.send(shopper.getEmail(), subject, emailBody);
            }

            log.info("Shopping request cancelled notifications sent for request: {}", shoppingRequest.getId());
        } catch (Exception e) {
            log.error("Failed to send shopping request cancelled notifications: {}", e.getMessage());
        }
    }

    public void notifyShoppingRequestUpdated(ShoppingRequest shoppingRequest) {
        try {
            User customer = shoppingRequest.getCustomer().getUser();

            if (shoppingRequest.getShopper() != null) {
                User shopper = shoppingRequest.getShopper().getUser();
                String subject = "Shopping Request Updated - #" + shoppingRequest.getId();
                String emailBody = buildShoppingRequestUpdatedEmail(shoppingRequest, shopper, customer);
                emailService.send(shopper.getEmail(), subject, emailBody);
                log.info("Shopping request updated notification sent to shopper: {}", shopper.getEmail());
            }
        } catch (Exception e) {
            log.error("Failed to send shopping request updated notification: {}", e.getMessage());
        }
    }

    public void notifyShoppingRequestAbandoned(ShoppingRequest shoppingRequest, String shopperEmail) {
        try {
            User customer = shoppingRequest.getCustomer().getUser();

            String subject = "Shopping Request Available Again - #" + shoppingRequest.getId();
            String emailBody = buildShoppingRequestAbandonedEmail(shoppingRequest, customer, shopperEmail);

            emailService.send(customer.getEmail(), subject, emailBody);
            log.info("Shopping request abandoned notification sent to customer: {}", customer.getEmail());
        } catch (Exception e) {
            log.error("Failed to send shopping request abandoned notification: {}", e.getMessage());
        }
    }

    private String buildShoppingRequestCreatedEmail(ShoppingRequest request, User customer) {
        return String.format("""
            <html>
            <body>
                <h2>Shopping Request Created Successfully</h2>
                <p>Hello %s,</p>
                <p>Your shopping request has been created successfully and is now available for shoppers to accept.</p>
               \s
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <h3>Request Details</h3>
                    <p><strong>Request ID:</strong> #%d</p>
                    <p><strong>Store:</strong> %s</p>
                    <p><strong>Store Address:</strong> %s</p>
                    <p><strong>Delivery Address:</strong> %s</p>
                    <p><strong>Estimated Items Price:</strong> €%.2f</p>
                    <p><strong>Delivery Fee:</strong> €%.2f</p>
                    <p><strong>Status:</strong> %s</p>
                </div>
               \s
                <p>You can track your request and communicate with shoppers once it's accepted.</p>
                <p><a href="%s/shopping-requests/%d" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View Request</a></p>
               \s
                <p>Best regards,<br>The ShoppingBuddy Team</p>
            </body>
            </html>
           \s""",
            customer.getFirstName(),
            request.getId(),
            request.getStoreName() != null ? request.getStoreName() : "Not specified",
            request.getStoreAddress() != null ? request.getStoreAddress() : "Not specified",
            request.getDeliveryAddress(),
            request.getEstimatedItemsPrice(),
            request.getDeliveryFee(),
            request.getStatus().name(),
            baseUrl,
            request.getId()
        );
    }

    private String buildShoppingRequestAcceptedCustomerEmail(ShoppingRequest request, User customer, User shopper) {
        return String.format("""
            <html>
            <body>
                <h2>Great News! Your Shopping Request Has Been Accepted</h2>
                <p>Hello %s,</p>
                <p>Your shopping request #%d has been accepted by a shopper.</p>
               \s
                <div style="background-color: #d4edda; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #28a745;">
                    <h3>Shopper Information</h3>
                    <p><strong>Shopper:</strong> %s %s</p>
                    <p><strong>Email:</strong> %s</p>
                </div>
               \s
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <h3>Request Details</h3>
                    <p><strong>Store:</strong> %s</p>
                    <p><strong>Store Address:</strong> %s</p>
                    <p><strong>Delivery Address:</strong> %s</p>
                    <p><strong>Estimated Items Price:</strong> €%.2f</p>
                    <p><strong>Delivery Fee:</strong> €%.2f</p>
                </div>
               \s
                <p>Your shopper will start shopping soon. You can communicate with them through the chat feature.</p>
                <p><a href="%s/shopping-requests/%d" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View Request & Chat</a></p>
               \s
                <p>Best regards,<br>The ShoppingBuddy Team</p>
            </body>
            </html>
           \s""",
            customer.getFirstName(),
            request.getId(),
            shopper.getFirstName(),
            shopper.getLastName(),
            shopper.getEmail(),
            request.getStoreName() != null ? request.getStoreName() : "Not specified",
            request.getStoreAddress() != null ? request.getStoreAddress() : "Not specified",
            request.getDeliveryAddress(),
            request.getEstimatedItemsPrice(),
            request.getDeliveryFee(),
            baseUrl,
            request.getId()
        );
    }

    private String buildShoppingRequestAcceptedShopperEmail(ShoppingRequest request, User shopper, User customer) {
        return String.format("""
            <html>
            <body>
                <h2>Shopping Request Accepted</h2>
                <p>Hello %s,</p>
                <p>You have successfully accepted shopping request #%d.</p>
               \s
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <h3>Customer Information</h3>
                    <p><strong>Customer:</strong> %s %s</p>
                    <p><strong>Email:</strong> %s</p>
                </div>
               \s
                <div style="background-color: #fff3cd; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #ffc107;">
                    <h3>Request Details</h3>
                    <p><strong>Store:</strong> %s</p>
                    <p><strong>Store Address:</strong> %s</p>
                    <p><strong>Delivery Address:</strong> %s</p>
                    <p><strong>Estimated Items Price:</strong> €%.2f</p>
                    <p><strong>Delivery Fee:</strong> €%.2f</p>
                </div>
               \s
                <p>Please start shopping when you're ready and keep the customer updated through the chat feature.</p>
                <p><a href="%s/shopping-requests/%d" style="background-color: #28a745; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Start Shopping</a></p>
               \s
                <p>Best regards,<br>The ShoppingBuddy Team</p>
            </body>
            </html>
           \s""",
            shopper.getFirstName(),
            request.getId(),
            customer.getFirstName(),
            customer.getLastName(),
            customer.getEmail(),
            request.getStoreName() != null ? request.getStoreName() : "Not specified",
            request.getStoreAddress() != null ? request.getStoreAddress() : "Not specified",
            request.getDeliveryAddress(),
            request.getEstimatedItemsPrice(),
            request.getDeliveryFee(),
            baseUrl,
            request.getId()
        );
    }

    private String buildShoppingStartedEmail(ShoppingRequest request, User customer, User shopper) {
        return String.format("""
            <html>
            <body>
                <h2>Shopping Started for Your Request</h2>
                <p>Hello %s,</p>
                <p>%s %s has started shopping for your request #%d.</p>
               \s
                <div style="background-color: #d1ecf1; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #17a2b8;">
                    <h3>Shopping in Progress</h3>
                    <p>Your shopper is now collecting your items. They may contact you if they have questions about specific products or substitutions.</p>
                </div>
               \s
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <h3>Request Details</h3>
                    <p><strong>Store:</strong> %s</p>
                    <p><strong>Store Address:</strong> %s</p>
                    <p><strong>Delivery Address:</strong> %s</p>
                    <p><strong>Estimated Items Price:</strong> €%.2f</p>
                    <p><strong>Delivery Fee:</strong> €%.2f</p>
                </div>
               \s
                <p>You can chat with your shopper to stay updated on the progress.</p>
                <p><a href="%s/shopping-requests/%d" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View Progress & Chat</a></p>
               \s
                <p>Best regards,<br>The ShoppingBuddy Team</p>
            </body>
            </html>
           \s""",
            customer.getFirstName(),
            shopper.getFirstName(),
            shopper.getLastName(),
            request.getId(),
            request.getStoreName() != null ? request.getStoreName() : "Not specified",
            request.getStoreAddress() != null ? request.getStoreAddress() : "Not specified",
            request.getDeliveryAddress(),
            request.getEstimatedItemsPrice(),
            request.getDeliveryFee(),
            baseUrl,
            request.getId()
        );
    }

    private String buildShoppingCompletedCustomerEmail(ShoppingRequest request, User customer, User shopper) {
        return String.format("""
            <html>
            <body>
                <h2>Your Shopping Has Been Completed!</h2>
                <p>Hello %s,</p>
                <p>Great news! %s %s has completed shopping for your request #%d.</p>
               \s
                <div style="background-color: #d4edda; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #28a745;">
                    <h3>Shopping Completed</h3>
                    <p>Your items have been purchased and delivered by your shopper. If you have any further queries please contact our support</p>
                </div>
               \s
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <h3>Request Details</h3>
                    <p><strong>Store:</strong> %s</p>
                    <p><strong>Store Address:</strong> %s</p>
                    <p><strong>Delivery Address:</strong> %s</p>
                    <p><strong>Estimated Items Price:</strong> €%.2f</p>
                    <p><strong>Delivery Fee:</strong> €%.2f</p>
                </div>
               \s
                <p>Thanks for shopping with us!</p>
                <p><a href="%s/shopping-requests/%d" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View Request Details</a></p>
               \s
                <p>Thank you for using ShoppingBuddy!</p>
                <p>Best regards,<br>The ShoppingBuddy Team</p>
            </body>
            </html>
           \s""",
            customer.getFirstName(),
            shopper.getFirstName(),
            shopper.getLastName(),
            request.getId(),
            request.getStoreName() != null ? request.getStoreName() : "Not specified",
            request.getStoreAddress() != null ? request.getStoreAddress() : "Not specified",
            request.getDeliveryAddress(),
            request.getEstimatedItemsPrice(),
            request.getDeliveryFee(),
            baseUrl,
            request.getId()
        );
    }

    private String buildShoppingCompletedShopperEmail(ShoppingRequest request, User shopper, User customer) {
        return String.format("""
            <html>
            <body>
                <h2>Shopping Request Completed</h2>
                <p>Hello %s,</p>
                <p>You have successfully completed shopping request #%d for %s %s.</p>
               \s
                <div style="background-color: #d4edda; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #28a745;">
                    <h3>Well Done!</h3>
                    <p>Thank you for completing this shopping request. Please proceed with the delivery to the customer.</p>
                </div>
               \s
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <h3>Request Details</h3>
                    <p><strong>Store:</strong> %s</p>
                    <p><strong>Store Address:</strong> %s</p>
                    <p><strong>Delivery Address:</strong> %s</p>
                    <p><strong>Customer Contact:</strong> %s</p>
                </div>
               \s
                <p>Remember to handle the delivery with care and confirm completion with the customer.</p>
                <p><a href="%s/shopping-requests/%d" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View Request Details</a></p>
               \s
                <p>Thank you for your service!</p>
                <p>Best regards,<br>The ShoppingBuddy Team</p>
            </body>
            </html>
           \s""",
            shopper.getFirstName(),
            request.getId(),
            customer.getFirstName(),
            customer.getLastName(),
            request.getStoreName() != null ? request.getStoreName() : "Not specified",
            request.getStoreAddress() != null ? request.getStoreAddress() : "Not specified",
            request.getDeliveryAddress(),
            customer.getEmail(),
            baseUrl,
            request.getId()
        );
    }

    private String buildShoppingRequestCancelledEmail(ShoppingRequest request, User recipient, boolean isCustomer, User otherParty) {
        String otherRole = isCustomer ? "shopper" : "customer";

        return String.format("""
            <html>
            <body>
                <h2>Shopping Request Cancelled</h2>
                <p>Hello %s,</p>
                <p>Shopping request #%d has been cancelled by the %s.</p>
               \s
                <div style="background-color: #f8d7da; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #dc3545;">
                    <h3>Request Cancelled</h3>
                    <p>This shopping request is no longer active.</p>
                    %s
                </div>
               \s
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <h3>Request Details</h3>
                    <p><strong>Request ID:</strong> #%d</p>
                    <p><strong>Store:</strong> %s</p>
                    <p><strong>Store Address:</strong> %s</p>
                    <p><strong>Delivery Address:</strong> %s</p>
                    <p><strong>Estimated Items Price:</strong> €%.2f</p>
                </div>
               \s
                <p>%s</p>
               \s
                <p>Thank you for using ShoppingBuddy.</p>
                <p>Best regards,<br>The ShoppingBuddy Team</p>
            </body>
            </html>
           \s""",
            recipient.getFirstName(),
            request.getId(),
            otherRole,
            otherParty != null ?
                String.format("<p><strong>%s:</strong> %s %s (%s)</p>",
                    otherRole.substring(0, 1).toUpperCase() + otherRole.substring(1),
                    otherParty.getFirstName(),
                    otherParty.getLastName(),
                    otherParty.getEmail()) : "",
            request.getId(),
            request.getStoreName() != null ? request.getStoreName() : "Not specified",
            request.getStoreAddress() != null ? request.getStoreAddress() : "Not specified",
            request.getDeliveryAddress(),
            request.getEstimatedItemsPrice(),
            isCustomer ?
                "We apologize for any inconvenience. You can create a new shopping request anytime." :
                "You are now free to accept other shopping requests."
        );
    }

    private String buildShoppingRequestUpdatedEmail(ShoppingRequest request, User shopper, User customer) {
        return String.format("""
            <html>
            <body>
                <h2>Shopping Request Updated</h2>
                <p>Hello %s,</p>
                <p>The customer %s %s has updated shopping request #%d that you accepted.</p>
               \s
                <div style="background-color: #fff3cd; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #ffc107;">
                    <h3>Request Updated</h3>
                    <p>Please review the updated request details below.</p>
                </div>
               \s
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <h3>Updated Request Details</h3>
                    <p><strong>Store:</strong> %s</p>
                    <p><strong>Store Address:</strong> %s</p>
                    <p><strong>Delivery Address:</strong> %s</p>
                    <p><strong>Estimated Items Price:</strong> €%.2f</p>
                    <p><strong>Delivery Fee:</strong> €%.2f</p>
                </div>
               \s
                <p>Please review the updated items list and contact the customer if you have any questions.</p>
                <p><a href="%s/shopping-requests/%d" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View Updated Request</a></p>
               \s
                <p>Best regards,<br>The ShoppingBuddy Team</p>
            </body>
            </html>
           \s""",
            shopper.getFirstName(),
            customer.getFirstName(),
            customer.getLastName(),
            request.getId(),
            request.getStoreName() != null ? request.getStoreName() : "Not specified",
            request.getStoreAddress() != null ? request.getStoreAddress() : "Not specified",
            request.getDeliveryAddress(),
            request.getEstimatedItemsPrice(),
            request.getDeliveryFee(),
            baseUrl,
            request.getId()
        );
    }

    private String buildShoppingRequestAbandonedEmail(ShoppingRequest request, User customer, String shopperEmail) {
        return String.format("""
            <html>
            <body>
                <h2>Shopping Request Available Again</h2>
                <p>Hello %s,</p>
                <p>Your shopping request #%d is now available for other shoppers to accept.</p>
               \s
                <div style="background-color: #f8d7da; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #dc3545;">
                    <h3>Request Abandoned</h3>
                    <p>The shopper (%s) has abandoned the request. It is now open for other shoppers.</p>
                </div>
               \s
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <h3>Request Details</h3>
                    <p><strong>Request ID:</strong> #%d</p>
                    <p><strong>Store:</strong> %s</p>
                    <p><strong>Store Address:</strong> %s</p>
                    <p><strong>Delivery Address:</strong> %s</p>
                    <p><strong>Estimated Items Price:</strong> €%.2f</p>
                    <p><strong>Delivery Fee:</strong> €%.2f</p>
                    <p><strong>Status:</strong> %s</p>
                </div>
               \s
                <p>You can track your request and communicate with shoppers once it's accepted.</p>
                <p><a href="%s/shopping-requests/%d" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View Request</a></p>
               \s
                <p>Best regards,<br>The ShoppingBuddy Team</p>
            </body>
            </html>
           \s""",
            customer.getFirstName(),
            request.getId(),
            shopperEmail,
            request.getId(),
            request.getStoreName() != null ? request.getStoreName() : "Not specified",
            request.getStoreAddress() != null ? request.getStoreAddress() : "Not specified",
            request.getDeliveryAddress(),
            request.getEstimatedItemsPrice(),
            request.getDeliveryFee(),
            request.getStatus().name(),
            baseUrl,
            request.getId()
        );
    }
}
