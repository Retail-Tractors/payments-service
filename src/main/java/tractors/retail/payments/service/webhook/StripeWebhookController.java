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

import tractors.retail.payments.service.config.StripeConfig;
import tractors.retail.payments.service.services.StripeOnBoardingService;
import tractors.retail.payments.service.services.PaymentsService;

@RestController
@RequestMapping("/api/stripe/webhook")
public class StripeWebhookController {

    private final StripeOnBoardingService stripeService;
    private final PaymentsService paymentsService;

    @Autowired
    private StripeConfig stripeConfig;

    public StripeWebhookController(StripeOnBoardingService stripeService,  PaymentsService paymentsService) {
        this.stripeService = stripeService;
        this.paymentsService = paymentsService;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        String webhookSecret = stripeConfig.getWebhookSecret();
        Event event;
        try {
            // Try to read webhook event
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(400).body("Invalid signature");
        }
        // Handle different event types
        switch (event.getType()) {
            case "account.updated":
                // create var account received from the webhook, if fails account=null
                Account account = (Account) event.getDataObjectDeserializer().getObject().orElse(null);
                handleAccountUpdated(account);
                return ResponseEntity.ok("Success");

            case "capability.updated":
                Capability capability = (Capability) event.getDataObjectDeserializer().getObject().orElse(null);
                handleCapabilityUpdated(capability);
                return ResponseEntity.ok("Success");

            case "payment_intent.succeeded":
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                handlePaymentIntentSucceeded(paymentIntent);
                return ResponseEntity.ok("Success");

            default:
                break;
        }

        // default response when the event isnt handled
        return ResponseEntity.ok("Success");
    }

    private void handleAccountUpdated(Account account) {
        if (account == null) {return;}
        // requirements contains the documents/info this account is missing
        var requirements = account.getRequirements();
        if (requirements == null) {return;}
        String accountId = account.getId();

        // mark account as verified in our db when stripe approves account.
        // in production we would need to check if requirements is empty.
        // in test mode we cant add id so we mark the account as verified regardless of missing documents
        if (Boolean.TRUE.equals(account.getDetailsSubmitted())) {
            stripeService.markAccountVerified(accountId);
        }
        // handle missing documents - must upload soon
        if (!requirements.getCurrentlyDue().isEmpty()) {
            // TODO: send "Please upload your ID" email to client when email-service is implemented
            stripeService.markAccountPendingVerification(accountId);
        }
        // handle missing documents - verification will be required soon
        if (!requirements.getEventuallyDue().isEmpty()) {
            // TODO: send "You will need to upload your ID to avoid account suspension" email to client when email-service is implemented
            stripeService.markAccountPendingVerification(accountId);
        }
        // handle missing documents - payouts/transfers blocked
        if (!requirements.getPastDue().isEmpty()) {
            // TODO: send "Your account payouts/transfers are blocked." email to client when email-service is implemented
            stripeService.markAccountDisabled(accountId);
        }
    }

    private void handleCapabilityUpdated(Capability capability) {
        if (capability == null) {return;}

        String accountId = capability.getAccount();
        String status = capability.getStatus(); // active, inactive or disabled
        // in the future we could handle transfers/payouts differently.
        // capability.getId() returns the type of capability updated
        // for test reasons we will simplify and just restrict the account when one of these is disabled/inactive

        switch (status) {
            case "active":
                // capabilities restored
                stripeService.markAccountActive(accountId);
                break;

            case "inactive":
            case "disabled":
                // restrict account when stripe removes their transfers/payouts capability
                stripeService.markAccountDisabled(accountId);
                break;

            default:
                break;
        }
    }

    private void handlePaymentIntentSucceeded(PaymentIntent paymentIntent) {
        if (paymentIntent == null) {
            System.out.print("paymentIntent is null");
            return;
        }

        String paymentIntentId = paymentIntent.getId();
        Long amount = paymentIntent.getAmountReceived();
        String currency = paymentIntent.getCurrency();

        String postIdStr = paymentIntent.getMetadata().get("post_id");
        if (postIdStr == null) {
            System.out.print("postId is null");
            return;
        }
        Long postId = Long.valueOf(postIdStr);

        String buyerEmail = paymentIntent.getReceiptEmail();
        try {
            paymentsService.createPayment(
                    postId,
                    buyerEmail,
                    paymentIntentId,
                    amount,
                    currency
            );
        } catch (Exception e) {
            System.out.println("Failed to create payment: " + e.getMessage());
        }

    }
}
