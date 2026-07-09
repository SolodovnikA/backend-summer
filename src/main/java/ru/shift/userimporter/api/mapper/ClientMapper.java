package ru.shift.userimporter.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.shift.userimporter.api.dto.ClientResponse;
import ru.shift.userimporter.core.model.User;

import java.util.List;


@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "phone", expression = "java(Long.parseLong(user.getPhone()))")
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "birthdate", source = "birthDate")
    @Mapping(target = "creationTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    ClientResponse toClientResponse(User user);

    List<ClientResponse> toClientResponseList(List<User> users);
}
