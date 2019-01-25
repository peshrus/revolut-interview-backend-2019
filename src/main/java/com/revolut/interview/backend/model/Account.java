package com.revolut.interview.backend.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
  private Long id;
  private BigDecimal balance;

  public Account(BigDecimal balance) {
    this.balance = balance;
  }
}
