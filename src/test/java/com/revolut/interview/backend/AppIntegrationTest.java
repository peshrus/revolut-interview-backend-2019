package com.revolut.interview.backend;

import static com.revolut.interview.backend.App.DEFAULT_REST_PORT;
import static com.revolut.interview.backend.rest.TransferHandler.PARAM_FROM;
import static com.revolut.interview.backend.rest.TransferHandler.PARAM_TO;
import static org.junit.Assert.assertEquals;

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
