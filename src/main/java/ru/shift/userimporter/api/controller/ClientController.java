package ru.shift.userimporter.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.shift.userimporter.api.dto.ClientResponse;
import ru.shift.userimporter.core.model.User;
import ru.shift.userimporter.core.service.UserService;
import ru.shift.userimporter.api.mapper.ClientMapper;

import java.util.List;


@RequiredArgsConstructor
@RequestMapping("/clients")
@RestController
public class ClientController {
    private final UserService userService;
    private final ClientMapper clientMapper;

    @GetMapping
    public List<ClientResponse> getClients(@RequestParam(required = false) Long phone,
                                           @RequestParam(required = false) String name,
                                           @RequestParam(required = false) String lastName,
                                           @RequestParam(required = false) String email,
                                           @RequestParam(required = false, defaultValue = "0") long offset,
                                           @RequestParam(required = false, defaultValue = "100") int limit) {

        List<User> users = userService.findWithFilter(phone, name, lastName, email, offset, limit);
        return clientMapper.toClientResponseList(users);
    }
}
