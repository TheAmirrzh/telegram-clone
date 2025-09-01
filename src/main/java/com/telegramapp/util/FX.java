package com.telegramapp.util;

import javafx.application.Platform;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FX {
    private static final ExecutorService EXEC = new ThreadPoolExecutor(
            2, 8, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            r -> { Thread t = new Thread(r, "db-worker"); t.setDaemon(true); return t; }
    );

    public static <T> void runAsync(Supplier<T> supplier, Consumer<T> onSuccess, Consumer<Throwable> onError){
        EXEC.submit(() -> {
            try {
                T result = supplier.get();
                if (onSuccess != null) Platform.runLater(() -> onSuccess.accept(result));
            } catch (Throwable t){
                if (onError != null) Platform.runLater(() -> onError.accept(t));
            }
        });
    }

    public static void runAsync(Runnable run, Runnable onSuccess, Consumer<Throwable> onError){
        EXEC.submit(() -> {
            try {
                run.run();
                if (onSuccess != null) Platform.runLater(onSuccess);
            } catch (Throwable t){
                if (onError != null) Platform.runLater(() -> onError.accept(t));
            }
        });
    }

    public static void shutdown(){ EXEC.shutdown(); }
}
