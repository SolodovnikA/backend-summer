package ru.shift.userimporter.core.specifications;

import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import ru.shift.userimporter.core.model.User;

@UtilityClass
public class UserSpecifications {

    public Specification<User> hasPhone(Long phone) {
        if (phone == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("phone"),
                String.valueOf(phone));
    }

    public Specification<User> hasFirstName(String firstName) {
        if (firstName == null) {
            return null;
        }
        return ((root, query, cb) ->
                cb.equal(root.get("firstName"), firstName));
    }

    public Specification<User> hasLastName(String lastName) {
        if (lastName == null) {
            return null;
        }
        return ((root, query, cb) ->
                cb.equal(root.get("lastName"), lastName));
    }

    public Specification<User> hasEmail(String email) {
        if (email == null) {
            return null;
        }
        return ((root, query, cb) ->
                cb.equal(root.get("email"), email));
    }
}
