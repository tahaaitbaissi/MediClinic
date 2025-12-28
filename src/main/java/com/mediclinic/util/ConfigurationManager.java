package com.mediclinic.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationManager {

    private static ConfigurationManager instance;
    private Properties properties;

    private ConfigurationManager() {
        properties = new Properties();
        loadProperties();
    }

    public static ConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (ConfigurationManager.class) {
                if (instance == null) {
                    instance = new ConfigurationManager();
                }
            }
        }
        return instance;
    }

    private void loadProperties() {
        try {
            InputStream defaultProps = getClass()
                .getClassLoader()
                .getResourceAsStream("application.properties");

            if (defaultProps != null) {
                properties.load(defaultProps);
                defaultProps.close();
            }

            InputStream localProps = getClass()
                .getClassLoader()
                .getResourceAsStream("application-local.properties");

            if (localProps != null) {
                properties.load(localProps);
                localProps.close();
                System.out.println("Loaded local configuration from application-local.properties");
            }

        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getProperty(String key) {
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);

        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    public int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer value for " + key + ": " + value);
            }
        }
        return defaultValue;
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    public String getEmailUsername() {
        return getProperty("email.username");
    }

    public String getEmailPassword() {
        return getProperty("email.password");
    }

    public String getSmtpHost() {
        return getProperty("email.smtp.host", "smtp.gmail.com");
    }

    public int getSmtpPort() {
        return getIntProperty("email.smtp.port", 587);
    }

    public boolean isSmtpAuthEnabled() {
        return getBooleanProperty("email.smtp.auth", true);
    }

    public boolean isStartTlsEnabled() {
        return getBooleanProperty("email.smtp.starttls.enable", true);
    }

    public String getSmtpSslTrust() {
        return getProperty("email.smtp.ssl.trust", "smtp.gmail.com");
    }

    public void reload() {
        properties.clear();
        loadProperties();
    }
}
