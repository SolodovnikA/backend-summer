package ru.shift.userimporter.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.shift.userimporter.core.model.*;
import ru.shift.userimporter.core.repository.UploadedFileRepository;
import ru.shift.userimporter.core.validator.RowValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


@Slf4j
@RequiredArgsConstructor
@Component
public class FileProcessingRunner {
    private final FileProcessingErrorService fileProcessingErrorService;
    private final UserService userService;
    private final UploadedFileRepository uploadedFileRepository;


    @Async
    public void runAsync(UploadedFile uploadedFile) {

        try {
            AtomicInteger totalRows = new AtomicInteger(0);
            AtomicInteger insertedRows = new AtomicInteger(0);
            AtomicInteger updatedRows = new AtomicInteger(0);
            AtomicInteger invalidRows = new AtomicInteger(0);

            Map<String, User> usersByPhone = new HashMap<>();
            List<FileProcessingError> errorsToSave = new ArrayList<>();

            try (Stream<String> lines = Files.lines(Path.of(uploadedFile.getStoragePath()),
                    StandardCharsets.UTF_8)) {
                lines.forEach(line -> {
                    int rowNumber = totalRows.incrementAndGet();
                    String[] fields = line.split(",");

                    RowValidationResult validation = RowValidator.validateRow(fields);
                    if (!validation.valid()) {
                        errorsToSave.add(fileProcessingErrorService.buildError(uploadedFile, rowNumber,
                                validation.errorMessage(), validation.errorCode()));
                        invalidRows.incrementAndGet();
                        return;
                    }

                    String phone = fields[4];
                    User user = usersByPhone.get(phone);
                    boolean isUpdate;
                    if (user != null) {
                        isUpdate = true;
                    } else {
                        Optional<User> existingUser = userService.findByPhone(phone);
                        user = existingUser.orElseGet(User::new);
                        isUpdate = existingUser.isPresent();
                    }

                    if (isUpdate) {
                        updatedRows.incrementAndGet();
                    } else {
                        insertedRows.incrementAndGet();
                    }

                    user.setFirstName(fields[0]);
                    user.setLastName(fields[1]);
                    user.setMiddleName(fields[2].isEmpty() ? null : fields[2]);
                    user.setEmail(fields[3]);
                    user.setPhone(phone);
                    user.setBirthDate(validation.birthDate());

                    usersByPhone.put(phone, user);
                });
            } catch (IOException e) {
                throw new RuntimeException("Не удалось прочитать файл", e);
            }

            completeProcessing(uploadedFile, usersByPhone.values(), errorsToSave, totalRows.get(),
                    insertedRows.get(), updatedRows.get(), invalidRows.get());

        } catch (Exception e) {
            log.error("Ошибка при обработке файла с ID {}", uploadedFile.getId(), e);
            markAsFailed(uploadedFile);
        }
    }

    @Transactional
    public void completeProcessing(UploadedFile uploadedFile, Collection<User> users, List<FileProcessingError> errors,
                                   int total, int inserted, int updated, int invalid) {

        userService.saveUsers(users);
        fileProcessingErrorService.saveErrors(errors);

        int valid = total - invalid;
        uploadedFile.setTotalRows(total);
        uploadedFile.setProcessedRows(total);
        uploadedFile.setUpdatedRows(updated);
        uploadedFile.setInsertedRows(inserted);
        uploadedFile.setInvalidRows(invalid);
        uploadedFile.setValidRows(valid);
        uploadedFile.setStatus(FileStatus.DONE);
        uploadedFileRepository.save(uploadedFile);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(UploadedFile uploadedFile) {
        uploadedFile.setStatus(FileStatus.FAILED);
        uploadedFileRepository.save(uploadedFile);
    }

}
