package com.mmvs.util;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import com.mmvs.exception.BusinessException;

public final class SafeFileNameGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private SafeFileNameGenerator() {
    }

    public static String extensionOf(String originalFileName) {
        String filename = StringUtils.cleanPath(originalFileName == null ? "" : originalFileName);
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new BusinessException("VIDEO_INVALID_FORMAT", "视频文件缺少扩展名", HttpStatus.BAD_REQUEST);
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    public static String storedRelativePath(String videoId, String originalFileName) {
        String extension = extensionOf(originalFileName);
        String safeBaseName = safeBaseName(originalFileName);
        String datePath = LocalDate.now().format(DATE_FORMATTER);
        return datePath + "/" + videoId + "-" + safeBaseName + "." + extension;
    }

    private static String safeBaseName(String originalFileName) {
        String cleaned = StringUtils.cleanPath(originalFileName == null ? "video" : originalFileName);
        int dotIndex = cleaned.lastIndexOf('.');
        String baseName = dotIndex > 0 ? cleaned.substring(0, dotIndex) : cleaned;
        String normalized = Normalizer.normalize(baseName, Normalizer.Form.NFKC);
        String safe = normalized.replaceAll("[^a-zA-Z0-9._-]", "-").replaceAll("-+", "-");
        if (safe.isBlank()) {
            return "video";
        }
        return safe.length() > 48 ? safe.substring(0, 48) : safe;
    }
}
