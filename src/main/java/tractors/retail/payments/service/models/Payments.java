package tractors.retail.payments.service.models;

import jakarta.persistence.*;
import lombok.*;

import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name ="payments")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Payment record created after a successful purchase")
public class Payments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the payment", example = "1")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id", nullable = false)
    @Schema(description = "Post that was purchased")
    private Post post;

    @Column(name = "buyer_email")
    @Schema(description = "Email of the buyer used during checkout", example = "buyer@example.com")
    private String buyerEmail;

    @Column(name = "stripe_payment_intent_id")
    @Schema(description = "Stripe Payment Intent ID associated with this payment", example = "pi_3PXXXXXXX")
    private String stripePaymentIntentId;

    @Schema(description = "Total payment amount in minor units (e.g. cents)", example = "2500000")
    private Long amount;

    @Schema(description = "Currency code in ISO-4217 format", example = "EUR")
    private String currency;

    @Schema(description = "Current payment status", example = "SUCCEEDED")
    private String status;

    @Column(name = "created_at", updatable = false, insertable = false)
    @Schema(description = "Timestamp when the payment was created", example = "2025-01-10T12:30:00")
    private java.time.LocalDateTime createdAt;
}
