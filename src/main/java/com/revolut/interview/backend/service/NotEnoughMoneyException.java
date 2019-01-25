package com.revolut.interview.backend.service;

public class NotEnoughMoneyException extends Exception {

  private static final long serialVersionUID = -7092570773428424585L;

  public NotEnoughMoneyException(String message) {
    super(message);
  }
}