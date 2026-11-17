package glitched.adlips;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.user.admin.usecase.ProvisionOperatorAdminsUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.auth.token-secret=test-only-secret-value-32-characters",
        "spring.datasource.url=jdbc:h2:mem:admin_roles;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.project.export-processing-enabled=false",
        "app.admin.operator-emails=op@test.local",
        // The scheduler would provision on its own; the test runs the use case explicitly instead.
        "app.admin.operator-sync-initial-delay=PT1H"
})
@AutoConfigureMockMvc
class AdminRolesIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired TransactionRunner transactions;
    @Autowired UserRepositoryPort users;
    @Autowired ProfileRepositoryPort profiles;
    @Autowired GalleryManagementPort galleries;
    @Autowired AccessTokenPort tokens;
    @Autowired ProvisionOperatorAdminsUseCase provisionOperatorAdmins;

    @Test
    void 운영자_어드민이_커뮤니티_어드민을_지정하고_해임한다() throws Exception {
        long[] ids = transactions.required(() -> {
            Long operator = users.save(User.create("op@test.local")).getId();
            Long member = users.save(User.create("member@test.local")).getId();
            profiles.save(Profile.create(operator, "운영자"));
            profiles.save(Profile.create(member, "멤버"));
            return new long[] {operator, member, galleries.createGallery("테스트 커뮤니티", "설명")};
        });
        long memberId = ids[1];
        String operator = "Bearer " + tokens.issue(ids[0]);
        String member = "Bearer " + tokens.issue(memberId);
        String managers = "/api/v1/galleries/" + ids[2] + "/manage/managers";

        provisionOperatorAdmins.execute();
        mockMvc.perform(get("/api/v1/admin/me").header("Authorization", operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operatorAdmin").value(true));
        mockMvc.perform(get("/api/v1/admin/me").header("Authorization", member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operatorAdmin").value(false));

        mockMvc.perform(get(managers).header("Authorization", member))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(managers).header("Authorization", operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + memberId + "}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get(managers).header("Authorization", member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(memberId))
                .andExpect(jsonPath("$.data[0].nickname").value("멤버"));

        mockMvc.perform(delete(managers + "/" + memberId).header("Authorization", member))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LAST_GALLERY_MANAGER"));
        mockMvc.perform(delete(managers + "/" + memberId).header("Authorization", operator))
                .andExpect(status().isOk());
        mockMvc.perform(get(managers).header("Authorization", operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/v1/galleries/" + ids[2] + "/manage/logs").header("Authorization", operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logs[*].actionType").value(hasItems("GRANT_MANAGER", "REVOKE_MANAGER")));
    }

    @Test
    void DB에_혼합_대소문자로_저장된_이메일도_운영자_어드민으로_지정된다() throws Exception {
        Long mixedId = transactions.required(() -> {
            User saved = users.save(User.create("Mixed.Op@Test.Local"));
            profiles.save(Profile.create(saved.getId(), "대소문자운영자"));
            return saved.getId();
        });
        ProvisionOperatorAdminsUseCase mixedUseCase = new ProvisionOperatorAdminsUseCase(
                users,
                transactions,
                "mixed.op@test.local"
        );
        mixedUseCase.execute();

        mockMvc.perform(get("/api/v1/admin/me").header("Authorization", "Bearer " + tokens.issue(mixedId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operatorAdmin").value(true));
    }
}
