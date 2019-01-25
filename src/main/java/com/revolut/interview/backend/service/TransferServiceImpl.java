package com.revolut.interview.backend.service;

import com.google.inject.Inject;
import com.revolut.interview.backend.dao.AccountDao;
import com.revolut.interview.backend.dao.AccountNotFoundException;
import com.revolut.interview.backend.model.Account;
import java.math.BigDecimal;

public class TransferServiceImpl implements TransferService {

  private final AccountDao accountDao;

  @Inject
  public TransferServiceImpl(AccountDao accountDao) {
    this.accountDao = accountDao;
  }

  @Override
  public void transferMoney(BigDecimal sum, Long fromAccountId, Long toAccountId)
      throws NotEnoughMoneyException, AccountNotFoundException {
    if (sum.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException(
          "Negative sum: " + sum + " (from: " + fromAccountId + ", to: " + toAccountId + ")");
    }

    final Account to = accountDao.findById(toAccountId);
    final BigDecimal newToBalance = to.getBalance().add(sum);
    to.setBalance(newToBalance);

    final Account from = accountDao.findById(fromAccountId);
    final BigDecimal oldFromBalance = from.getBalance();
    final BigDecimal newFromBalance = oldFromBalance.subtract(sum);
    checkHasEnoughMoney(newFromBalance, oldFromBalance, sum, fromAccountId, toAccountId);
    from.setBalance(newFromBalance);

    accountDao.saveTransactionally(from, to);
  }

  private void checkHasEnoughMoney(BigDecimal newBalance, BigDecimal oldBalance, BigDecimal sum,
      Long fromAccountId, Long toAccountId) throws NotEnoughMoneyException {
    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
      throw new NotEnoughMoneyException(
          "Not enough money: (" + oldBalance + " - " + sum + ") = " + newBalance + " (from: "
              + fromAccountId + ", to: " + toAccountId + ")");
    }
  }
}