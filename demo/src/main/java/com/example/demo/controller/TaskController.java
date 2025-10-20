package com.example.demo.controller;

import com.example.demo.model.Task;
import com.example.demo.model.TaskExecution;
import com.example.demo.repository.TaskRepository;
import com.example.demo.service.TaskService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    @Autowired
    private TaskRepository repo;

    @Autowired
    private TaskService taskService;

    @GetMapping
    public List<Task> all() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Task get(@PathVariable String id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/search")
    public List<Task> search(@RequestParam String name) {
        List<Task> res = repo.findByNameContainingIgnoreCase(name);
        if (res.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return res;
    }

    @PutMapping
    public Task upsert(@RequestBody Task task) {
        if (!taskService.isValidCommand(task.getCommand())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsafe or unsupported command");
        }
        return repo.save(task);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        repo.deleteById(id);
    }

    @PutMapping("/{id}/execute")
    public TaskExecution execute(@PathVariable String id) throws Exception {
        Task t = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!taskService.isValidCommand(t.getCommand())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsafe or unsupported command");
        }

        TaskExecution exec = taskService.runCommand(t.getCommand(), 10); // 10s timeout
        t.getTaskExecutions().add(exec);
        repo.save(t);
        return exec;
    }
    @PostMapping("/{id}/execute")
    public ResponseEntity<String> executeTask(@PathVariable String id) {
    Optional<Task> taskOptional = repo.findById(id);
    if (taskOptional.isPresent()) {
        Task task = taskOptional.get();
        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", task.getCommand())
        .redirectErrorStream(true)
        .start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.lines().collect(Collectors.joining("\n"));
            process.waitFor();
            return ResponseEntity.ok("Executed: " + output);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Execution failed: " + e.getMessage());
        }
    } else {
        return ResponseEntity.status(404).body("Task not found");
    }
}

}
