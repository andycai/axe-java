package com.iwayee.activity.utils;

public class TokenExpiredException extends RuntimeException {
  public TokenExpiredException() {
    super();
  }

  public TokenExpiredException(String s) {
    super(s);
  }

  public TokenExpiredException(String message, Throwable cause) {
    super(message, cause);
  }

  public TokenExpiredException(Throwable cause) {
    super(cause);
  }
}
