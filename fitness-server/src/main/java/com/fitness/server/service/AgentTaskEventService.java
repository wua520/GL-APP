package com.fitness.server.service;

import com.fitness.server.dto.agent.TaskResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 可选的任务事件订阅。事件通道只观察任务快照，同步任务接口仍是事实来源。
 */
@Service
public class AgentTaskEventService {
    private static final long EMITTER_TIMEOUT_MS = 5 * 60 * 1000L;
    private static final long POLL_INTERVAL_MS = 500L;

    private final AgentOrchestrator orchestrator;
    private final boolean enabled;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "agent-task-events");
        thread.setDaemon(true);
        return thread;
    });

    public AgentTaskEventService(
            AgentOrchestrator orchestrator,
            @Value("${agent.events.enabled:false}") boolean enabled) {
        this.orchestrator = orchestrator;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public SseEmitter subscribe(Long userId, Long taskId) {
        if (!enabled) {
            throw new IllegalStateException("任务事件流未启用");
        }

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        EventSubscription subscription = new EventSubscription(emitter, userId, taskId);
        emitter.onCompletion(subscription::cancel);
        emitter.onTimeout(subscription::cancel);
        emitter.onError(error -> subscription.cancel());
        subscription.start();
        return emitter;
    }

    private final class EventSubscription {
        private final SseEmitter emitter;
        private final Long userId;
        private final Long taskId;
        private volatile ScheduledFuture<?> future;
        private String lastSignature;

        private EventSubscription(SseEmitter emitter, Long userId, Long taskId) {
            this.emitter = emitter;
            this.userId = userId;
            this.taskId = taskId;
        }

        private void start() {
            future = scheduler.scheduleAtFixedRate(this::publishIfChanged, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }

        private void publishIfChanged() {
            try {
                TaskResponse response = orchestrator.getTask(userId, taskId);
                if (response == null) {
                    cancel();
                    return;
                }
                String signature = signatureOf(response);
                if (!signature.equals(lastSignature)) {
                    emitter.send(SseEmitter.event()
                            .name("task")
                            .id(String.valueOf(taskId))
                            .data(response, MediaType.APPLICATION_JSON));
                    lastSignature = signature;
                }
                if (isTerminal(response.getStatus())) {
                    emitter.complete();
                    cancel();
                }
            } catch (Exception exception) {
                cancel();
                emitter.completeWithError(exception);
            }
        }

        private void cancel() {
            ScheduledFuture<?> current = future;
            if (current != null) {
                current.cancel(false);
            }
        }
    }

    private String signatureOf(TaskResponse response) {
        StringBuilder signature = new StringBuilder()
                .append(response.getStatus()).append('|')
                .append(response.getAssistantMessage()).append('|')
                .append(response.getFailureReason());
        if (response.getPendingActions() != null) {
            response.getPendingActions().forEach(action -> signature.append('|')
                    .append(action.getActionId()).append(':').append(action.getStatus()));
        }
        return signature.toString();
    }

    private boolean isTerminal(String status) {
        return "SUCCEEDED".equals(status) || "FAILED".equals(status) ||
                "BLOCKED".equals(status) || "CANCELLED".equals(status);
    }
}
