package tractors.retail.payments.service.services;

import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tractors.retail.payments.service.models.Post;
import tractors.retail.payments.service.models.Seller;
import tractors.retail.payments.service.repository.PostRepository;
import tractors.retail.payments.service.repository.SellerRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final SellerRepository sellerRepository;

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }

    public Post createPost(Post post, Long sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        if (seller.getStatus().equals("DISABLED")) {
            throw new RuntimeException("Seller not allowed to create posts");
        }

        post.setSeller(seller);
        return postRepository.save(post);
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    public String createCheckoutSession(Long postId) throws Exception {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Seller seller = post.getSeller();

        if (seller == null) {
            throw new RuntimeException("Post has no seller");
        }

        String stripeAccountId = seller.getStripeAccountId();
        if (stripeAccountId == null || stripeAccountId.isEmpty()) {
            throw new RuntimeException("Seller Stripe account not configured");
        }

        if (seller.getStatus().equals("DISABLED")) {
            throw new RuntimeException("Seller is disabled and cannot receive payments");
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:8080/api/posts/success?postId=" + post.getId())
                .setCancelUrl("http://localhost:8080/api/posts/cancel?postId=" + post.getId())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(post.getCurrency())
                                                .setUnitAmount(post.getPrice() * 100)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(post.getTitle())
                                                                .setDescription(post.getDescription())
                                                                .build()
                                                ).build()
                                ).build()
                )
                .setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .setApplicationFeeAmount(post.getPrice() * 10)
                                .putMetadata("post_id", post.getId().toString())
                                .setReceiptEmail("buyer@example.com")
                                .setTransferData(
                                        SessionCreateParams.PaymentIntentData.TransferData.builder()
                                                .setDestination(stripeAccountId)
                                                .build()
                                ).build()
                ).build();

        Session session = Session.create(params);
        String url = session.getUrl();
        return url;
    }
}
