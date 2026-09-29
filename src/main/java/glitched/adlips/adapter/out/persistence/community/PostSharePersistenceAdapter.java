package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.PostSharePort;
import org.springframework.stereotype.Repository;

@Repository
public class PostSharePersistenceAdapter implements PostSharePort {

    private final PostJpaRepository postRepository;

    public PostSharePersistenceAdapter(PostJpaRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public boolean existsActivePost(Long postId) {
        return postRepository.findActiveById(postId).isPresent();
    }
}
