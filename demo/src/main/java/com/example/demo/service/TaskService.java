package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.example.demo.model.TaskExecution;

@Service
public class TaskService {

    // Whitelist of allowed
    private static final List<String> WHITELIST = List.of("echo", "date", "uptime", "ls", "whoami", "cmd");

    public boolean isValidCommand(String command) {
        if (command == null || command.isBlank()) return false;
        String base = command.trim().split("\\s+")[0].toLowerCase();
        return WHITELIST.contains(base);
    }


    public TaskExecution runCommand(String command, long timeoutSeconds) throws Exception {
        TaskExecution exec = new TaskExecution();
        exec.setStartTime(Instant.now());

        // Use bash -c on Windows you may use "cmd.exe /c" but using cross-platform: detect OS
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        ProcessBuilder pb;
        if (isWindows) {
            pb = new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            pb = new ProcessBuilder("bash", "-c", command);
        }
        pb.redirectErrorStream(true);
        Process p = pb.start();

        boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            exec.setOutput("Process timed out after " + timeoutSeconds + " seconds");
            exec.setExitCode(-1);
            exec.setEndTime(Instant.now());
            return exec;
        }

        int exitCode = p.exitValue();
        InputStream is = p.getInputStream();
        String output = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        exec.setEndTime(Instant.now());
        exec.setExitCode(exitCode);
        exec.setOutput(output);
        return exec;
    }
}
