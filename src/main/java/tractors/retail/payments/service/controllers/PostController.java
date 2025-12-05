package tractors.retail.payments.service.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tractors.retail.payments.service.models.Post;
import tractors.retail.payments.service.services.PostService;
import tractors.retail.payments.service.dto.PostsResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments/posts")
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
        try {
            String checkoutUrl = postService.createCheckoutSession(id);
            return ResponseEntity.ok(Map.of("url", checkoutUrl));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
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
