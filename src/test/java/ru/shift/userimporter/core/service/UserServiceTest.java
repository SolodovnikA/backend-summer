package ru.shift.userimporter.core.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import ru.shift.userimporter.core.model.User;
import ru.shift.userimporter.core.repository.UserRepository;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void findWithFilter_shouldReturnList() {
        User testUser = User.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .email("ivan@shift.ru")
                .phone("79995551122")
                .build();

        List<User> users = List.of(testUser);
        Page<User> page = new PageImpl<>(users);

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        List<User> result = userService.findWithFilter(null, null, null,
                null, 0, 100);

        assertEquals(users, result);


    }

    @Test
    void findWithFilter_shouldReturnEmptyList_whenNoMatches() {
        Page<User> emptyPage = new PageImpl<>(List.of());
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        List<User> result = userService.findWithFilter(666L, null, null,
                null, 0, 100);

        assertTrue(result.isEmpty());
    }
}
