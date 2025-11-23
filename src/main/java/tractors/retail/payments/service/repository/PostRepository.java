package tractors.retail.payments.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tractors.retail.payments.service.models.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
}
