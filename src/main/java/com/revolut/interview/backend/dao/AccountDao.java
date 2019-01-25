package com.revolut.interview.backend.dao;

import com.revolut.interview.backend.model.Account;

public interface AccountDao {

  Account create(Account account);

  Account findById(Long id) throws AccountNotFoundException;

  void saveTransactionally(Account... accounts);
}
