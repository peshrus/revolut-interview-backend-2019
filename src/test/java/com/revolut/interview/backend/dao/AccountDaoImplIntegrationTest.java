package com.revolut.interview.backend.dao;

import static java.util.stream.IntStream.rangeClosed;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.revolut.interview.backend.model.Account;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.embedded.RedisServer;

public class AccountDaoImplIntegrationTest {

  private static RedisServer redisServer;
  private static JedisPool jedisPool;

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  private AccountDao accountDao;

  @BeforeClass
  public static void setUpOnce() throws Exception {
    redisServer = new RedisServer();
    redisServer.start();
    jedisPool = new JedisPool(new JedisPoolConfig());
  }

  @AfterClass
  public static void tearDownOnce() {
    jedisPool.destroy();
    redisServer.stop();
  }

  @Before
  public void setUp() {
    accountDao = new AccountDaoImpl(jedisPool);
  }

  @Test
  public void createFindById_OK() throws AccountNotFoundException {
    // Given
    final Account account = new Account(new BigDecimal(100500.10500));

    // When
    final Account savedAccount = accountDao.create(account);
    final Account foundAccount = accountDao.findById(savedAccount.getId());

    // Then
    assertEquals(account.getBalance(), foundAccount.getBalance());
  }

  @Test(/* Then */ expected = AccountNotFoundException.class)
  public void findById_NotFound() throws AccountNotFoundException {
    // When
    accountDao.findById(100500L);
  }

  @Test
  public void transferMoneyTransactionally_NegativeSum() throws Exception {
    // Then
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Negative sum: -1 (from: 1, to: 2)");

    // When
    accountDao.transferMoneyTransactionally(BigDecimal.valueOf(-1L), 1L, 2L);
  }

  @Test
  public void transferMoneyTransactionally_SameFromToAccount() throws Exception {
    // Given
    final long fromAccountId = 1L;

    // Then
    thrown.expect(FromAndToAccountsTheSameException.class);
    thrown.expectMessage(String.valueOf(fromAccountId));

    // When
    accountDao.transferMoneyTransactionally(BigDecimal.ONE, fromAccountId, fromAccountId);
  }

  @Test
  public void transferMoneyTransactionally_FromAccountNotFound() throws Exception {
    // Given
    final long fromAccountId = 100500L;
    final Account toAccount = accountDao.create(new Account(BigDecimal.ONE));

    // Then
    thrown.expect(AccountNotFoundException.class);
    thrown.expectMessage(String.valueOf(fromAccountId));

    // When
    accountDao.transferMoneyTransactionally(BigDecimal.TEN, fromAccountId, toAccount.getId());
  }

  @Test
  public void transferMoneyTransactionally_ToAccountNotFound() throws Exception {
    // Given
    final Account fromAccount = accountDao.create(new Account(BigDecimal.ONE));
    final long toAccountId = 100500L;

    // Then
    thrown.expect(AccountNotFoundException.class);
    thrown.expectMessage(String.valueOf(toAccountId));

    // When
    accountDao.transferMoneyTransactionally(BigDecimal.TEN, fromAccount.getId(), toAccountId);
  }

  @Test
  public void transferMoneyTransactionally_NotEnoughMoney() throws Exception {
    // Given
    final Account fromAccount = accountDao.create(new Account(BigDecimal.ONE));
    final Account toAccount = accountDao.create(new Account(BigDecimal.TEN));

    // Then
    thrown.expect(NotEnoughMoneyException.class);
    thrown.expectMessage(
        "Not enough money: (1 - 10) = -9 (from: " + fromAccount.getId() + ", to: " + toAccount
            .getId() + ")");

    // When
    accountDao.transferMoneyTransactionally(BigDecimal.TEN, fromAccount.getId(), toAccount.getId());
  }

  @Test(timeout = 5000)
  public void transferMoneyTransactionally_OK() throws Exception {
    // Given
    final int transactionsNum = 10;
    final Account fromAccount = accountDao.create(new Account(BigDecimal.valueOf(transactionsNum)));
    final Account toAccount = accountDao.create(new Account(BigDecimal.ONE));
    final CountDownLatch startTransaction = new CountDownLatch(1);
    final CountDownLatch transactionsCompleted = new CountDownLatch(transactionsNum);
    final ExecutorService executorService = Executors.newFixedThreadPool(transactionsNum);

    try {
      // When
      rangeClosed(1, transactionsNum).forEach(value ->
          executorService.execute(() -> {
            try {
              startTransaction.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }

            try {
              accountDao
                  .transferMoneyTransactionally(BigDecimal.valueOf(0.1), fromAccount.getId(),
                      toAccount.getId());
            } catch (Exception e) {
              fail(e.toString());
            }

            transactionsCompleted.countDown();
          })
      );
      startTransaction.countDown();

      // Then
      transactionsCompleted.await();
      assertEquals(BigDecimal.valueOf(9.0), accountDao.findById(fromAccount.getId()).getBalance());
      assertEquals(BigDecimal.valueOf(2.0), accountDao.findById(toAccount.getId()).getBalance());
    } finally {
      executorService.shutdown();
    }
  }
}