package com.example.taskmanagement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taskmanagement.security.CustomUserDetails;
import com.example.taskmanagement.security.CustomUserDetailsService;
import com.example.taskmanagement.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TaskCommentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtService jwtService;

    @Test
    void userCanCommentOnOwnTaskAndAuditFieldsAreReturned() throws Exception {
        String token = jwtService.generateToken((CustomUserDetails) userDetailsService.loadUserByUsername("user"));

        String taskResponse = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Commentable task",
                                  "description": "Need a discussion",
                                  "priority": "LOW"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = taskResponse.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(post("/api/tasks/" + taskId + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Initial comment"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorUsername").value("user"))
                .andExpect(jsonPath("$.createdBy").value("user"))
                .andExpect(jsonPath("$.createdAt").exists());
    }
}
