package tractors.retail.payments.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tractors.retail.payments.service.models.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
}
