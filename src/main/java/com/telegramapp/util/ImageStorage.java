package com.telegramapp.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class ImageStorage {
    private static final String BASE_DIR = System.getProperty("user.home") + File.separator + ".telegram-clone" + File.separator + "attachments";

    public static String saveAttachment(File source) throws IOException {
        Path base = Path.of(BASE_DIR);
        if (!Files.exists(base)) Files.createDirectories(base);
        String ext = "";
        String name = source.getName();
        int dot = name.lastIndexOf('.');
        if (dot > -1) ext = name.substring(dot);
        Path target = base.resolve(UUID.randomUUID().toString() + ext);
        Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }
}
