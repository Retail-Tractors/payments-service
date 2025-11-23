package tractors.retail.payments.service.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tractors.retail.payments.service.models.Payments;
import tractors.retail.payments.service.models.Post;
import tractors.retail.payments.service.repository.PaymentsRepository;
import tractors.retail.payments.service.repository.PostRepository;

import java.time.LocalDateTime;

@Service
public class PaymentsService {
    private final PaymentsRepository paymentsRepository;
    private final PostRepository postRepository;

    public PaymentsService(PaymentsRepository paymentsRepository, PostRepository postRepository) {
        this.paymentsRepository = paymentsRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public Payments createPayment(Long postId, String buyerEmail, String stripePaymentIntentId, Long amount, String currency) {
        paymentsRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .ifPresent(existing -> {
                    System.out.println("Payment already exists for intent " + stripePaymentIntentId);
                    throw new IllegalStateException("Payment already exists");
                });

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    System.out.println("Post not found for id " + postId);
                    return new IllegalArgumentException("Post not found");
                });

        Payments payment = Payments.builder()
                .post(post)
                .buyerEmail(buyerEmail)
                .stripePaymentIntentId(stripePaymentIntentId)
                .amount(amount)
                .currency(currency)
                .status("COMPLETED")
                .build();

        Payments saved = paymentsRepository.save(payment);
        return saved;
    }


}
