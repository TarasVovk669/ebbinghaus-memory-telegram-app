package com.ebbinghaus.memory.app.exception;

public class TelegramCallException extends RuntimeException {

  public TelegramCallException() {}

  public TelegramCallException(String message) {
    super(message);
  }
}
