package com.revolut.interview.backend.dao;

public class NotEnoughMoneyException extends Exception {

  private static final long serialVersionUID = -7092570773428424585L;

  @SuppressWarnings("WeakerAccess")
  public NotEnoughMoneyException(String message) {
    super(message);
  }
}
