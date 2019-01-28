package com.revolut.interview.backend.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.revolut.interview.backend.model.Account;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.embedded.RedisServer;

public class AccountDaoImplIntegrationTest {

  private static RedisServer redisServer;
  private static JedisPool jedisPool;

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

  @Test(timeout = 5000)
  public void saveAllTransactionally_OK() throws AccountNotFoundException, InterruptedException {
    // Given
    final Account account1 = accountDao.create(new Account(BigDecimal.ONE));
    final Account account2 = accountDao.create(new Account(BigDecimal.TEN));
    final CountDownLatch getIdLatch = new CountDownLatch(1);
    final CountDownLatch checkDbInMiddleOfTransaction = new CountDownLatch(1);
    final CountDownLatch transactionCompleted = new CountDownLatch(1);
    final ExecutorService executorService = Executors.newFixedThreadPool(1);

    account1.setBalance(BigDecimal.ZERO);
    account2.setBalance(BigDecimal.ZERO);

    final Account blockedAccount1 = new Account(account1.getId(), account1.getBalance()) {
      @Override
      public Long getId() {
        try {
          return super.getId();
        } finally {
          checkDbInMiddleOfTransaction.countDown();
        }
      }
    };
    final Account blockedAccount2 = new Account(account2.getId(), account2.getBalance()) {
      @Override
      public Long getId() {
        try {
          getIdLatch.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        return super.getId();
      }
    };

    try {
      // When
      executorService.submit(() -> {
        accountDao.saveAllTransactionally(blockedAccount1, blockedAccount2);
        transactionCompleted.countDown();
      });

      // Then
      checkDbInMiddleOfTransaction.await();
      assertNotEquals(account1.getBalance(), accountDao.findById(account1.getId()).getBalance());
      assertNotEquals(account2.getBalance(), accountDao.findById(account2.getId()).getBalance());
      getIdLatch.countDown();
      transactionCompleted.await();
      assertEquals(account1.getBalance(), accountDao.findById(account1.getId()).getBalance());
      assertEquals(account2.getBalance(), accountDao.findById(account2.getId()).getBalance());
    } finally {
      executorService.shutdown();
    }
  }
}