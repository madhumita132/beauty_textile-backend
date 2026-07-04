package com.beautytextile.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Backup endpoint: streams a mysqldump of the beauty_textile database.
 * Admin-only (secured in SecurityConfig).
 *
 * GET /api/admin/backup   → triggers mysqldump and sends as .sql download
 */
@RestController
@RequestMapping("/api/admin")
public class BackupController {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${DB_USERNAME:root}")
    private String dbUser;

    @Value("${DB_PASSWORD:root}")
    private String dbPass;

    @GetMapping("/backup")
    public void downloadBackup(HttpServletResponse response) throws IOException {
        // Extract DB name from JDBC URL
        String dbName = extractDbName(datasourceUrl);

        String filename = "beauty-textile-backup-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + ".sql";

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setCharacterEncoding("UTF-8");

        // Build mysqldump command
        ProcessBuilder pb = new ProcessBuilder(
                "mysqldump",
                "-u", dbUser,
                "-p" + dbPass,          // no space between -p and password
                "--single-transaction",
                "--routines",
                "--triggers",
                "--add-drop-table",
                "--complete-insert",
                dbName
        );
        pb.redirectErrorStream(false);

        Process process = pb.start();

        // Stream dump output directly to HTTP response
        byte[] buf = new byte[8192];
        int n;
        try (var in = process.getInputStream();
             var out = response.getOutputStream()) {
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
        }

        // Capture any error output
        try (BufferedReader err = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            StringBuilder errMsg = new StringBuilder();
            while ((line = err.readLine()) != null) {
                if (!line.startsWith("mysqldump: [Warning]")) {   // suppress password warning
                    errMsg.append(line).append("\n");
                }
            }
            // If there was a non-warning error, we can't change the response now (headers sent)
            // but at least log it
            if (!errMsg.isEmpty()) {
                System.err.println("[BackupController] mysqldump stderr: " + errMsg);
            }
        }
    }

    private String extractDbName(String jdbcUrl) {
        // jdbc:mysql://localhost:3306/beauty_textile?...
        String path = jdbcUrl.replace("jdbc:mysql://", "");
        int slash = path.indexOf('/');
        if (slash < 0) return "beauty_textile";
        String after = path.substring(slash + 1);
        int q = after.indexOf('?');
        return q >= 0 ? after.substring(0, q) : after;
    }
}
