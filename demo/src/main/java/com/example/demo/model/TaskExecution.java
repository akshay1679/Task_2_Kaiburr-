package com.example.demo.model;

import java.time.Instant;

public class TaskExecution {
    private Instant startTime;
    private Instant endTime;
    private String output;
    private int exitCode;

    public TaskExecution() {}

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public int getExitCode() { return exitCode; }
    public void setExitCode(int exitCode) { this.exitCode = exitCode; }
}
