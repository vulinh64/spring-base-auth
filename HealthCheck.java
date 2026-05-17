import module java.net.http;

import java.net.http.HttpResponse.BodyHandlers;

static final int MAX_ATTEMPTS = 10;
static final long RETRY_DELAY_MS = 1000L;
static final Duration TIMEOUT = Duration.ofSeconds(2);

static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

static final HttpRequest HEALTH_CHECK_REQUEST =
    HttpRequest.newBuilder(URI.create("http://localhost:8080/health"))
        .timeout(TIMEOUT)
        .GET()
        .build();

void main() throws InterruptedException {
  for (var attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
    executeHealthCheck(HTTP_CLIENT, HEALTH_CHECK_REQUEST, attempt);
  }
}

void executeHealthCheck(HttpClient client, HttpRequest request, int attempt)
    throws InterruptedException {
  try {
    if (attempt > MAX_ATTEMPTS) {
      IO.println(
          "Maximum allowed number of attempts exceeded %s times, stopping..."
              .formatted(MAX_ATTEMPTS));
      client.close();
      System.exit(1);
    }

    var response = client.send(request, BodyHandlers.ofString());

    if (response.statusCode() == 200) {
      IO.println("The service is up!");
      client.close();
      System.exit(0);
    }
  } catch (IOException _) {
    // Delay 1 second before next health check attempt
    Thread.sleep(RETRY_DELAY_MS);
  }
}
