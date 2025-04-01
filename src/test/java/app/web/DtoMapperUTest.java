package app.web;

import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.model.NotificationType;
import app.web.dto.NotificationPreferenceResponse;
import app.web.dto.NotificationResponse;
import app.web.mapper.DtoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class DtoMapperUTest {

    @Test
    void givenHappyPath_whenMappingFromNotificationPreference_thenExpectNotificationPreferenceResponse() {

        // Given
        NotificationPreference notificationPreference = NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .type(NotificationType.EMAIL)
                .enabled(true)
                .contactInfo("alex@gmail.com")
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
        // When
        NotificationPreferenceResponse responseDto = DtoMapper.fromNotificationPreference(notificationPreference);

        // Then
        assertEquals(notificationPreference.getId(), responseDto.getId());
        assertEquals(notificationPreference.getUserId(), responseDto.getUserId());
        assertEquals(notificationPreference.getType(), responseDto.getType());
        assertEquals(notificationPreference.isEnabled(), responseDto.isEnabled());
        assertEquals(notificationPreference.getContactInfo(), responseDto.getContactInfo());
    }

    @Test
    void givenHappyPath_whenMappingFromNotification_thenExpectNotificationResponse() {

        // Given
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .subject("asdf")
                .body("asdf")
                .createdOn(LocalDateTime.now())
                .status(NotificationStatus.SUCCEEDED)
                .type(NotificationType.EMAIL)
                .userId(UUID.randomUUID())
                .isDeleted(false)
                .build();

        // When
        NotificationResponse response = DtoMapper.fromNotification(notification);

        // Then
        assertEquals(notification.getSubject(), response.getSubject());
        assertEquals(notification.getStatus(), response.getStatus());
        assertEquals(notification.getType(), response.getType());
        assertEquals(notification.getCreatedOn(), response.getCreatedOn());
    }
}
