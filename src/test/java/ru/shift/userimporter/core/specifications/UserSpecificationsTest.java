package ru.shift.userimporter.core.specifications;

import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.jpa.domain.Specification;

import ru.shift.userimporter.core.model.User;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

public class UserSpecificationsTest {

    @ParameterizedTest
    @MethodSource("nullSpecificationsProvider")
    void specification_shouldReturnNull_whenArgumentIsNull(Function<Object, Specification<User>>
                                                        specMethod) {
        assertNull(specMethod.apply(null));
    }

    static Stream<Function<Object, Specification<User>>> nullSpecificationsProvider() {
        return Stream.of(
                phone -> UserSpecifications.hasPhone((Long) phone),
                firstName -> UserSpecifications.hasFirstName((String) firstName),
                lastName -> UserSpecifications.hasLastName((String) lastName),
                email -> UserSpecifications.hasEmail((String) email)
        );
    }

    @Test
    void hasPhone_shouldBuildEqualPredicate_whenPhoneProvided() {
        Root<User> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get("phone")).thenReturn(path);
        when(cb.equal(path,"79995551122")).thenReturn(expectedPredicate);

        Specification<User> spec = UserSpecifications.hasPhone(79995551122L);
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(expectedPredicate, result);
        verify(root).get("phone");
        verify(cb).equal(path,"79995551122");
    }

    @Test
    void hasEmail_shouldBuildEqualPredicate_whenEmailProvided() {
        Root<User> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get("email")).thenReturn(path);
        when(cb.equal(path, "ivan@shift.ru")).thenReturn(expectedPredicate);

        Specification<User> spec = UserSpecifications.hasEmail("ivan@shift.ru");
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(expectedPredicate, result);
        verify(root).get("email");
        verify(cb).equal(path, "ivan@shift.ru");
    }

    @Test
    void hasFirstName_shouldBuildEqualPredicate_whenFirstNameProvided() {
        Root<User> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get("firstName")).thenReturn(path);
        when(cb.equal(path, "Иван")).thenReturn(expectedPredicate);

        Specification<User> spec = UserSpecifications.hasFirstName("Иван");
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(expectedPredicate, result);
        verify(root).get("firstName");
        verify(cb).equal(path, "Иван");
    }

    @Test
    void hasLastName_shouldBuildEqualPredicate_whenLastNameProvided() {
        Root<User> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> path = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        when(root.get("lastName")).thenReturn(path);
        when(cb.equal(path, "Иванов")).thenReturn(expectedPredicate);

        Specification<User> spec  = UserSpecifications.hasLastName("Иванов");
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(expectedPredicate, result);
        verify(root).get("lastName");
        verify(cb).equal(path, "Иванов");
    }
}
