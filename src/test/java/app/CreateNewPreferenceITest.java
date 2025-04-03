package app;

import app.model.NotificationPreference;
import app.repository.NotificationPreferenceRepository;
import app.repository.NotificationRepository;
import app.service.NotificationService;
import app.web.dto.NotificationTypeRequest;
import app.web.dto.UpsertNotificationPreference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest
public class CreateNewPreferenceITest {

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Test
    void createNewNotificationPreference_happyPath() {

        // Given
        UUID userId = UUID.randomUUID();
        UpsertNotificationPreference notificationPreference = UpsertNotificationPreference.builder()
                .userId(userId)
                .type(NotificationTypeRequest.EMAIL)
                .notificationEnabled(true)
                .contactInfo("test@email.com")
                .build();

        // When
        notificationService.upsertPreference(notificationPreference);

        // Then
        List<NotificationPreference> preferences = notificationPreferenceRepository.findAll();
        assertThat(preferences).hasSize(1);
        NotificationPreference preference = preferences.get(0);
        assertEquals(userId, preference.getUserId());
    }
}