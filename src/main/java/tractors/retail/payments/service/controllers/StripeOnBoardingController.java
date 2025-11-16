package tractors.retail.payments.service.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tractors.retail.payments.service.services.StripeOnBoardingService;

@RestController
@RequestMapping("/api/stripe")
public class StripeOnBoardingController {

    private final StripeOnBoardingService stripeService;

    public StripeOnBoardingController(StripeOnBoardingService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/onboard")
    public ResponseEntity<?> onboardOwner(@RequestParam String email,
                                          @RequestParam String name) {
        try {
            String accountId = stripeService.createConnectedAccount(email, name);
            String onboardingLink = stripeService.generateOnboardingLink(accountId);
            return ResponseEntity.ok(onboardingLink);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/success")
    public ResponseEntity<String> handleSuccess() {
        String html = """
            <html>
              <head>
                <title>Stripe Onboarding Complete</title>
                <style>
                  body { font-family: Arial, sans-serif; text-align: center; margin-top: 100px; }
                  h1 { color: #4CAF50; }
                  a { text-decoration: none; color: #2196F3; }
                </style>
              </head>
              <body>
                <h1>Onboarding Complete!</h1>
                <p>Your Stripe account setup is done. You can safely close this page or return to the app.</p>
              </body>
            </html>
        """;
        return ResponseEntity.ok().header("Content-type", "text/html").body(html);
    }
}
