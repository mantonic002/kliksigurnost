package com.kliksigurnost.demo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class CloudflareApiException extends RuntimeException {
  private HttpStatusCode statusCode;

  public CloudflareApiException(String message) {
    super(message);
  }

  public CloudflareApiException(String message, Throwable cause) {
    super(message, cause);
  }

  public CloudflareApiException(String message, HttpStatusCode statusCode) {
    super(message);
    this.statusCode = statusCode;
  }
}