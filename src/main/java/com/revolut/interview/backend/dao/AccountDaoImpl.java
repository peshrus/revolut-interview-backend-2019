package com.revolut.interview.backend.dao;

import static java.util.Collections.singletonMap;

import com.google.inject.Inject;
import com.revolut.interview.backend.model.Account;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.Transaction;

public class AccountDaoImpl implements AccountDao {

  private static final Logger LOG = LoggerFactory.getLogger(AccountDaoImpl.class);

  private static final String KEY_UNIQUE_IDS = "unique_ids";
  private static final String KEY_AUTHOR = "author";
  private static final String FIELD_BALANCE = "balance";

  private final JedisPoolAbstract jedisPool;

  @Inject
  public AccountDaoImpl(JedisPoolAbstract jedisPool) {
    this.jedisPool = jedisPool;
  }

  @Override
  public Account create(Account account) {
    try (Jedis jedis = jedisPool.getResource()) {
      final Long newId = jedis.hincrBy(KEY_UNIQUE_IDS, KEY_AUTHOR, 1);
      jedis.hmset(makeAccountKey(newId), makeFieldsHash(account));
      account.setId(newId);

      LOG.debug("Created: " + account);

      return account;
    }
  }

  private String makeAccountKey(Long newId) {
    return KEY_AUTHOR + ":" + newId;
  }

  private Map<String, String> makeFieldsHash(Account account) {
    return singletonMap(FIELD_BALANCE, account.getBalance().toString());
  }

  @Override
  public Account findById(Long id) throws AccountNotFoundException {
    final List<String> accountFields = getAccountFields(id);
    final BigDecimal balance = new BigDecimal(accountFields.get(0));
    final Account result = new Account(id, balance);

    LOG.debug("Found: " + result);

    return result;
  }

  private List<String> getAccountFields(Long id) throws AccountNotFoundException {
    List<String> accountFields;

    try (Jedis jedis = jedisPool.getResource()) {
      accountFields = getBalance(jedis, makeAccountKey(id));
    }

    if (accountFields.size() == 0 || accountFields.get(0) == null) {
      throw new AccountNotFoundException(id.toString());
    }

    return accountFields;
  }

  private List<String> getBalance(Jedis jedis, String fromKey) {
    return jedis.hmget(fromKey, FIELD_BALANCE);
  }

  // FIXED The solution is not synchronised: balances might change between get and set operations
  // which will cause the inconsistent state
  // NOTE it's not implemented using HINCRBYFLOAT because of not precise operations in Redis
  @Override
  public void transferMoneyTransactionally(BigDecimal sum, Long fromAccountId, Long toAccountId)
      throws AccountNotFoundException, NotEnoughMoneyException, FromAndToAccountsTheSameException {
    checkSum(sum, fromAccountId, toAccountId);
    checkAccountIds(fromAccountId, toAccountId);

    try (Jedis jedis = jedisPool.getResource()) {
      final String fromKey = makeAccountKey(fromAccountId);
      final String toKey = makeAccountKey(toAccountId);
      List<Object> transactionResult;

      do {
        jedis.watch(fromKey, toKey);

        final List<String> fromBalance = getBalanceAndCheck(fromKey, fromAccountId, jedis);
        final BigDecimal oldFromBalance = new BigDecimal(fromBalance.get(0));
        final List<String> toBalance = getBalanceAndCheck(toKey, toAccountId, jedis);
        final BigDecimal oldToBalance = new BigDecimal(toBalance.get(0));
        final BigDecimal newFromBalance = oldFromBalance.subtract(sum);
        final BigDecimal newToBalance = oldToBalance.add(sum);

        checkHasEnoughMoney(newFromBalance, oldFromBalance, sum, fromAccountId, toAccountId);

        final Transaction transaction = jedis.multi();
        transaction.hmset(fromKey, singletonMap(FIELD_BALANCE, newFromBalance.toString()));
        transaction.hmset(toKey, singletonMap(FIELD_BALANCE, newToBalance.toString()));
        transactionResult = transaction.exec();

        LOG.debug(String.valueOf(transactionResult));
      } while (transactionResult == null);
    }
  }

  private void checkSum(BigDecimal sum, Long fromAccountId, Long toAccountId) {
    if (sum.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException(
          "Negative sum: " + sum + " (from: " + fromAccountId + ", to: " + toAccountId + ")");
    }
  }

  private void checkAccountIds(Long fromAccountId, Long toAccountId)
      throws FromAndToAccountsTheSameException {
    if (fromAccountId.equals(toAccountId)) {
      throw new FromAndToAccountsTheSameException(fromAccountId.toString());
    }
  }

  @NotNull
  private List<String> getBalanceAndCheck(String fromKey, Long accountId, Jedis jedis)
      throws AccountNotFoundException {
    final List<String> result = getBalance(jedis, fromKey);

    if (result.get(0) == null) {
      throw new AccountNotFoundException(accountId.toString());
    }

    return result;
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
