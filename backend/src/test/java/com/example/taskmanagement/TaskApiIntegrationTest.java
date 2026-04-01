package com.example.taskmanagement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class TaskApiIntegrationTest extends IntegrationTestSupport {

    @Test
    void userShouldCreateUpdateListGetCommentSubmitAndDeleteTask() throws Exception {
        String userToken = loginAndGetToken("user", "User@123");
        Long taskId = createTaskAndGetId(userToken, "Integration Task", "Task for API coverage");

        mockMvc.perform(put("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Integration Task",
                                  "description": "Updated description",
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/tasks?status=COMPLETED&search=Integration&page=0&size=5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(taskId));

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId));

        mockMvc.perform(post("/api/tasks/" + taskId + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "API comment"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("API comment"));

        mockMvc.perform(post("/api/tasks/" + taskId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submittedAt").exists())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(delete("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminShouldAssignApproveRejectAndViewTasks() throws Exception {
        String adminToken = loginAndGetToken("admin", "Admin@123");
        String userToken = loginAndGetToken("user", "User@123");
        Long taskId = createTaskAndGetId(userToken, "Review Task", "Needs admin actions");

        mockMvc.perform(post("/api/tasks/" + taskId + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assigneeUserId": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(2));

        mockMvc.perform(put("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Review Task",
                                  "description": "Ready for approval",
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tasks/" + taskId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tasks/" + taskId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment": "Approved in test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        Long taskIdTwo = createTaskAndGetId(userToken, "Reject Task", "Needs rejection");
        mockMvc.perform(put("/api/tasks/" + taskIdTwo)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Reject Task",
                                  "description": "Ready for rejection",
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tasks/" + taskIdTwo + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tasks/" + taskIdTwo + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment": "Rejected in test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/tasks?page=0&size=10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
