package com.revolut.interview.backend.dao;

import com.google.inject.Inject;
import com.revolut.interview.backend.model.Account;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.Transaction;

public class AccountDaoImpl implements AccountDao {

  private static final String KEY_UNIQUE_IDS = "unique_ids";
  private static final String KEY_AUTHOR = "author";
  private static final String KEY_BALANCE = "balance";

  private final JedisPoolAbstract jedisPool;

  @Inject
  public AccountDaoImpl(JedisPoolAbstract jedisPool) {
    this.jedisPool = jedisPool;
  }

  @Override
  public Account create(Account account) {
    try (Jedis jedis = jedisPool.getResource()) {
      final Long newId = jedis.hincrBy(KEY_UNIQUE_IDS, KEY_AUTHOR, 1);
      jedis.hmset(makeAccountKey(newId), makeBalanceHash(account));
      account.setId(newId);

      return account;
    }
  }

  private String makeAccountKey(Long newId) {
    return KEY_AUTHOR + ":" + newId;
  }

  private Map<String, String> makeBalanceHash(Account account) {
    return Collections.singletonMap(KEY_BALANCE, account.getBalance().toString());
  }

  @Override
  public Account findById(Long id) throws AccountNotFoundException {
    List<String> accountFields;

    try (Jedis jedis = jedisPool.getResource()) {
      accountFields = jedis.hmget(makeAccountKey(id), KEY_BALANCE);
    }

    if (accountFields.size() == 0) {
      throw new AccountNotFoundException(id.toString());
    }

    final BigDecimal balance = new BigDecimal(accountFields.get(0));

    return new Account(id, balance);
  }

  @Override
  public void saveTransactionally(Account... accounts) {
    try (Jedis jedis = jedisPool.getResource()) {
      final Transaction transaction = jedis.multi();

      for (Account account : accounts) {
        transaction.hmset(makeAccountKey(account.getId()), makeBalanceHash(account));
      }

      transaction.exec();
    }
  }
}
