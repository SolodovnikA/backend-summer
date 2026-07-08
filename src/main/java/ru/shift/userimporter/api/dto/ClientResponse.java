package ru.shift.userimporter.api.dto;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

@Builder
public record ClientResponse(Long phone, String name, String lastName, String middleName,
                             String email, LocalDate birthdate, Instant creationTime,
                             Instant updateTime) {
}
