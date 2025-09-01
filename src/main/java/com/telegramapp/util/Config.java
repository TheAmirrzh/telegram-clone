package com.telegramapp.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = Config.class.getResourceAsStream("/application.properties")){
            if (in != null) props.load(in);
        } catch (IOException ignored) { }
    }

    public static String get(String key, String def){
        return props.getProperty(key, def);
    }
}
