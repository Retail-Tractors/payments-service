package tractors.retail.payments.service.webhook;

import org.springframework.beans.factory.annotation.Autowired;
import tractors.retail.payments.service.config.StripeConfig;
import tractors.retail.payments.service.services.StripeOnBoardingService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stripe/webhook")
public class StripeWebhookController {

    private final StripeOnBoardingService stripeService;

    @Autowired
    private StripeConfig stripeConfig;

    public StripeWebhookController(StripeOnBoardingService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        String webhookSecret = stripeConfig.getWebhookSecret();
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            if ("account.updated".equals(event.getType())) {
                var account = (com.stripe.model.Account) event.getDataObjectDeserializer()
                        .getObject()
                        .orElse(null);
                if (account != null && Boolean.TRUE.equals(account.getDetailsSubmitted())) {
                    stripeService.markAccountVerified(account.getId());
                }
            }

            return ResponseEntity.ok("Success");
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(400).body("Invalid signature");
        }
    }
}
