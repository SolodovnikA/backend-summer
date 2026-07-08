package ru.shift.userimporter.core.specifications;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import ru.shift.userimporter.core.model.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserSpecifications {

    public static Specification<User> hasPhone(Long phone) {
        if (phone == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("phone"),
                String.valueOf(phone));
    }

    public static Specification<User> hasFirstName(String firstName) {
        if (firstName == null) {
            return null;
        }
        return ((root, query, cb) ->
                cb.equal(root.get("firstName"), firstName));
    }

    public static  Specification<User> hasLastName(String lastName) {
        if (lastName == null) {
            return null;
        }
        return ((root, query, cb) ->
                cb.equal(root.get("lastName"), lastName));
    }

    public static Specification<User> hasEmail(String email) {
        if (email == null) {
            return null;
        }
        return ((root, query, cb) ->
                cb.equal(root.get("email"), email));
    }
}
