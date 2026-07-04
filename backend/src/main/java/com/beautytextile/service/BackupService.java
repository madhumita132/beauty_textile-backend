package com.beautytextile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Daily 1 AM database backup.
 * Dumps essential tables to /backups/beautytextile_YYYYMMDD.sql
 *
 * NOTE: Requires mysqldump on the PATH in the runtime environment.
 * On Render/Railway, use their built-in scheduled backup instead,
 * or point this to a cloud bucket in production.
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final List<String> TABLES = List.of(
            "categories", "products", "orders", "order_items", "billing", "billing_items");

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPass;

    private final JdbcTemplate jdbc;

    public BackupService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every day at 01:00 AM. Cron: second minute hour day month weekday */
    @Scheduled(cron = "0 0 1 * * *")
    public void runDailyBackup() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path backupDir = Paths.get("backups").toAbsolutePath();
        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            log.error("Backup directory creation failed: {}", e.getMessage());
            return;
        }

        Path outFile = backupDir.resolve("beautytextile_" + date + ".sql");
        String dbName = extractDbName(jdbcUrl);

        String[] cmd = buildMysqldumpCommand(dbName, outFile);
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().put("MYSQL_PWD", dbPass);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exit = process.waitFor();
            if (exit == 0) {
                log.info("Daily backup created: {}", outFile);
            } else {
                String err = new String(process.getInputStream().readAllBytes());
                log.error("Backup failed (exit {}): {}", exit, err);
            }
        } catch (Exception e) {
            log.error("Backup process error: {}", e.getMessage());
        }
    }

    private String[] buildMysqldumpCommand(String dbName, Path outFile) {
        // Extract host and port from JDBC URL
        String host = "localhost";
        String port = "3306";
        try {
            String stripped = jdbcUrl.replace("jdbc:mysql://", "");
            String hostPort = stripped.split("/")[0];
            if (hostPort.contains(":")) {
                host = hostPort.split(":")[0];
                port = hostPort.split(":")[1];
            } else {
                host = hostPort;
            }
        } catch (Exception ignored) {}

        return new String[]{
                "mysqldump",
                "-h", host,
                "-P", port,
                "-u", dbUser,
                "--single-transaction",
                "--no-tablespaces",
                "--result-file=" + outFile.toAbsolutePath(),
                dbName
        };
    }

    private String extractDbName(String url) {
        try {
            String path = url.split("\\?")[0];
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (Exception e) {
            return "beauty_textile";
        }
    }
}
