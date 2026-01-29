package tractors.retail.payments.service.services;

import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @Value("${stripe.commission-percentage}")
    private Integer commissionPercentage;

    public void validateSellerStatus(Seller seller) {
        if (seller.getStatus().equals("DISABLED")) {
            throw new RuntimeException("Seller is disabled and cannot receive payments and create posts");
        }
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }

    @Transactional
    public Post createPost(Post post, Integer userId) {
        
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        validateSellerStatus(seller);

        post.setSeller(seller);
        return postRepository.save(post);
    }

    @Transactional
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    @Transactional
    public void updatePostStatus(Long postId, String newStatus) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        post.setStatus(newStatus);
        post.setUpdatedAt(java.time.LocalDateTime.now());
        postRepository.save(post);
    }

    public String getSellerStripeId(Post post) {
        Seller seller = post.getSeller();
        String stripeAccountId = seller.getStripeAccountId();

        if (seller == null || stripeAccountId == null || stripeAccountId.isEmpty()) {
            throw new RuntimeException("The seller of this post needs to configure their Stripe account before creating posts");
        }
        return stripeAccountId;
    }

    public String createCheckoutSession(Long postId, String bookingId, String buyerEmail) throws Exception {
        // TODO: replace email with actual buyer email, get it from request

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        String stripeAccountId = getSellerStripeId(post);

        SessionCreateParams.PaymentIntentData.Builder paymentIntentBuilder = SessionCreateParams.PaymentIntentData.builder()
                .setApplicationFeeAmount(post.getPrice() * commissionPercentage)
                .putMetadata("post_id", post.getId().toString())
                .setReceiptEmail(buyerEmail)
                .setTransferData(
                        SessionCreateParams.PaymentIntentData.TransferData.builder()
                                .setDestination(stripeAccountId)
                                .build()
                );

        // Se tivermos um bookingId, adicionamos aos metadados!
        // É ISTO QUE PERMITE AO WEBHOOK SABER QUAL A RESERVA
        if (bookingId != null && !bookingId.isEmpty()) {
            paymentIntentBuilder.putMetadata("booking_id", bookingId);
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?postId=" + post.getId())
                .setCancelUrl(cancelUrl + "?postId=" + post.getId())
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
                .setPaymentIntentData(paymentIntentBuilder.build())
                .build();

        Session session = Session.create(params);
        String url = session.getUrl();
        return url;
    }

    public String handlePostPurchaseSuccess(Long postId) {
        String html = String.format("""
            <html>
              <head>
                <title>Stripe Payment Complete</title>
                <style>
                  body { font-family: Arial, sans-serif; text-align: center; margin-top: 100px; }
                  h1 { color: #4CAF50; }
                  a { text-decoration: none; color: #2196F3; }
                </style>
              </head>
              <body>
                <h1>Congratulations, you successfully bought the tractor from the post with id:%2d</h1>
              </body>
            </html>
        """, postId);

        return html;
    }

    public String handlePostPurchaseCancel(Long postId) {
        String html = String.format("""
            <html>
              <head>
                <title>Stripe Payment Cancellation</title>
                <style>
                  body { font-family: Arial, sans-serif; text-align: center; margin-top: 100px; }
                  h1 { color: red; }
                  a { text-decoration: none; color: #2196F3; }
                </style>
              </head>
              <body>
                <h1>You cancelled your purchase from post with the id: %2d</h1>
              </body>
            </html>
        """, postId);

        return html;
    }

}