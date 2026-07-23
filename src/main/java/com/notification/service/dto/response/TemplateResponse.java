package com.notification.service.dto.response;

import com.notification.service.entity.Template;
import com.notification.service.entity.enums.ChannelType;

import java.time.Instant;

public record TemplateResponse(
        Long id,
        String name,
        ChannelType channelType,
        String contentTemplate,
        Instant createdAt
) {
    public static TemplateResponse from(Template template) {
        return new TemplateResponse(
                template.getId(),
                template.getName(),
                template.getChannelType(),
                template.getContentTemplate(),
                template.getCreatedAt());
    }
}