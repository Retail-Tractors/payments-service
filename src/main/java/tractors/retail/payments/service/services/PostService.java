package tractors.retail.payments.service.services;

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
        post.setSeller(seller);
        return postRepository.save(post);
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }
}
