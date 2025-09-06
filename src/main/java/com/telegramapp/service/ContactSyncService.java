package com.telegramapp.service;

import com.telegramapp.model.User;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for importing/exporting contacts and contact synchronization
 */
public class ContactSyncService {
    private final ContactService contactService;

    public ContactSyncService(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * Export contacts to CSV file
     */
    public void exportContacts(String userId, Window owner) throws Exception {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Contacts");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        fileChooser.setInitialFileName("telegram_contacts.csv");

        File file = fileChooser.showSaveDialog(owner);
        if (file == null) return;

        List<User> contacts = contactService.getContacts(userId);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Display Name,Username,Bio,Status");
            for (User contact : contacts) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        escapeCSV(contact.getDisplayName()),
                        escapeCSV(contact.getUsername()),
                        escapeCSV(contact.getBio()),
                        escapeCSV(contact.getStatus())
                );
            }
        }
    }

    /**
     * Import contacts from CSV file
     */
    public ImportResult importContacts(String userId, Window owner) throws Exception {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Contacts");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = fileChooser.showOpenDialog(owner);
        if (file == null) return new ImportResult(0, 0, Collections.emptyList());

        List<String> lines = Files.readAllLines(Paths.get(file.getPath()));
        if (lines.isEmpty()) return new ImportResult(0, 0, Collections.emptyList());

        // Skip header
        lines = lines.subList(1, lines.size());

        int imported = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (String line : lines) {
            try {
                String[] parts = parseCSVLine(line);
                if (parts.length >= 2) {
                    String username = parts[1].trim();
                    // Search for user by username
                    List<User> found = contactService.searchUsersForContacts(username, userId);
                    Optional<User> userOpt = found.stream()
                            .filter(u -> u.getUsername().equalsIgnoreCase(username))
                            .findFirst();

                    if (userOpt.isPresent()) {
                        boolean added = contactService.addContact(userId, userOpt.get().getId());
                        if (added) imported++;
                    } else {
                        errors.add("User not found: " + username);
                        failed++;
                    }
                }
            } catch (Exception e) {
                errors.add("Error processing line: " + line.substring(0, Math.min(50, line.length())));
                failed++;
            }
        }

        return new ImportResult(imported, failed, errors);
    }

    /**
     * Backup contacts to JSON
     */
    public void backupContacts(String userId, Window owner) throws Exception {
        // Implementation for JSON backup
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Backup Contacts");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        File file = fileChooser.showSaveDialog(owner);
        if (file == null) return;

        List<User> contacts = contactService.getContacts(userId);

        // Simple JSON export (you might want to use a proper JSON library)
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("{");
            writer.println("  \"export_date\": \"" + new Date() + "\",");
            writer.println("  \"contacts\": [");

            for (int i = 0; i < contacts.size(); i++) {
                User contact = contacts.get(i);
                writer.println("    {");
                writer.println("      \"username\": \"" + escapeJSON(contact.getUsername()) + "\",");
                writer.println("      \"display_name\": \"" + escapeJSON(contact.getDisplayName()) + "\",");
                writer.println("      \"bio\": \"" + escapeJSON(contact.getBio()) + "\"");
                writer.print("    }");
                if (i < contacts.size() - 1) writer.println(",");
                else writer.println();
            }

            writer.println("  ]");
            writer.println("}");
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    private String escapeJSON(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());

        return result.toArray(new String[0]);
    }

    public static class ImportResult {
        public final int imported;
        public final int failed;
        public final List<String> errors;

        public ImportResult(int imported, int failed, List<String> errors) {
            this.imported = imported;
            this.failed = failed;
            this.errors = errors;
        }
    }
}