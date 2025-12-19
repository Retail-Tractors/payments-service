package tractors.retail.payments.service.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sellers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Retrieved from token, links to the users table from users-service
    @Column(name = "userid", nullable = false, unique = true)
    private Integer userId; 
    private String name;
    private String email;

    @Column(name = "stripe_account_id")
    private String stripeAccountId;
    // we assume the account is verified even tough they still need to send ID to stripe because we are using test mode
    // in production we wouldnt need this
    private boolean verified;

    // this status is linked to the real stripe account status
    @Column(nullable = false)
    private String status;

    // added updatable = false, insertable = false
    // because the db uses now() as default expression and it cannot change
    @Column(name = "created_at", updatable = false, insertable = false)
    private java.time.LocalDateTime createdAt;
}