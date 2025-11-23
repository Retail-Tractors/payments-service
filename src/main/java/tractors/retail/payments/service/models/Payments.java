package tractors.retail.payments.service.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name ="payments")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // JOIN to posts table
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "buyer_email")
    private String buyerEmail;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    private Long amount;
    private String currency;
    private String status;

    @Column(name = "created_at", updatable = false, insertable = false)
    private java.time.LocalDateTime createdAt;
}
