package ru.shift.userimporter.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import ru.shift.userimporter.api.dto.ClientResponse;
import ru.shift.userimporter.api.mapper.ClientMapper;
import ru.shift.userimporter.core.model.User;
import ru.shift.userimporter.core.service.UserService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
public class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userservice;

    @MockitoBean
    private ClientMapper clientMapper;

    @Test
    void getClients_shouldReturn200_withEmptyList() throws Exception {

        when(userservice.findWithFilter(null, null, null, null, 0, 100)).
                thenReturn(List.of());
        when(clientMapper.toClientResponseList(List.of())).thenReturn(List.of());

        mockMvc.perform(get("/clients")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void getClients_shouldReturn200_withClientList() throws Exception {
        User testUser = User.builder()
                .id(1L)
                .firstName("Иван")
                .lastName("Иванов")
                .phone("79995551122")
                .email("ivan@shift.ru")
                .birthDate(LocalDate.of(1995, 3, 14))
                .build();

        ClientResponse testResponse = ClientResponse.builder()
                .phone(Long.parseLong(testUser.getPhone()))
                .name(testUser.getFirstName())
                .lastName(testUser.getLastName())
                .email(testUser.getEmail())
                .birthdate(testUser.getBirthDate())
                .build();

        when(userservice.findWithFilter(null, null, null, null, 0, 100)).
                thenReturn(List.of(testUser));
        when(clientMapper.toClientResponseList(List.of(testUser))).thenReturn(List.of(testResponse));

        mockMvc.perform(get("/clients")).andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].name").value("Иван"));
    }
}
