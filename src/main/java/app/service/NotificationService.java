package app.service;

import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.model.NotificationType;
import app.repository.NotificationPreferenceRepository;
import app.repository.NotificationRepository;
import app.web.dto.NotificationRequest;
import app.web.dto.UpsertNotificationPreference;
import app.web.mapper.DtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final MailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationPreferenceRepository notificationPreferenceRepository, MailSender mailSender, NotificationRepository notificationRepository) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.mailSender = mailSender;
        this.notificationRepository = notificationRepository;
    }

    public NotificationPreference upsertPreference(UpsertNotificationPreference dto) {

        // tries to find if such exist in the db and if exists it's just updating it
        Optional<NotificationPreference> userNotificationPreferenceOptional = notificationPreferenceRepository.findByUserId(dto.getUserId());
        if (userNotificationPreferenceOptional.isPresent()) {
            NotificationPreference preference = userNotificationPreferenceOptional.get();
            preference.setContactInfo(dto.getContactInfo());
            preference.setEnabled(dto.isNotificationEnabled());
            preference.setType(DtoMapper.fromNotificationTypeRequest(dto.getType()));
            preference.setUpdatedOn(LocalDateTime.now());
            return notificationPreferenceRepository.save(preference);
        }

        NotificationPreference notificationPreference = NotificationPreference.builder()
                .userId(dto.getUserId())
                .type(DtoMapper.fromNotificationTypeRequest(dto.getType()))
                .enabled(dto.isNotificationEnabled())
                .contactInfo(dto.getContactInfo())
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
        return notificationPreferenceRepository.save(notificationPreference);


    }

    public NotificationPreference getPreferenceByUserId(UUID userId) {
        return notificationPreferenceRepository.findByUserId(userId).orElseThrow(() -> new NullPointerException("Notification preference for user id %s was not found.".formatted(userId)));
    }

    public Notification sendNotification(NotificationRequest notificationRequest) {

        UUID userId = notificationRequest.getUserId();
        NotificationPreference userPreference = getPreferenceByUserId(userId);

        if (!userPreference.isEnabled()) {
            throw new IllegalArgumentException("User with id %s does not allow to receive notifications.".formatted(userId));
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userPreference.getContactInfo());
        message.setSubject(notificationRequest.getSubject());
        message.setText(notificationRequest.getBody());

        // building the entity
        Notification notification = Notification.builder()
                .subject(notificationRequest.getSubject())
                .body(notificationRequest.getBody())
                .createdOn(LocalDateTime.now())
                .userId(userId)
                .isDeleted(false)
                .type(NotificationType.EMAIL)
                .build();

        try {
            mailSender.send(message);
            notification.setStatus(NotificationStatus.SUCCEEDED);
        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            log.warn("There was an issue sending an email to %s due to %s".formatted(userPreference.getContactInfo(), e.getMessage()));
        }

        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationHistory(UUID userId) {

        return notificationRepository.findAllByUserIdAndDeletedIsFalse(userId);
    }

    public NotificationPreference changeNotificationPreference(UUID userId, boolean enabled) {

        NotificationPreference notificationPreference = getPreferenceByUserId(userId);
        notificationPreference.setEnabled(enabled);
        return notificationPreferenceRepository.save(notificationPreference);
    }

    public void clearNotifications(UUID userId) {

        List<Notification> notifications = getNotificationHistory(userId);

        notifications.forEach(notification -> {
            notification.setDeleted(true);
            notificationRepository.save(notification);
        });
    }

    public void retryFailedNotifications(UUID userId) {

        NotificationPreference userPreference = getPreferenceByUserId(userId);
        if (!userPreference.isEnabled()) {
            throw new IllegalArgumentException("User with id %s does not allow to receive notifications.".formatted(userId));
        }

        List<Notification> failedNotifications = notificationRepository.findAllByUserIdAndStatus(userId, NotificationStatus.FAILED);

        for (Notification notification : failedNotifications) {

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(userPreference.getContactInfo());
            message.setSubject(notification.getSubject());
            message.setText(notification.getBody());

            try {
                mailSender.send(message);
                notification.setStatus(NotificationStatus.SUCCEEDED);
            } catch (Exception e) {
                notification.setStatus(NotificationStatus.FAILED);
                log.warn("There was an issue sending an email to %s due to %s".formatted(userPreference.getContactInfo(), e.getMessage()));
            }

            notificationRepository.save(notification);
        }
    }
}
