package ru.shift.userimporter.core.repository;

import ru.shift.userimporter.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
