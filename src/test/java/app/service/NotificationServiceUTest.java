package app.service;

import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.model.NotificationType;
import app.repository.NotificationPreferenceRepository;
import app.repository.NotificationRepository;
import app.web.dto.NotificationRequest;
import app.web.dto.NotificationTypeRequest;
import app.web.dto.UpsertNotificationPreference;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceUTest {

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Mock
    private MailSender mailSender;
    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;


    @Test
    void givenNotExistingNotificationPreference_whenChangeNotificationPreference_thenExpectException() {

        // Given
        UUID userId = UUID.randomUUID();
        boolean isNotificationEnabled = true;
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NullPointerException.class, () -> notificationService.changeNotificationPreference(userId, isNotificationEnabled));
    }


    @Test
    void givenNotExistingNotificationPreference_whenChangeNotificationPreference_thenExpectEnabledToBeChanged() {

        // Given
        UUID userId = UUID.randomUUID();
        NotificationPreference preference = NotificationPreference.builder()
                .enabled(false)
                .build();
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        // When
        notificationService.changeNotificationPreference(userId, true);

        // Then
        assertTrue(preference.isEnabled());
        verify(notificationPreferenceRepository, times(1)).save(preference);
    }

    @Test
    void givenListOfNotificationForUserId_whenClearHistory_thenExpectToClearHistory() {

        // Given

        UUID userId = UUID.randomUUID();
        List<Notification> notifications = List.of(new Notification(), new Notification());
        when(notificationRepository.findAllByUserIdAndDeletedIsFalse(userId)).thenReturn(notifications);

        // When
        notificationService.clearNotifications(userId);

        // Then
        for (Notification notification : notifications) {
            assertTrue(notification.isDeleted());
            verify(notificationRepository, times(2)).save(notification);
        }
    }

    @Test
    void givenBadNotificationPreferenceForUserId_whenRetryFailedNotifications_thenExpectException() {

        // Given
        UUID userId = UUID.randomUUID();
        NotificationPreference notificationPreference = NotificationPreference.builder()
                .enabled(false)
                .build();
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(notificationPreference));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> notificationService.retryFailedNotifications(userId));

    }

    @Test
    void givenValidNotificationPreferenceForUserId_whenRetryFailedNotifications_thenExpectNewEmailSent() {

        // Given
        UUID userId = UUID.randomUUID();
        NotificationPreference notificationPreference = NotificationPreference.builder()
                .enabled(true)
                .contactInfo("alex@gmail.com")
                .build();

        Notification failedNotification1 = Notification.builder().build();
        Notification failedNotification2 = Notification.builder().build();
        List<Notification> failedNotifications = List.of(failedNotification1, failedNotification2);

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(notificationPreference));
        when(notificationRepository.findAllByUserIdAndStatus(userId, NotificationStatus.FAILED)).thenReturn(failedNotifications);

        // When
        notificationService.retryFailedNotifications(userId);

        // Then
        for (Notification notification : failedNotifications) {
            assertEquals(NotificationStatus.SUCCEEDED, notification.getStatus());
            verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
            verify(notificationRepository, times(2)).save(notification);
        }
    }

    @Test
    void givenValidNotificationPreferenceForUserId_whenRetryFailedNotifications_thenExpectExceptionForSendingTheEmail() {

        // Given

        UUID userId = UUID.randomUUID();
        NotificationPreference notificationPreference = NotificationPreference.builder()
                .enabled(true)
                .contactInfo("alex@gmail.com")
                .build();

        Notification failedNotification = Notification.builder().build();
        List<Notification> failedNotifications = List.of(failedNotification);

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(notificationPreference));
        when(notificationRepository.findAllByUserIdAndStatus(userId, NotificationStatus.FAILED)).thenReturn(failedNotifications);
        doThrow(new RuntimeException()).when(mailSender).send(any(SimpleMailMessage.class));

        // When
        notificationService.retryFailedNotifications(userId);

        // Then
        assertEquals(NotificationStatus.FAILED, failedNotification.getStatus());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        verify(notificationRepository, times(1)).save(failedNotification);
    }

    @Test
    void givenBadNotificationPreferenceFromNotificationRequest_whenSendNotification_thenExpectException() {

        // Given
        UUID userId = UUID.randomUUID();
        NotificationPreference notificationPreference = NotificationPreference.builder()
                .enabled(false)
                .build();
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .userId(userId)
                .subject("asdfasdf")
                .body("asdfasdf")
                .build();
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(notificationPreference));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> notificationService.sendNotification(notificationRequest));
    }

    @Test
    void givenValidNotificationPreferenceAndValidNotificationRequest_whenSendNotification_thenExpectNewEmailSent() {

        // Given

        UUID userId = UUID.randomUUID();
        NotificationPreference notificationPreference = NotificationPreference.builder()
                .enabled(true)
                .build();
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .userId(userId)
                .subject("asdfasdf")
                .body("asdfasdf")
                .build();
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(notificationPreference));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Notification notification = notificationService.sendNotification(notificationRequest);

        // Then
        assertEquals(NotificationStatus.SUCCEEDED, notification.getStatus());
        assertEquals("asdfasdf", notification.getSubject());
        assertEquals("asdfasdf", notification.getBody());
        assertEquals(userId, notification.getUserId());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        verify(notificationRepository, times(1)).save(notification);

    }

    @Test
    void givenValidNotificationPreferenceAndValidNotificationRequest_whenSendNotification_thenExpectException() {

        // Given

        UUID userId = UUID.randomUUID();
        NotificationPreference notificationPreference = NotificationPreference.builder()
                .enabled(true)
                .build();
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .userId(userId)
                .subject("asdfasdf")
                .body("asdfasdf")
                .build();
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(notificationPreference));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException()).when(mailSender).send(any(SimpleMailMessage.class));

        // When
        Notification notification = notificationService.sendNotification(notificationRequest);

        // Then
        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        verify(notificationRepository, times(1)).save(notification);
    }


    @Test
    void givenExistingPreference_whenUpsertPreference_thenExpectUpdateOfPreference() {

        // Given
        UUID userId = UUID.randomUUID();
        UpsertNotificationPreference dto = UpsertNotificationPreference.builder()
                .userId(userId)
                .notificationEnabled(true)
                .type(NotificationTypeRequest.EMAIL)
                .contactInfo("alex@gmail.com")
                .build();

        NotificationPreference existingPreference = NotificationPreference.builder()
                .userId(userId)
                .type(NotificationType.EMAIL)
                .contactInfo("alex@gmail.com")
                .enabled(false)
                .updatedOn(LocalDateTime.now())
                .createdOn(LocalDateTime.now())
                .build();

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existingPreference));
        when(notificationPreferenceRepository.save(any(NotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        NotificationPreference updatedPreference = notificationService.upsertPreference(dto);

        // Then
        assertEquals("alex@gmail.com", updatedPreference.getContactInfo());
        assertTrue(updatedPreference.isEnabled());
        assertEquals(NotificationType.EMAIL, updatedPreference.getType());
        verify(notificationPreferenceRepository).save(updatedPreference);
    }

    @Test
    void givenNotExistingPreference_whenUpsertPreference_thenExpectNewPreference() {
        // Given
        UUID userId = UUID.randomUUID();
        UpsertNotificationPreference dto = UpsertNotificationPreference.builder()
                .userId(userId)
                .notificationEnabled(true)
                .type(NotificationTypeRequest.EMAIL)
                .contactInfo("alex@gmail.com")
                .build();
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(notificationPreferenceRepository.save(any(NotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        NotificationPreference newPreference = notificationService.upsertPreference(dto);

        // Then
        assertEquals(userId, newPreference.getUserId());
        assertEquals("alex@gmail.com", newPreference.getContactInfo());
        assertTrue(newPreference.isEnabled());
        assertEquals(NotificationType.EMAIL, newPreference.getType());
        verify(notificationPreferenceRepository).save(newPreference);
    }
}
