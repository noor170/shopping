package com.example.taskmanagement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

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
                                  "priority": "HIGH",
                                  "deadline": "2026-11-15",
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.deadline").value("2026-11-15"));

        mockMvc.perform(get("/api/tasks?status=COMPLETED&priority=HIGH&search=Integration&page=0&size=5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(taskId))
                .andExpect(jsonPath("$.content[0].priority").value("HIGH"));

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId));

        MockMultipartFile attachment = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                "demo attachment".getBytes());

        MvcResult attachmentResult = mockMvc.perform(multipart("/api/tasks/" + taskId + "/attachments")
                        .file(attachment)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFilename").value("sample.pdf"))
                .andReturn();

        JsonNode attachmentJson = objectMapper.readTree(attachmentResult.getResponse().getContentAsString());
        Long attachmentId = attachmentJson.get("id").asLong();

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

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachments[0].originalFilename").value("sample.pdf"));

        mockMvc.perform(get("/api/tasks/" + taskId + "/attachments/" + attachmentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk());

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
                                  "priority": "LOW",
                                  "deadline": "2026-10-01",
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
                                  "priority": "HIGH",
                                  "deadline": "2026-10-15",
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
