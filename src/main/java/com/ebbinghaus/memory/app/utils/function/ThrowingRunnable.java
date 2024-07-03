package com.ebbinghaus.memory.app.utils.function;

@FunctionalInterface
public interface ThrowingRunnable {
  void run() throws Exception;
}
