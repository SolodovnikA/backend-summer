package ru.shift.userimporter.core.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.shift.userimporter.core.model.ErrorCode;
import ru.shift.userimporter.core.model.RowValidationResult;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class RowValidatorTest {

    @Test
    void validRow_shouldReturnValidResult() {
        String [] fields = {"Иван", "Иванов", "Иванович", "ivan@shift.ru", "79995551122", "1995-03-14"};

        RowValidationResult result = RowValidator.validateRow(fields);

        assertTrue(result.valid());
        assertEquals(LocalDate.of(1995, 3, 14), result.birthDate());
    }

    @ParameterizedTest
    @MethodSource("invalidRowsProvider")
    void invalidRow_shouldReturnInvalidResult(String[] fields, ErrorCode expectedErrorCode) {
        RowValidationResult result = RowValidator.validateRow(fields);

        assertFalse(result.valid());
        assertEquals(expectedErrorCode, result.errorCode());

    }

    static Stream<Arguments> invalidRowsProvider() {
        return Stream.of(
                Arguments.of(new String[]{"иван", "Иванов", "Иванович", "ivan@shift.ru", "79995551122", "1995-03-14"},
                        ErrorCode.INVALID_NAME),
                Arguments.of(new String[] {"Иван", "иванов", "Иванович", "ivan@shift.ru", "79995551122", "1995-03-14"},
                        ErrorCode.INVALID_LAST_NAME),
                Arguments.of(new String[] {"Иван", "Иванов", "иванович", "ivan@shift.ru", "79995551122", "1995-03-14"},
                        ErrorCode.INVALID_MIDDLE_NAME),
                Arguments.of(new String[] {"Иван", "Иванов", "Иванович", "not-an-email", "79995551122", "1995-03-14"},
                        ErrorCode.INVALID_EMAIL),
                Arguments.of(new String[] {"Иван", "Иванов", "Иванович", "ivan@gmail.com", "79995551122", "1995-03-14"},
                        ErrorCode.INVALID_EMAIL),
                Arguments.of(new String[] {"Иван", "Иванов", "Иванович", "ivan@shift.ru", "89995551122", "1995-03-14"},
                        ErrorCode.INVALID_PHONE),
                Arguments.of(new String[] {"Иван", "Иванов", "Иванович", "ivan@shift.ru", "79995551122", "14-03-1995"},
                        ErrorCode.INVALID_BIRTHDATE),
                Arguments.of(new String[] {"Иван", "Иванов", "Иванович", "ivan@shift.ru", "79995551122", "2020-01-01"},
                        ErrorCode.INVALID_BIRTHDATE)
        );
    }

    @Test
    void invalidFormat_shouldReturnInvalidResult() {
        String[] field = {"Иван", "Иванов"};

        RowValidationResult result = RowValidator.validateRow(field);

        assertFalse(result.valid());
        assertEquals(ErrorCode.INVALID_FORMAT, result.errorCode());
    }


}
