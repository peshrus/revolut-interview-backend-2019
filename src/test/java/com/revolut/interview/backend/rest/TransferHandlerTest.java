package com.revolut.interview.backend.rest;

import static com.revolut.interview.backend.rest.TransferHandler.ERR_MSG;
import static com.revolut.interview.backend.rest.TransferHandler.PARAM_FROM;
import static com.revolut.interview.backend.rest.TransferHandler.PARAM_SUM;
import static com.revolut.interview.backend.rest.TransferHandler.PARAM_TO;
import static com.revolut.interview.backend.rest.TransferHandler.PATH;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revolut.interview.backend.dao.AccountDao;
import io.javalin.Context;
import io.javalin.Javalin;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TransferHandlerTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();
  private AccountDao accountDaoMock;
  private TransferHandler transferHandler;
  private int status;

  @Before
  public void setUp() {
    accountDaoMock = mock(AccountDao.class);
    transferHandler = new TransferHandler(accountDaoMock);
  }

  @Test
  public void handle_EmptySum() throws Exception {
    handleErrorFixture("", "1", "2");
  }

  @Test
  public void handle_NotNumSum() throws Exception {
    handleErrorFixture("a", "1", "2");
  }

  @Test
  public void handle_EmptyFrom() throws Exception {
    handleErrorFixture("100", "", "2");
  }

  @Test
  public void handle_NotNumFrom() throws Exception {
    handleErrorFixture("100", "a", "2");
  }

  @Test
  public void handle_EmptyTo() throws Exception {
    handleErrorFixture("100", "1", "");
  }

  @Test
  public void handle_NotNumTo() throws Exception {
    handleErrorFixture("100", "1", "a");
  }

  @Test
  public void handle_OK() throws Exception {
    // Given
    final String sumStr = "100";
    final String fromStr = "1";
    final String toStr = "2";
    final Context ctx = makeContext(sumStr, fromStr, toStr);

    // When
    transferHandler.handle(ctx);

    // Then
    verify(accountDaoMock)
        .transferMoneyTransactionally(new BigDecimal(sumStr), Long.valueOf(fromStr),
            Long.valueOf(toStr));
    assertEquals(HttpStatus.NO_CONTENT_204, status);
  }

  private void handleErrorFixture(String sumStr, String fromStr, String toStr) throws Exception {
    // Given
    final Context ctx = makeContext(sumStr, fromStr, toStr);

    // Then
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(ERR_MSG);

    // When
    transferHandler.handle(ctx);
  }

  private Context makeContext(String sumStr, String fromStr, String toStr)
      throws NoSuchFieldException, IllegalAccessException {
    final HttpServletRequest requestMock = mock(HttpServletRequest.class);
    when(requestMock.getQueryString())
        .thenReturn(PARAM_FROM + "=" + fromStr + "&" + PARAM_TO + "=" + toStr);

    final HttpServletResponse responseMock = mock(HttpServletResponse.class);
    doAnswer(invocation -> {
      status = invocation.getArgument(0);
      return null;
    }).when(responseMock).setStatus(anyInt());

    final Context context = new Context(requestMock, responseMock, mock(Javalin.class));
    setField(context, "pathParamMap", Collections.singletonMap(PARAM_SUM, sumStr));
    setField(context, "matchedPath", PATH);

    return context;
  }

  private void setField(Context context, String fieldName, Object fieldValue)
      throws NoSuchFieldException, IllegalAccessException {
    final Field pathParamMap = Context.class.getDeclaredField(fieldName);
    pathParamMap.setAccessible(true);
    pathParamMap.set(context, fieldValue);
  }
}