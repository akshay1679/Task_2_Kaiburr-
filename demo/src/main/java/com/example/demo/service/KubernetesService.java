package com.example.demo.service;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class KubernetesService {

    private final KubernetesClient client;

    public KubernetesService() {
        // This client is configured to talk to the K8s API server.
        // When running inside a pod, it will automatically find the cluster credentials.
        this.client = new KubernetesClientBuilder().build();
    }

    /**
     * Executes a shell command inside a new, temporary Kubernetes pod.
     * @param command The shell command to execute.
     * @return The log output from the pod.
     */
    public String executeCommandInPod(String command) {
        final String podName = "task-exec-" + UUID.randomUUID().toString();
        final CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder podLogs = new StringBuilder();

        try {
            // 1. Define the Pod using the busybox image
            Pod pod = new PodBuilder()
                .withNewMetadata()
                    .withName(podName)
                    .withNamespace("default") // Target the default namespace
                .endMetadata()
                .withNewSpec()
                    .addNewContainer()
                        .withName("task-runner")
                        .withImage("busybox:latest")
                        .withCommand("sh", "-c", command) // Execute the command
                    .endContainer()
                    .withRestartPolicy("Never") // Pod should not restart after completion
                .endSpec()
                .build();

            // 2. Create the pod in the cluster
            client.pods().inNamespace("default").create(pod);

            // 3. Watch the pod until it succeeds or fails
            try (Watch watch = client.pods().inNamespace("default").withName(podName).watch(new Watcher<Pod>() {
                @Override
                public void eventReceived(Action action, Pod resource) {
                    String phase = resource.getStatus().getPhase();
                    if ("Succeeded".equals(phase) || "Failed".equals(phase)) {
                        latch.countDown();
                    }
                }

                @Override
                public void onClose(WatcherException cause) {
                    latch.countDown(); // Release latch on error too
                }
            })) {
                // Wait for a maximum of 2 minutes for the pod to complete
                if (!latch.await(2, TimeUnit.MINUTES)) {
                    return "Error: Pod execution timed out.";
                }
            }

            // 4. Retrieve the logs from the completed pod
            podLogs.append(client.pods().inNamespace("default").withName(podName).getLog());

        } catch (Exception e) {
            return "Error executing command in pod: " + e.getMessage();
        } finally {
            // 5. Always clean up and delete the pod
            client.pods().inNamespace("default").withName(podName).delete();
        }

        return podLogs.toString();
    }
}