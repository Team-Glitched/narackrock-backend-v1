package glitched.adlips.adapter.in.web.community;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.community.GetLikedPostsUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/liked-posts")
public class LikedPostsController {
    private final GetLikedPostsUseCase likedPosts;
    private final AuthenticatedUserResolver users;
    public LikedPostsController(GetLikedPostsUseCase likedPosts, AuthenticatedUserResolver users) { this.likedPosts = likedPosts; this.users = users; }
    @GetMapping
    public ApiResponse<List<Long>> list(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.success("좋아요한 게시글 목록", likedPosts.execute(users.requireUserId(authorization)));
    }
}
