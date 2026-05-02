package com.leafy.notificationservice.service.template;

import com.leafy.common.enums.NotificationType;
import com.leafy.notificationservice.enums.NotificationChannel;
import com.leafy.notificationservice.model.NotificationTemplate;
import com.leafy.notificationservice.repository.NotificationTemplateRepository;
import com.leafy.notificationservice.utils.TemplateEngine;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    NotificationTemplateRepository templateRepository;
    TemplateEngine templateEngine;

    @Override
    public NotificationTemplate find(NotificationType type, NotificationChannel channel, String locale) {
        // Try the requested locale first, fall back to Vietnamese
        return templateRepository.findByTypeAndChannelAndLocaleAndActiveTrue(type, channel, locale)
                .or(() -> {
                    if (!"vi".equals(locale)) {
                        log.debug("[Template] Locale '{}' not found for type={}, falling back to 'vi'", locale, type);
                        return templateRepository.findByTypeAndChannelAndLocaleAndActiveTrue(type, channel, "vi");
                    }
                    return java.util.Optional.empty();
                })
                .orElseGet(() -> {
                    log.debug("[Template] No active template found for type={}, channel={}, locale={}", type, channel, locale);
                    return null;
                });
    }

    @Override
    public String render(String template, Map<String, Object> payload) {
        return templateEngine.render(template, payload);
    }
}
