package tractors.retail.payments.service.dto;

import tractors.retail.payments.service.models.Post;

public record PostsResponse(
        Long id,
        String title,
        String description,
        Long price,
        String currency,
        Long sellerId,
        String sellerName
) {
    public static PostsResponse from(Post post) {
        return new PostsResponse(
                post.getId(),
                post.getTitle(),
                post.getDescription(),
                post.getPrice(),
                post.getCurrency(),
                post.getSeller().getId(),
                post.getSeller().getName()
        );
    }
}