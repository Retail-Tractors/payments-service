package tractors.retail.payments.service.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "sellers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Schema(description = "Seller that owns posts and receives payments via Stripe")
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the seller", example = "1")
    private Long id;

    @Column(name = "userid", nullable = false, unique = true)
    @Schema(
        description = "User ID from users-service that this seller is linked to",
        example = "42"
    )
    private Integer userId;

    @Schema(description = "Name of the seller", example = "Tractor Depot Lda")
    private String name;

    @Schema(description = "Email address of the seller", example = "seller@example.com")
    private String email;

    @Column(name = "stripe_account_id")
    @Schema(
        description = "Stripe connected account ID associated with this seller",
        example = "acct_1PXXXXXXX"
    )
    private String stripeAccountId;

    @Schema(
        description = "Indicates whether the seller is considered verified in this system",
        example = "true"
    )
    private boolean verified;

    @Column(nullable = false)
    @Schema(
        description = "Current status of the seller account",
        example = "ACTIVE"
    )
    private String status;

    @Column(name = "created_at", updatable = false, insertable = false)
    @Schema(
        description = "Timestamp when the seller record was created",
        example = "2025-01-10T12:30:00"
    )
    private java.time.LocalDateTime createdAt;
}
