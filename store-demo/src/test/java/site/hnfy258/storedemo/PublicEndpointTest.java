package site.hnfy258.storedemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class PublicEndpointTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void testPublicEndpointsAccessible() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        try {
            // 测试公开的测试接口
            mockMvc.perform(get("/api/test/public"))
                    .andExpect(status().isOk());
            System.out.println("✅ 公开测试接口访问成功");
        } catch (Exception e) {
            System.err.println("❌ 公开测试接口访问失败: " + e.getMessage());
            throw e;
        }

        try {
            // 测试注册接口（不需要认证）- 使用随机用户名避免重复
            String randomUsername = "testpublic" + System.currentTimeMillis();
            mockMvc.perform(post("/api/auth/register")
                    .param("username", randomUsername)
                    .param("password", "123456")
                    .param("phone", "13800138999"))
                    .andExpect(status().isOk());
            System.out.println("✅ 注册接口访问成功，用户名: " + randomUsername);
        } catch (Exception e) {
            System.err.println("❌ 注册接口访问失败: " + e.getMessage());
            throw e;
        }

        System.out.println("✅ 所有公开接口测试通过");
    }

    @Test
    void testProtectedEndpointRequiresAuth() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 测试受保护的接口（应该返回401）
        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized());

        System.out.println("受保护接口正确要求认证");
    }
}