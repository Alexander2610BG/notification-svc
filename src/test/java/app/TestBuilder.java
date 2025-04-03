package app;

import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.model.NotificationType;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class TestBuilder {
    public static NotificationPreference aRandomNotificationPreference() {

        return NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .enabled(true)
                .contactInfo("text")
                .type(NotificationType.EMAIL)
                .updatedOn(LocalDateTime.now())
                .createdOn(LocalDateTime.now())
                .build();
    }

    public static Notification aRandomNotification() {

        return Notification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .subject("asdf")
                .body("asdf")
                .createdOn(LocalDateTime.now())
                .status(NotificationStatus.SUCCEEDED)
                .type(NotificationType.EMAIL)
                .isDeleted(false)
                .build();
    }

}
