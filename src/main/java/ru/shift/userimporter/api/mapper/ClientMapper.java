package ru.shift.userimporter.api.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.shift.userimporter.api.dto.ClientResponse;
import ru.shift.userimporter.core.model.User;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientMapper {
    public static ClientResponse toClientResponse(User user) {
        return ClientResponse.builder()
                .phone(Long.parseLong(user.getPhone()))
                .name(user.getFirstName())
                .lastName(user.getLastName())
                .middleName(user.getMiddleName())
                .email(user.getEmail())
                .birthdate(user.getBirthDate())
                .creationTime(user.getCreatedAt())
                .updateTime(user.getUpdatedAt())
                .build();

    }

    public static List<ClientResponse> toClientResponseList(List<User> users) {
        return users.stream()
                .map(ClientMapper::toClientResponse)
                .toList();
    }
}
