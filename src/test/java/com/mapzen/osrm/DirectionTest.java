package com.mapzen.osrm;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import static org.fest.assertions.api.Assertions.assertThat;

public class DirectionTest {
    @Captor
    @SuppressWarnings("unused")
    ArgumentCaptor<Callback> callback;

    @Captor
    @SuppressWarnings("unused")
    ArgumentCaptor<Route> route;

    @Captor
    @SuppressWarnings("unused")
    ArgumentCaptor<Integer> statusCode;

    Direction.Router validRouter;

    MockWebServer server;

    @Before
    public void setup() throws Exception {
        server = new MockWebServer();
        MockitoAnnotations.initMocks(this);
        double[] loc = new double[] {1.0, 2.0};
        validRouter = Direction.getRouter().setLocation(loc).setLocation(loc);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void shouldHaveDefaultEndpoint() throws Exception {
        URL url = validRouter.getRouteUrl();
        assertThat(url.toString()).startsWith("http://osrm.test.mapzen.com");
    }

    @Test
    public void shouldSetEndpoint() throws Exception {
        URL url = validRouter.setEndpoint("http://testing.com").getRouteUrl();
        assertThat(url.toString()).startsWith("http://testing.com");
    }

    @Test
    public void shouldHaveDefaultZoom() throws Exception {
        URL url = validRouter.getRouteUrl();
        assertThat(url.toString()).contains("z=17");
    }

    @Test
    public void shouldSetZoom() throws Exception {
        URL url = validRouter.setZoomLevel(11).getRouteUrl();
        assertThat(url.toString()).contains("z=11");
    }

    @Test
    public void shouldDefaultToCar() throws Exception {
        URL url = validRouter.getRouteUrl();
        assertThat(url.toString()).contains("car/viaroute");
    }

    @Test
    public void shouldSetToCar() throws Exception {
        URL url = validRouter.setDriving().getRouteUrl();
        assertThat(url.toString()).contains("car/viaroute");
    }

    @Test
    public void shouldSetToBike() throws Exception {
        URL url = validRouter.setBiking().getRouteUrl();
        assertThat(url.toString()).contains("bike/viaroute");
    }

    @Test
    public void shouldSetToFoot() throws Exception {
        URL url = validRouter.setWalking().getRouteUrl();
        assertThat(url.toString()).contains("foot/viaroute");
    }

    @Test(expected=MalformedURLException.class)
    public void shouldThrowErrorWhenNoLocation() throws Exception {
        Direction.getRouter().getRouteUrl();
    }

    @Test(expected=MalformedURLException.class)
    public void shouldThrowErrorWhenOnlyOneLocation() throws Exception {
        Direction.getRouter().setLocation(new double[] {1.0, 1.0}).getRouteUrl();
    }

    @Test
    public void shouldAddLocations() throws Exception {
        double[] loc1 = { 1.0, 2.0 };
        double[] loc2 = { 3.0, 4.0 };
        double[] loc3 = { 5.0, 6.0 };
        URL url = Direction.getRouter()
                .setLocation(loc1)
                .setLocation(loc2)
                .setLocation(loc3).getRouteUrl();
        assertThat(url.toString()).contains("&loc=1.0,2.0&loc=3.0,4.0&loc=5.0,6.0");
    }

    @Test
    public void shouldGetRoute() throws Exception {
        startServerAndEnqueue(new MockResponse().setBody(getFixture("brooklyn")));
        String endpoint = server.getUrl("").toString();
        Callback callback = Mockito.mock(Callback.class);
        Direction.Router router = Direction.getRouter()
                .setEndpoint(endpoint)
                .setLocation(new double[] { 40.659241, -73.983776 })
                .setLocation(new double[] { 40.671773, -73.981115 });
        router.setCallback(callback);
        router.fetch();
        router.runner.join();
        Mockito.verify(callback).success(route.capture());
        assertThat(route.getValue().foundRoute()).isTrue();
    }

    @Test
    public void shouldGetError() throws Exception {
        startServerAndEnqueue(new MockResponse().setResponseCode(500));
        Callback callback = Mockito.mock(Callback.class);
        String endpoint = server.getUrl("").toString();
        Direction.Router router = Direction.getRouter()
                .setEndpoint(endpoint)
                .setLocation(new double[] { 40.659241, -73.983776 })
                .setLocation(new double[] { 40.671773, -73.981115 });
        router.setCallback(callback);
        router.fetch();
        router.runner.join();
        Mockito.verify(callback).failure(statusCode.capture());
        assertThat(statusCode.getValue()).isEqualTo(500);
    }

    @Test
    public void shouldGetNotFound() throws Exception {
        startServerAndEnqueue(new MockResponse().setResponseCode(404));
        Callback callback = Mockito.mock(Callback.class);
        String endpoint = server.getUrl("").toString();
        Direction.Router router = Direction.getRouter()
                .setEndpoint(endpoint)
                .setLocation(new double[] { 40.659241, -73.983776 })
                .setLocation(new double[] { 40.671773, -73.981115 });
        router.setCallback(callback);
        router.fetch();
        router.runner.join();
        Mockito.verify(callback).failure(statusCode.capture());
        assertThat(statusCode.getValue()).isEqualTo(404);
    }

    private void startServerAndEnqueue(MockResponse response) throws Exception {
        server.enqueue(response);
        server.play();
    }


    public static String getFixture(String name) {
        String basedir = System.getProperty("user.dir");
        File file = new File(basedir + "/src/test/fixtures/" + name + ".route");
        String fixture = "";
        try {
            fixture = Files.toString(file, Charsets.UTF_8);
        } catch (Exception e) {
            fixture = "not found";
        }
        return fixture;
    }
}
