package com.revolut.interview.backend.dao;

public class AccountNotFoundException extends Exception {

  private static final long serialVersionUID = 7241613258348808765L;

  public AccountNotFoundException(String message) {
    super(message);
  }
}
