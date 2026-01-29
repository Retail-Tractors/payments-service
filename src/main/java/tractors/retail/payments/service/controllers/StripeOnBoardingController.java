package tractors.retail.payments.service.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import tractors.retail.payments.service.services.StripeOnBoardingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/payments/stripe")
@Tag(
    name = "Stripe Onboarding",
    description = "Endpoints for onboarding owners to Stripe connected accounts"
)
public class StripeOnBoardingController {

    private final StripeOnBoardingService stripeService;

    public StripeOnBoardingController(StripeOnBoardingService stripeService) {
        this.stripeService = stripeService;
    }

    @Operation(
        summary = "Onboard owner to Stripe",
        description = "Creates a Stripe connected account for the authenticated owner and returns an onboarding URL."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Onboarding link created successfully",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(
                    description = "Stripe onboarding URL",
                    example = "https://connect.stripe.com/setup/s/xxxxxxxx"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Error while creating Stripe account or onboarding link",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(
                    description = "Error message",
                    example = "Error: Invalid email"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized – missing or invalid JWT"
        )
    })
    @PostMapping("/onboard")
    public ResponseEntity<?> onboardOwner(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        try {
            Integer userId = Integer.parseInt(jwt.getSubject());
            String email = (String) jwt.getClaim("email");
            String name = (String) jwt.getClaim("name");

            String accountId = stripeService.createConnectedAccount(userId, email, name);
            String onboardingLink = stripeService.generateOnboardingLink(accountId);
            return ResponseEntity.ok(onboardingLink);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Onboarding success page",
        description = "Simple HTML page shown to the user when Stripe onboarding completes successfully."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "HTML success page returned",
            content = @Content(mediaType = "text/html")
        )
    })
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
                <p>Your Stripe account setup is done. You can safely close this page and return to the app.</p>
              </body>
            </html>
        """;
        return ResponseEntity.ok().header("Content-type", "text/html").body(html);
    }
}
