package ru.shift.userimporter.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HashCalculator {

    public static String computeHash(MultipartFile file) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e){
            throw new IllegalArgumentException("Алгоритм SHA-1 недоступен", e);
        }

        try(DigestInputStream digestInputStream = new DigestInputStream(file.getInputStream(),
                digest)) {
            digestInputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось получить содержимое файла", e);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
