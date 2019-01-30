package com.revolut.interview.backend.dao;

import com.revolut.interview.backend.model.Account;
import java.math.BigDecimal;

public interface AccountDao {

  Account create(Account account);

  Account findById(Long id) throws AccountNotFoundException;

  void transferMoneyTransactionally(BigDecimal sum, Long fromAccountId, Long toAccountId)
      throws AccountNotFoundException, NotEnoughMoneyException, FromAndToAccountsTheSameException;
}
