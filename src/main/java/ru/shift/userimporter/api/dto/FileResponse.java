package ru.shift.userimporter.api.dto;

import lombok.Builder;

@Builder
public record FileResponse(String fileId, String status, FileStatistic statistic) {
}
