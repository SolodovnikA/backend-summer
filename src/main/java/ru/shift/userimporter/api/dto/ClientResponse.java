package ru.shift.userimporter.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ClientResponse(Long phone, String name, String lastName, String middleName,
                             String email, LocalDate birthdate, OffsetDateTime creationTime,
                             OffsetDateTime updateTime) {
}
