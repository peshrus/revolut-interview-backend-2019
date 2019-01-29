package com.revolut.interview.backend;

import static java.util.stream.LongStream.range;

import com.revolut.interview.backend.dao.AccountDao;
import com.revolut.interview.backend.dao.AccountDaoImpl;
import com.revolut.interview.backend.model.Account;
import java.math.BigDecimal;
import org.jsmart.zerocode.core.domain.LoadWith;
import org.jsmart.zerocode.core.domain.TestMapping;
import org.jsmart.zerocode.core.domain.UseHttpClient;
import org.jsmart.zerocode.core.httpclient.ssl.SslTrustHttpClient;
import org.jsmart.zerocode.core.runner.parallel.ZeroCodeLoadRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import redis.clients.jedis.JedisPool;

// NOTE to do a proper load testing a separate server with the application is needed
@LoadWith("load-config.properties")
@UseHttpClient(SslTrustHttpClient.class)
@TestMapping(testClass = AppLoadTest.class, testMethod = "transfer")
@RunWith(ZeroCodeLoadRunner.class)
public class LoadTest {

  private static App app;
  private static AccountDao accountDao;

  @BeforeClass
  public static void setUpOnce() throws Exception {
    app = new App();
    app.start();

    accountDao = new AccountDaoImpl(new JedisPool());

    // See https://www.revolut.com/en-US/about-revolut (3M users and 250M transactions in reality)
    // TODO 2 accounts are used because there is no way to set valid values dynamically in transfer.json
    range(1, 3).parallel()
        .forEach(value -> accountDao.create(new Account(BigDecimal.valueOf(10000))));
  }

  @AfterClass
  public static void tearDownOnce() {
    app.stop();
  }

}
