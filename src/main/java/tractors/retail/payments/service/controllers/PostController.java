package tractors.retail.payments.service.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import tractors.retail.payments.service.models.Post;
import tractors.retail.payments.service.services.PostService;
import tractors.retail.payments.service.dto.PostsResponse;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/payments/posts")
@RequiredArgsConstructor
@Tag(name = "Payments Posts", description = "Operations related to posts that can be purchased via payments")
public class PostController {

    private final PostService postService;

    @Operation(
        summary = "Payment success page",
        description = "Returns an HTML page indicating that the payment for the given post was successful."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "HTML success page returned",
            content = @Content(mediaType = "text/html"))
    })
    @GetMapping("/success")
    public ResponseEntity<String> paymentSuccess(
            @Parameter(description = "ID of the post that was purchased", example = "1")
            @RequestParam Long postId) {
        
        String html = postService.handlePostPurchaseSuccess(postId);
        
        return ResponseEntity.ok().header("Content-type", "text/html").body(html);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Operation(
        summary = "Payment cancel page",
        description = "Returns an HTML page indicating that the payment for the given post was cancelled."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "HTML cancellation page returned",
            content = @Content(mediaType = "text/html"))
    })
    @GetMapping("/cancel")
    public ResponseEntity<String> paymentCancel(
            @Parameter(description = "ID of the post whose purchase was cancelled", example = "1")
            @RequestParam Long postId) {

        String html = postService.handlePostPurchaseCancel(postId);

        return ResponseEntity.ok().header("Content-type", "text/html").body(html);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Operation(
        summary = "Get all posts",
        description = "Returns a list of all posts that can be purchased."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of posts returned",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = PostsResponse.class))
            ))
    })
    @GetMapping
    public List<PostsResponse> getAllPosts() {
        return postService.getAllPosts().stream().map(PostsResponse::from).toList();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Operation(
        summary = "Create a post",
        description = "Creates a new post associated with the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Post created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Post.class)
            )),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<Post> createPost(
            @Parameter(description = "Post payload") @RequestBody Post post,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        Integer userId = Integer.parseInt(jwt.getSubject());
        return ResponseEntity.ok(postService.createPost(post, userId));
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Operation(
        summary = "Get post by ID",
        description = "Returns details of a single post by its ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Post found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PostsResponse.class)
            )),
        @ApiResponse(responseCode = "404", description = "Post not found")
    })
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<PostsResponse> getPostById(
            @Parameter(description = "ID of the post", example = "1")
            @PathVariable Long id) {
        return postService.getPostById(id)
                .map(PostsResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Operation(
        summary = "Create checkout session to buy a post",
        description = "Creates a Stripe checkout session URL for buying a post. Optionally links to a booking."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Checkout URL created",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "Map containing checkout URL",
                    example = "{\"url\": \"https://checkout.stripe.com/...\"}"
                )
            )),
        @ApiResponse(responseCode = "400", description = "Invalid request or business rule violation",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "Error message",
                    example = "{\"error\": \"Post is not available for purchase\"}"
                )
            )),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id:[0-9]+}/buy")
    public ResponseEntity<Map<String, String>> buyPost(
            @Parameter(description = "ID of the post to buy", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Optional booking ID associated with this purchase", example = "123")
            @RequestParam(required = false) String bookingId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        try {
            String userEmail = jwt.getClaim("email");
            String checkoutUrl = postService.createCheckoutSession(id, bookingId, userEmail);
            return ResponseEntity.ok(Map.of("url", checkoutUrl));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Operation(
        summary = "Delete a post",
        description = "Deletes a post by its ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Post deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Post not found")
    })
    @DeleteMapping("/{id:[0-9]+}")
    public ResponseEntity<?> deletePost(
            @Parameter(description = "ID of the post to delete", example = "1")
            @PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok("Post deleted successfully");
    }
}
