package tractors.retail.payments.service.repository;

import tractors.retail.payments.service.models.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    Optional<Seller> findByEmail(String email);
    Optional<Seller> findByStripeAccountId(String stripeAccountId);
    Optional<Seller> findByUserId(Integer userId);
}
