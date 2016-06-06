package com.mapzen.valhalla;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.mapzen.TestUtils.getFixture;
import static org.fest.assertions.api.Assertions.assertThat;
import retrofit.Endpoint;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

public class HttpHandlerTest {

  String endpoint = "https://valhalla.mapzen.com/";
  HttpHandler httpHandler;

  @Before public void setup() throws IOException {
    httpHandler = new HttpHandler("test", endpoint, RestAdapter.LogLevel.NONE);
  }

  @Test public void shouldHaveApiKey() {
    String key = (String) Whitebox.getInternalState(httpHandler, "apiKey");
    assertThat(key).isEqualTo("test");
  }

  @Test public void shouldHaveEndpoint() {
    final String endpoint =
        (String) Whitebox.getInternalState(httpHandler, "endpoint");
    assertThat(endpoint).startsWith("https://valhalla.mapzen.com/");
  }

  @Test public void shouldHaveLogLevel() {
    final RestAdapter.LogLevel logLevel =
        (RestAdapter.LogLevel) Whitebox.getInternalState(httpHandler, "logLevel");
    assertThat(logLevel).isEqualTo(RestAdapter.LogLevel.NONE);
  }

  @Test public void shouldAddHeaders() throws IOException {
    final MockWebServer server = new MockWebServer();
    server.start();
    server.enqueue(new MockResponse().setBody(getFixture("brooklyn")));
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        String endpoint = server.getUrl("").toString();
        TestHttpHandler httpHandler = new TestHttpHandler("key", endpoint, RestAdapter.LogLevel.NONE);
        Router router = new ValhallaRouter()
            .setHttpHandler(httpHandler)
            .setLocation(new double[] { 40.659241, -73.983776 })
            .setLocation(new double[] { 40.671773, -73.981115 });
        RouteCallback callback = Mockito.mock(RouteCallback.class);
        router.setCallback(callback);
        router.fetch();
        assertThat(httpHandler.headersAdded).isTrue();
      }
    });
  }

  private class TestHttpHandler extends HttpHandler {

    public boolean headersAdded = false;

    public TestHttpHandler(String apiKey, String endpoint, RestAdapter.LogLevel logLevel) {
      super(apiKey, endpoint, logLevel);
    }

    @Override
    protected void addHeadersForRequest(RequestInterceptor.RequestFacade requestFacade) {
      headersAdded = true;
    }
  }
}
