package tractors.retail.payments.service.models;

import jakarta.persistence.*;
import lombok.*;

import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Post that can be listed and purchased")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the post", example = "1")
    private Long id;

    @Schema(description = "Title of the post", example = "John Deere 5100 tractor")
    private String title;

    @Schema(description = "Detailed description of the tractor/post", example = "2018 model, well maintained, 1500 hours")
    private String description;

    @Schema(description = "Price in minor units (e.g. cents)", example = "2500000")
    private long price;

    @Schema(description = "Currency code in ISO-4217 format", example = "EUR")
    private String currency;

    @Schema(description = "Current status of the post", example = "AVAILABLE")
    private String status;

    @Column(name = "created_at", updatable = false, insertable = false)
    @Schema(description = "Timestamp when the post was created", example = "2025-01-10T12:30:00")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Schema(description = "Timestamp when the post was last updated", example = "2025-01-15T09:45:00")
    private java.time.LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    @Schema(description = "Seller that owns this post")
    private Seller seller;
}