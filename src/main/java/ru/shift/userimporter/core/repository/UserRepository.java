package ru.shift.userimporter.core.repository;

import org.springframework.transaction.annotation.Transactional;;
import ru.shift.userimporter.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);

    @Transactional
    default void saveUsers(Collection<User> users) {
        saveAll(users);
    }
}
