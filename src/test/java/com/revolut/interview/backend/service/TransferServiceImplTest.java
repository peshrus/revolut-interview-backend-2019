package com.revolut.interview.backend.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revolut.interview.backend.dao.AccountDao;
import com.revolut.interview.backend.dao.AccountNotFoundException;
import com.revolut.interview.backend.model.Account;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TransferServiceImplTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();
  private AccountDao accountDaoMock;
  private TransferService transferService;

  @Before
  public void setUp() {
    accountDaoMock = mock(AccountDao.class);
    transferService = new TransferServiceImpl(accountDaoMock);
  }

  @Test
  public void transferMoney_NegativeSum() throws Exception {
    // Then
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Negative sum: -1 (from: 1, to: 2)");

    // When
    transferService.transferMoney(BigDecimal.valueOf(-1L), 1L, 2L);
  }

  @Test
  public void transferMoney_FromAccountNotFound() throws Exception {
    // Given
    final long fromAccountId = 1L;
    final String fromAccountIdStr = String.valueOf(fromAccountId);

    when(accountDaoMock.findById(fromAccountId))
        .thenThrow(new AccountNotFoundException(fromAccountIdStr));

    // Then
    thrown.expect(AccountNotFoundException.class);
    thrown.expectMessage(fromAccountIdStr);

    // When
    transferService.transferMoney(BigDecimal.ONE, fromAccountId, 2L);
  }

  @Test
  public void transferMoney_ToAccountNotFound() throws Exception {
    // Given
    final long fromAccountId = 1L;
    final long toAccountId = 2L;
    final String toAccountIdStr = String.valueOf(toAccountId);

    when(accountDaoMock.findById(fromAccountId))
        .thenReturn(new Account(fromAccountId, BigDecimal.ONE));
    when(accountDaoMock.findById(toAccountId))
        .thenThrow(new AccountNotFoundException(toAccountIdStr));

    // Then
    thrown.expect(AccountNotFoundException.class);
    thrown.expectMessage(toAccountIdStr);

    // When
    transferService.transferMoney(BigDecimal.ONE, fromAccountId, toAccountId);
  }

  @Test
  public void transferMoney_NotEnoughMoney() throws Exception {
    // Given
    final long fromAccountId = 1L;
    final long toAccountId = 2L;
    final Account fromAccount = new Account(fromAccountId, BigDecimal.ONE);
    final Account toAccount = new Account(toAccountId, BigDecimal.ONE);
    final BigDecimal sum = BigDecimal.TEN;

    when(accountDaoMock.findById(fromAccountId)).thenReturn(fromAccount);
    when(accountDaoMock.findById(toAccountId)).thenReturn(toAccount);

    // Then
    thrown.expect(NotEnoughMoneyException.class);
    thrown.expectMessage(
        "Not enough money: (" + fromAccount.getBalance() + " - " + sum + ") = " + fromAccount
            .getBalance().subtract(sum) + " (from: " + fromAccountId + ", to: " + toAccountId
            + ")");

    // When
    transferService.transferMoney(sum, fromAccountId, toAccountId);
  }

  @Test
  public void transferMoney_OK() throws Exception {
    // Given
    final long fromAccountId = 1L;
    final long toAccountId = 2L;
    final Account fromAccount = new Account(fromAccountId, BigDecimal.ONE);
    final Account toAccount = new Account(toAccountId, BigDecimal.ONE);

    when(accountDaoMock.findById(fromAccountId)).thenReturn(fromAccount);
    when(accountDaoMock.findById(toAccountId)).thenReturn(toAccount);

    // When
    transferService.transferMoney(BigDecimal.ONE, fromAccountId, toAccountId);

    // Then
    verify(accountDaoMock).saveAllTransactionally(fromAccount, toAccount);
  }
}