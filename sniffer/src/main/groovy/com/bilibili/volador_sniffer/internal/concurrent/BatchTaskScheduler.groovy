package com.bilibili.volador_sniffer.internal.concurrent

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
class BatchTaskScheduler {

    ExecutorService executorService
    List< ? extends ITask> tasks = new ArrayList<>()

    BatchTaskScheduler() {
        executorService = Executors.newScheduledThreadPool(Runtime.runtime.availableProcessors() + 1)
    }

    public <T extends ITask> void addTask(T task) {
        tasks << task
    }

    void execute() {
        executorService.invokeAll(tasks)

        tasks.clear()
    }

}
