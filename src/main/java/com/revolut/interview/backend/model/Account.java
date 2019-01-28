package com.revolut.interview.backend.model;

import java.math.BigDecimal;

public class Account {

  private Long id;
  private BigDecimal balance;

  public Account(BigDecimal balance) {
    this.balance = balance;
  }

  public Account(Long id, BigDecimal balance) {
    this.id = id;
    this.balance = balance;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  @Override
  public String toString() {
    return "Account{" +
        "id=" + id +
        ", balance=" + balance +
        '}';
  }
}
