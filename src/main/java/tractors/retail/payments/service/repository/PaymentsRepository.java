package tractors.retail.payments.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tractors.retail.payments.service.models.Payments;

import java.util.Optional;

public interface  PaymentsRepository extends JpaRepository<Payments, Long> {
    Optional<Payments> findByStripePaymentIntentId(String intentId);
}
