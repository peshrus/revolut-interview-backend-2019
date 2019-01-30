package com.revolut.interview.backend.dao;

public class FromAndToAccountsTheSameException extends Exception {

  private static final long serialVersionUID = 8458510191241110087L;

  @SuppressWarnings("WeakerAccess")
  public FromAndToAccountsTheSameException(String message) {
    super(message);
  }
}
