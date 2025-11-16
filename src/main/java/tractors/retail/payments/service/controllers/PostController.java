package tractors.retail.payments.service.controllers;

import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tractors.retail.payments.service.models.Post;
import tractors.retail.payments.service.services.PostService;
import tractors.retail.payments.service.dto.PostsResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/success")
    public ResponseEntity<String> paymentSuccess(@RequestParam Long postId) {
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
        return ResponseEntity.ok().header("Content-type", "text/html").body(html);
    }

    @GetMapping("/cancel")
    public ResponseEntity<String> paymentCancel(@RequestParam Long postId) {
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
        return ResponseEntity.ok().header("Content-type", "text/html").body(html);
    }

    @GetMapping
    public List<PostsResponse> getAllPosts() {
        return postService.getAllPosts().stream().map(PostsResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody Post post, @RequestParam Long sellerId) {
        return ResponseEntity.ok(postService.createPost(post, sellerId));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<PostsResponse> getPostById(@PathVariable Long id) {
        return postService.getPostById(id)
                .map(PostsResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id:[0-9]+}/buy")
    public ResponseEntity<Map<String, String>> buyPost(@PathVariable Long id) {
        // Get the post
        Optional<Post> optionalPost = postService.getPostById(id);
        if (optionalPost.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Post post = optionalPost.get();

        // Get the seller stripe account
        String sellerStripeAccountId = post.getSeller().getStripeAccountId();
        if (sellerStripeAccountId == null || sellerStripeAccountId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "sellerStripeAccountId is null or empty"));
        }

        // Create checkout link
//        Stripe.apiKey = stripeSecretKey;
        try {
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
                                                    .setUnitAmount(post.getPrice()*100)
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
                                    .setApplicationFeeAmount(post.getPrice()*10)
                                    .setTransferData(
                                            SessionCreateParams.PaymentIntentData.TransferData.builder()
                                                    .setDestination(sellerStripeAccountId)
                                                    .build()
                                    ).build()
                    ).build();

            Session session = Session.create(params);
            return ResponseEntity.ok(Map.of("url", session.getUrl()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id:[0-9]+}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok("Post deleted successfully");
    }
}
