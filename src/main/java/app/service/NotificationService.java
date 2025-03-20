package app.service;

import app.model.NotificationPreference;
import app.repository.NotificationPreferenceRepository;
import app.web.dto.UpsertNotificationPreference;
import app.web.mapper.DtoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    public NotificationService(NotificationPreferenceRepository notificationPreferenceRepository) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
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
}
