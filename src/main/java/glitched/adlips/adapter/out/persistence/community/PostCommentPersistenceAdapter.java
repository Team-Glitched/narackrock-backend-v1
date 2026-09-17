package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.PostCommentPort;
import glitched.adlips.domain.community.Post;
import glitched.adlips.domain.community.PostComment;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PostCommentPersistenceAdapter implements PostCommentPort {

    private final PostJpaRepository postRepository;
    private final PostCommentJpaRepository postCommentRepository;
    private final EntityManager entityManager;

    public PostCommentPersistenceAdapter(
            PostJpaRepository postRepository,
            PostCommentJpaRepository postCommentRepository,
            EntityManager entityManager
    ) {
        this.postRepository = postRepository;
        this.postCommentRepository = postCommentRepository;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsActivePost(Long postId) {
        return postRepository.findActiveById(postId).isPresent();
    }

    @Override
    public boolean existsActiveParentComment(Long parentCommentId, Long postId) {
        return postCommentRepository.existsActiveByIdAndPostId(parentCommentId, postId);
    }

    @Override
    public Long save(Long postId, Long userId, String content, Long parentCommentId) {
        Post post = entityManager.getReference(Post.class, postId);
        User user = entityManager.getReference(User.class, userId);
        PostComment parent = parentCommentId == null
                ? null : entityManager.getReference(PostComment.class, parentCommentId);
        PostComment saved = postCommentRepository.save(new PostComment(post, user, content, parent));
        return saved.getId();
    }

    @Override
    public void adjustPostCommentCount(Long postId, int delta) {
        postRepository.adjustCommentCount(postId, delta);
    }

    @Override
    public void adjustParentReplyCount(Long parentCommentId, int delta) {
        postCommentRepository.adjustReplyCount(parentCommentId, delta);
    }

    @Override
    public Optional<PostComment> findActiveComment(Long commentId) {
        return postCommentRepository.findActiveById(commentId);
    }

    @Override
    public void update(PostComment comment) {
        postCommentRepository.save(comment);
    }
}
