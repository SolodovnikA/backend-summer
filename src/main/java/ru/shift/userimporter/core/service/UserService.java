package ru.shift.userimporter.core.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.shift.userimporter.core.model.OffsetPageRequest;
import ru.shift.userimporter.core.model.User;
import ru.shift.userimporter.core.repository.UserRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static ru.shift.userimporter.core.specifications.UserSpecifications.hasPhone;
import static ru.shift.userimporter.core.specifications.UserSpecifications.hasFirstName;
import static ru.shift.userimporter.core.specifications.UserSpecifications.hasLastName;
import static ru.shift.userimporter.core.specifications.UserSpecifications.hasEmail;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    @Transactional
    public void saveUsers(Collection<User> users) {
        userRepository.saveAll(users);
    }

    public List<User> findWithFilter(Long phone, String firstName, String lastName,
                                     String email, long offset, int limit) {
        Specification<User> spec = Specification.allOf(
                hasPhone(phone),
                hasFirstName(firstName),
                hasLastName(lastName),
                hasEmail(email)
        );

        OffsetPageRequest req = new OffsetPageRequest(offset, limit);
        return userRepository.findAll(spec, req).getContent();

    }
}
