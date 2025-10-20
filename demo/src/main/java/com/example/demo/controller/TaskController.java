package com.example.demo.controller;

import com.example.demo.model.Task;
import com.example.demo.model.TaskExecution;
import com.example.demo.repository.TaskRepository;
import com.example.demo.service.TaskService;
import com.example.demo.service.KubernetesService; // <-- 1. IMPORT THE NEW SERVICE

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    @Autowired
    private TaskRepository repo;

    @Autowired
    private TaskService taskService; // You still need this for command validation

    @Autowired
    private KubernetesService kubernetesService; // <-- 2. INJECT THE NEW SERVICE

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
        // Still a good idea to validate commands before saving them
        if (!taskService.isValidCommand(task.getCommand())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsafe or unsupported command");
        }
        return repo.save(task);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        repo.deleteById(id);
    }

    /**
     * THIS IS THE MODIFIED ENDPOINT FOR TASK 2
     * It now uses the KubernetesService to execute the command.
     */
    @PutMapping("/{id}/execute")
    public TaskExecution execute(@PathVariable String id) throws Exception {
        Task t = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Optional: You can re-validate here if you want
        if (!taskService.isValidCommand(t.getCommand())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsafe or unsupported command");
        }

        TaskExecution exec = new TaskExecution();
        exec.setStartTime(Instant.now());

        // 3. CALL THE NEW SERVICE instead of the old one
        String commandOutput = kubernetesService.executeCommandInPod(t.getCommand());
        exec.setOutput(commandOutput);
        exec.setEndTime(Instant.now());

        // Add the new execution record to the task and save it
        t.getTaskExecutions().add(exec);
        repo.save(t);
        return exec;
    }
}