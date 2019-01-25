package com.revolut.interview.backend;

import static org.junit.Assert.assertEquals;

import com.revolut.interview.backend.dao.AccountDao;
import com.revolut.interview.backend.dao.AccountDaoImpl;
import com.revolut.interview.backend.model.Account;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.JedisPool;
import redis.embedded.RedisServer;

public class AppIntegrationTest {

  private static RedisServer redisServer;
  private static App app;
  private static Long fromAccountId;
  private static Long toAccountId;

  private HttpClient httpClient;

  @BeforeClass
  public static void setUpOnce() throws Exception {
    redisServer = new RedisServer();
    redisServer.start();
    app = new App();
    app.start();

    final AccountDao accountDao = new AccountDaoImpl(new JedisPool());
    fromAccountId = accountDao.create(new Account(BigDecimal.valueOf(2000))).getId();
    toAccountId = accountDao.create(new Account(BigDecimal.valueOf(1000))).getId();
  }

  @AfterClass
  public static void tearDownOnce() {
    app.stop();
    redisServer.stop();
  }

  @Before
  public void setUp() {
    httpClient = HttpClient.newBuilder().version(Version.HTTP_2).build();
  }

  @Test
  public void testTransfer_OK() throws Exception {
    // Given
    final URI uri = URI
        .create("http://localhost:7000/transfer/100?from=" + fromAccountId + "&to=" + toAccountId);
    final HttpRequest httpRequest = HttpRequest.newBuilder(uri).POST(BodyPublishers.noBody())
        .build();

    // When
    final HttpResponse<String> response = httpClient.send(httpRequest, BodyHandlers.ofString());

    // Then
    assertEquals(HttpStatus.NO_CONTENT_204, response.statusCode());
  }
}
