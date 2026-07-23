package com.notification.service.dto.request;

import com.notification.service.entity.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TemplateRequest(
        @NotBlank String name,
        @NotNull ChannelType channelType,
        @NotBlank String contentTemplate
) {
}