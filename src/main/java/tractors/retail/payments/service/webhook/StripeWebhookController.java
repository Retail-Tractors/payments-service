package tractors.retail.payments.service.webhook;

import org.springframework.beans.factory.annotation.Autowired;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Account;
import com.stripe.model.Capability;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import tractors.retail.payments.service.config.StripeConfig;
import tractors.retail.payments.service.services.StripeOnBoardingService;
import tractors.retail.payments.service.services.PaymentsService;
import tractors.retail.payments.service.services.PostService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments/stripe/webhook")
public class StripeWebhookController {

    private final StripeOnBoardingService stripeService;
    private final PaymentsService paymentsService;
    private final RestTemplate restTemplate;

    @Autowired
    private StripeConfig stripeConfig;
    private final PostService postService;

    public StripeWebhookController(StripeOnBoardingService stripeService, PaymentsService paymentsService, PostService postService) {
        this.stripeService = stripeService;
        this.paymentsService = paymentsService;
        this.postService = postService;
        this.restTemplate = new RestTemplate(); // Cliente HTTP para chamar o Bookings
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        String webhookSecret = stripeConfig.getWebhookSecret();
        Event event;
        try {
            // Tenta validar a assinatura do evento (segurança)
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(400).body("Invalid signature");
        }

        // System.out.println("Webhook recebido: " + event.getType());

        // Lida com os diferentes tipos de eventos
        switch (event.getType()) {
            case "account.updated":
                Account account = (Account) event.getDataObjectDeserializer().getObject().orElse(null);
                handleAccountUpdated(account);
                return ResponseEntity.ok("Success");

            case "capability.updated":
                Capability capability = (Capability) event.getDataObjectDeserializer().getObject().orElse(null);
                handleCapabilityUpdated(capability);
                return ResponseEntity.ok("Success");

            case "payment_intent.succeeded":
                PaymentIntent paymentIntentSuccess = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                handlePaymentIntentSucceeded(paymentIntentSuccess);
                return ResponseEntity.ok("Success");

            case "payment_intent.payment_failed":
                PaymentIntent paymentIntentFailed = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                handlePaymentIntentFailed(paymentIntentFailed);
                return ResponseEntity.ok("Success");

            default:
                break;
        }

        return ResponseEntity.ok("Success");
    }

    private void handleAccountUpdated(Account account) {
        if (account == null) {return;}
        var requirements = account.getRequirements();
        if (requirements == null) {return;}
        String accountId = account.getId();

        if (Boolean.TRUE.equals(account.getDetailsSubmitted())) {
            stripeService.markAccountVerified(accountId);
        }
        if (!requirements.getCurrentlyDue().isEmpty()) {
            stripeService.markAccountPendingVerification(accountId);
        }
        if (!requirements.getEventuallyDue().isEmpty()) {
            stripeService.markAccountPendingVerification(accountId);
        }
        if (!requirements.getPastDue().isEmpty()) {
            stripeService.markAccountDisabled(accountId);
        }
    }

    private void handleCapabilityUpdated(Capability capability) {
        if (capability == null) {return;}

        String accountId = capability.getAccount();
        String status = capability.getStatus(); 

        switch (status) {
            case "active":
                stripeService.markAccountActive(accountId);
                break;
            case "inactive":
            case "disabled":
                stripeService.markAccountDisabled(accountId);
                break;
            default:
                break;
        }
    }

    private void handlePaymentIntentSucceeded(PaymentIntent paymentIntent) {
        if (paymentIntent == null) return;

        String paymentIntentId = paymentIntent.getId();
        Long amount = paymentIntent.getAmountReceived();
        String currency = paymentIntent.getCurrency();

        // 1. Tenta obter o booking_id dos metadados para atualizar a reserva
        Map<String, String> metadata = paymentIntent.getMetadata();
        String bookingId = metadata.get("booking_id");
        
        if (bookingId != null) {
            notifyBookingService(bookingId, paymentIntentId, "PAID", "Payment successful via Stripe");
        } else {
            System.out.println("AVISO: booking_id não encontrado nos metadados do Stripe.");
        }

        // 2. Lógica existente para guardar histórico na tabela Payments
        String postIdStr = metadata.get("post_id");
        if (postIdStr != null) {
            try {
                Long postId = Long.valueOf(postIdStr);
                String buyerEmail = paymentIntent.getReceiptEmail();
                paymentsService.createPayment(postId, buyerEmail, paymentIntentId, amount, currency);

                postService.updatePostStatus(postId, "COMPLETED");

            } catch (Exception e) {
                System.out.println("Failed to create payment record: " + e.getMessage());
            }
        }
    }

    private void handlePaymentIntentFailed(PaymentIntent paymentIntent) {
        if (paymentIntent == null) return;

        String paymentIntentId = paymentIntent.getId();
        String errorMessage = "Unknown error";
        
        if (paymentIntent.getLastPaymentError() != null) {
            errorMessage = paymentIntent.getLastPaymentError().getMessage();
        }
        
        Map<String, String> metadata = paymentIntent.getMetadata();
        String bookingId = metadata.get("booking_id");

        if (bookingId != null) {
            notifyBookingService(bookingId, paymentIntentId, "FAILED", errorMessage);
        } else {
            System.out.println("AVISO: Falha no pagamento mas booking_id não encontrado nos metadados.");
        }

        String postIdStr = metadata.get("post_id");
        if (postIdStr == null) {
            Long postId = Long.valueOf(postIdStr);
            postService.updatePostStatus(postId, "FAILED");
        }
    }

    // --- Comunicação HTTP com o Bookings Service ---
    private void notifyBookingService(String bookingId, String paymentId, String status, String notes) {
        // NOTA: O URL assume que tens o bookings-service montado com o prefixo '/bookings'
        // Se a rota for direta, remove o primeiro /bookings/
        String url = "http://bookings-service:3002/bookings/internal/" + bookingId + "/payment";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("paymentId", paymentId);
        requestBody.put("status", status);
        requestBody.put("notes", notes);

        try {
            System.out.println("A notificar Bookings Service em: " + url);
            restTemplate.postForEntity(url, requestBody, String.class);
            System.out.println("Bookings Service notificado com sucesso.");
        } catch (Exception e) {
            System.err.println("ERRO ao chamar Bookings Service: " + e.getMessage());
        }
    }
}