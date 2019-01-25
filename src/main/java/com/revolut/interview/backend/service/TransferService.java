package com.revolut.interview.backend.service;

import com.revolut.interview.backend.dao.AccountNotFoundException;
import java.math.BigDecimal;

public interface TransferService {

  void transferMoney(BigDecimal sum, Long fromAccountId, Long toAccountId)
      throws NotEnoughMoneyException, AccountNotFoundException;
}
