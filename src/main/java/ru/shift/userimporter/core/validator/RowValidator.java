package ru.shift.userimporter.core.validator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.shift.userimporter.core.model.ErrorCode;
import ru.shift.userimporter.core.model.RowValidationResult;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RowValidator {
    private static final String NAME_PATTERN = "^[А-Я][а-я'\\- ]{2,49}$";
    private static final String EMAIL_PATTERN = "^\\w[\\w.+-]*@[\\w-]+\\.[a-zA-Z]{2,}$";
    private static final String PHONE_PATTERN  = "^7\\d{10}$";
    private static final Set<String> SUPPORTED_EMAIL_DOMAINS = Set.of("shift.ru", "shift.com");

    public static RowValidationResult validateRow(String[] fields) {
        if (fields.length != 6) {
            return RowValidationResult.invalid(ErrorCode.INVALID_FORMAT, "Неверное количество полей в строке");
        }

        if (!fields[0].matches(NAME_PATTERN)) {
            return RowValidationResult.invalid(ErrorCode.INVALID_NAME, "Неверный формат имени!");
        }

        if (!fields[1].matches(NAME_PATTERN)) {
            return RowValidationResult.invalid(ErrorCode.INVALID_LAST_NAME, "Неверный формат фамилии!");
        }

        if (!fields[2].isEmpty() && !fields[2].matches(NAME_PATTERN)) {
            return RowValidationResult.invalid(ErrorCode.INVALID_MIDDLE_NAME, "Неверный формат отчества!");
        }

        if (!fields[3].matches(EMAIL_PATTERN)) {
            return RowValidationResult.invalid(ErrorCode.INVALID_EMAIL, "Неверный формат почты!");
        }

        String domain = fields[3].substring(fields[3].indexOf("@") + 1);
        if (!SUPPORTED_EMAIL_DOMAINS.contains(domain)) {
            return RowValidationResult.invalid(ErrorCode.INVALID_EMAIL, "Неверный формат почты!");
        }

        if (!fields[4].matches(PHONE_PATTERN)) {
            return RowValidationResult.invalid(ErrorCode.INVALID_PHONE, "Неверный формат номера телефона!");
        }

        try {
            LocalDate birthDate = LocalDate.parse(fields[5]);
            Period period = Period.between(birthDate, LocalDate.now());
            if (period.getYears() < 18) {
                return RowValidationResult.invalid(ErrorCode.INVALID_BIRTHDATE,
                        "Пользователь младше 18-ти лет");
            }
            return RowValidationResult.valid(birthDate);
        } catch (DateTimeParseException e) {
            return RowValidationResult.invalid(ErrorCode.INVALID_BIRTHDATE,
                    "Неверный формат даты рождения!");
        }
    }
}
