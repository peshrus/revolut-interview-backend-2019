package com.revolut.interview.backend;

import static com.revolut.interview.backend.App.DEFAULT_REST_PORT;
import static com.revolut.interview.backend.rest.TransferHandler.PARAM_FROM;
import static com.revolut.interview.backend.rest.TransferHandler.PARAM_TO;
import static java.util.stream.IntStream.rangeClosed;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.revolut.interview.backend.dao.AccountDao;
import com.revolut.interview.backend.dao.AccountDaoImpl;
import com.revolut.interview.backend.model.Account;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.JedisPool;

public class AppIntegrationTest {

  private static App app;
  private static AccountDao accountDao;
  private static Long fromAccountId;
  private static Long toAccountId;

  private HttpClient httpClient;

  @BeforeClass
  public static void setUpOnce() throws Exception {
    app = new App();
    app.start();

    accountDao = new AccountDaoImpl(new JedisPool());
    fromAccountId = accountDao.create(new Account(BigDecimal.valueOf(2000))).getId();
    toAccountId = accountDao.create(new Account(BigDecimal.valueOf(1000))).getId();
  }

  @AfterClass
  public static void tearDownOnce() {
    app.stop();
  }

  @Before
  public void setUp() {
    httpClient = HttpClient.newBuilder().version(Version.HTTP_2).build();
  }

  @Test
  public void transfer_EmptySum() throws Exception {
    transferFixture("", fromAccountId, toAccountId, HttpStatus.NOT_FOUND_404);
  }

  @Test
  public void transfer_NotNumSum() throws Exception {
    transferFixture("a", fromAccountId, toAccountId, HttpStatus.BAD_REQUEST_400);
  }

  @Test
  public void transfer_EmptyFrom() throws Exception {
    transferFixture("100", "", toAccountId.toString(), HttpStatus.BAD_REQUEST_400);
  }

  @Test
  public void transfer_NotNumFrom() throws Exception {
    transferFixture("100", "a", toAccountId.toString(), HttpStatus.BAD_REQUEST_400);
  }

  @Test
  public void transfer_AbsentFrom() throws Exception {
    transferFixture("100", 100500L, toAccountId, HttpStatus.BAD_REQUEST_400);
  }

  @Test
  public void transfer_EmptyTo() throws Exception {
    transferFixture("100", fromAccountId.toString(), "", HttpStatus.BAD_REQUEST_400);
  }

  @Test
  public void transfer_NotNumTo() throws Exception {
    transferFixture("100", fromAccountId.toString(), "a", HttpStatus.BAD_REQUEST_400);
  }

  @Test
  public void transfer_AbsentTo() throws Exception {
    transferFixture("100", fromAccountId, 100500L, HttpStatus.BAD_REQUEST_400);
  }

  @Test
  public void transfer_SameFromToAccount() throws Exception {
    transferFixture("100", fromAccountId, fromAccountId, HttpStatus.BAD_REQUEST_400);
  }

  @Test
  public void transfer_OK() throws Exception {
    // Given & When & Then
    transferFixture("100", fromAccountId, toAccountId, HttpStatus.NO_CONTENT_204);

    // Then
    assertEquals(BigDecimal.valueOf(1900L), accountDao.findById(fromAccountId).getBalance());
    assertEquals(BigDecimal.valueOf(1100L), accountDao.findById(toAccountId).getBalance());
  }

  // FIXED Load test does not test concurrent transfers
  // NOTE it's not a load test but it does what required
  @Test(timeout = 60000)
  public void transfer_ConcurrencyOK() throws Exception {
    // Given
    final int transactionsNum = 10;
    final Long accountId1 = accountDao.create(new Account(BigDecimal.TEN)).getId();
    final Long accountId2 = accountDao.create(new Account(BigDecimal.TEN)).getId();
    final Long accountId3 = accountDao.create(new Account(BigDecimal.TEN)).getId();
    final CountDownLatch startTransaction = new CountDownLatch(1);
    final CountDownLatch transactionsCompleted = new CountDownLatch(transactionsNum * 2);
    final ExecutorService executorService = Executors.newFixedThreadPool(transactionsNum * 2);

    // When
    rangeClosed(1, transactionsNum).forEach(value -> {
          submitTransferFixture(accountId1, accountId2, startTransaction, transactionsCompleted,
              executorService);
          submitTransferFixture(accountId2, accountId3, startTransaction, transactionsCompleted,
              executorService);
        }
    );
    startTransaction.countDown();

    // Then
    transactionsCompleted.await();
    assertEquals(0,
        BigDecimal.valueOf(9.9).compareTo(accountDao.findById(accountId1).getBalance()));
    assertEquals(0,
        BigDecimal.valueOf(10.0).compareTo(accountDao.findById(accountId2).getBalance()));
    assertEquals(0,
        BigDecimal.valueOf(10.1).compareTo(accountDao.findById(accountId3).getBalance()));
  }

  private void submitTransferFixture(Long fromAccountId, Long toAccountId,
      CountDownLatch startTransaction, CountDownLatch transactionsCompleted,
      ExecutorService executorService) {
    executorService.execute(() -> {
      try {
        startTransaction.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      try {
        transferFixture("0.01", fromAccountId, toAccountId, HttpStatus.NO_CONTENT_204);
      } catch (Exception e) {
        fail(e.toString());
      }

      transactionsCompleted.countDown();
    });
  }

  private void transferFixture(String sum, Long fromAccountId, Long toAccountId,
      int expectedStatusCode) throws IOException, InterruptedException {
    transferFixture(sum, fromAccountId.toString(), toAccountId.toString(), expectedStatusCode);
  }

  private void transferFixture(String sum, String fromAccountId, String toAccountId,
      int expectedStatusCode) throws IOException, InterruptedException {
    // Given
    final HttpRequest httpRequest = makeHttpRequest(sum, fromAccountId, toAccountId);

    // When
    final HttpResponse<String> response = httpClient.send(httpRequest, BodyHandlers.ofString());

    // Then
    assertEquals(expectedStatusCode, response.statusCode());
  }

  private HttpRequest makeHttpRequest(String sum, String fromAccountId, String toAccountId) {
    final URI uri = URI
        .create(
            "http://localhost:" + DEFAULT_REST_PORT + "/transfer/" + sum + "?" + PARAM_FROM + "="
                + fromAccountId + "&" + PARAM_TO + "=" + toAccountId);

    return HttpRequest.newBuilder(uri).POST(BodyPublishers.noBody()).build();
  }
}
