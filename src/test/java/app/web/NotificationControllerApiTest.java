package app.web;

import app.model.NotificationStatus;
import app.model.NotificationType;
import app.service.NotificationService;
import app.web.dto.NotificationRequest;
import app.web.dto.NotificationResponse;
import app.web.dto.NotificationTypeRequest;
import app.web.dto.UpsertNotificationPreference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static app.TestBuilder.aRandomNotification;
import static app.TestBuilder.aRandomNotificationPreference;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
public class NotificationControllerApiTest {

    @MockitoBean
    private NotificationService notificationService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getRequestNotificationPreference_happyPath() throws Exception {

        // 1. Build Request
        when(notificationService.getPreferenceByUserId(any())).thenReturn(aRandomNotificationPreference());
        MockHttpServletRequestBuilder request = get("/api/v1/notifications/preferences").param("userId", UUID.randomUUID().toString());

        // 2. Send Request
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("userId").isNotEmpty())
                .andExpect(jsonPath("type").isNotEmpty())
                .andExpect(jsonPath("enabled").isNotEmpty())
                .andExpect(jsonPath("contactInfo").isNotEmpty());
    }

    @Test
    void postWithBodyToCreatePreference_returns201AndCorrectDtoStructure() throws Exception {

        // 1. Build Request
        UpsertNotificationPreference requestDto = UpsertNotificationPreference.builder()
                .userId(UUID.randomUUID())
                .type(NotificationTypeRequest.EMAIL)
                .contactInfo("text")
                .notificationEnabled(true)
                .build();
        when(notificationService.upsertPreference(any())).thenReturn(aRandomNotificationPreference());
        MockHttpServletRequestBuilder request = post("/api/v1/notifications/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsBytes(requestDto));

        // 2. Send Request
        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("userId").isNotEmpty())
                .andExpect(jsonPath("type").isNotEmpty())
                .andExpect(jsonPath("enabled").isNotEmpty())
                .andExpect(jsonPath("contactInfo").isNotEmpty());
    }

    @Test
    void getNotificationHistory_happyPath() throws Exception {

        // 1. Prepare test data
        UUID userId = UUID.randomUUID();
        List<NotificationResponse> notificationResponses = List.of(
                NotificationResponse.builder()
                        .subject("Test Subject 1")
                        .status(NotificationStatus.SUCCEEDED)
                        .createdOn(LocalDateTime.now())
                        .type(NotificationType.EMAIL)
                        .build(),
                NotificationResponse.builder()
                        .subject("Test Subject 2")
                        .status(NotificationStatus.FAILED)
                        .createdOn(LocalDateTime.now())
                        .type(NotificationType.EMAIL)
                        .build()
        );

        // 2. Mock the service call
        when(notificationService.getNotificationHistory(any()))
                .thenReturn(notificationResponses.stream()
                        .map(response -> aRandomNotification()) // Or real entities if you prefer
                        .toList());

        // 3. Perform the GET request
        mockMvc.perform(get("/api/v1/notifications")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].subject").isNotEmpty())
                .andExpect(jsonPath("$[0].status").isNotEmpty())
                .andExpect(jsonPath("$[0].createdOn").isNotEmpty())
                .andExpect(jsonPath("$[0].type").isNotEmpty());

        // 4. Verify interaction
        verify(notificationService, times(1)).getNotificationHistory(userId);
    }

    @Test
    void changeNotificationPreference_happyPath() throws Exception {

        // 1. Prepare mock return
        when(notificationService.changeNotificationPreference(any(), anyBoolean()))
                .thenReturn(aRandomNotificationPreference());

        // 2. Build request
        MockHttpServletRequestBuilder request = put("/api/v1/notifications/preferences")
                .param("userId", UUID.randomUUID().toString())
                .param("enabled", "true");

        // 3. Send and verify response
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("userId").isNotEmpty())
                .andExpect(jsonPath("type").isNotEmpty())
                .andExpect(jsonPath("enabled").isNotEmpty())
                .andExpect(jsonPath("contactInfo").isNotEmpty());

        // 4. Verify service call
        verify(notificationService, times(1)).changeNotificationPreference(any(), eq(true));
    }


    @Test
    void postWithBodyToSendNotification_returns201AndNotification() throws Exception {

        UUID userId = UUID.randomUUID();
        NotificationRequest requestDto = NotificationRequest.builder()
                .userId(userId)
                .subject("asdf")
                .body("asdf")
                .build();
        when(notificationService.sendNotification(any())).thenReturn(aRandomNotification());
        MockHttpServletRequestBuilder request = post("/api/v1/notifications").param("userId", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsBytes(requestDto));

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("subject").isNotEmpty())
                .andExpect(jsonPath("status").isNotEmpty())
                .andExpect(jsonPath("createdOn").isNotEmpty())
                .andExpect(jsonPath("type").isNotEmpty());
    }

    @Test
    void putWithParamRetryFailedNotifications_returns200() throws Exception {

        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/notifications")
                .param("userId", userId.toString()))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).retryFailedNotifications(userId);
    }

    @Test
    void deleteWithParamClearNotificationHistory_returns200() throws Exception {

        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/notifications")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).clearNotifications(userId);
    }



}
