package com.vacancyparser.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@Slf4j
public class LoggingService {

    private final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    private final Thread loggingThread;

    public LoggingService() {
        loggingThread = new Thread(this::processLogs, "LoggingDaemon");
        loggingThread.setDaemon(true);
        loggingThread.start();
    }

    public void log(String message) {
        try {
            logQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processLogs() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String message = logQueue.take();
                log.info("[DAEMON LOG] {}", message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown() {
        loggingThread.interrupt();
    }
}
