package ru.shift.userimporter.core.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.shift.userimporter.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByPhone(String phone);

}
