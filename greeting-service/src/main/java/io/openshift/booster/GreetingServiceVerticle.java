package io.openshift.booster;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.StaticHandler;

/**
 * Greeting service using the Name Service
 */
public class GreetingServiceVerticle extends AbstractVerticle {

  private WebClient client;
  private boolean openCircuit = false;

  @Override
  public void start(Future<Void> future) {
    client = WebClient.create(vertx,
      new WebClientOptions()
        .setDefaultPort(8080)
        .setDefaultHost("vertx-istio-circuit-breaker-name")
        .setConnectTimeout(1000)
        .setIdleTimeout(1000)
    );

    Router router = Router.router(vertx);
    router.get("/api/greeting").handler(this::invoke);
    router.get("/api/ping").handler(rc -> rc.response().end(new JsonObject().put("content", "Hello OK").encode()));
    router.get("/api/cb-state").handler(rc -> rc.response().end(openCircuit ? "Open" : "Close"));
    router.get("/health").handler(rc -> rc.response().end("ok"));
    router.get("/*").handler(StaticHandler.create());

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .rxListen(config().getInteger("http.port", 8080))
      .toCompletable()
      .subscribe(CompletableHelper.toObserver(future));
  }

  private void invoke(RoutingContext rc) {
    String uri = "/api/name?from=" + rc.request().getParam("from");
    if (rc.request().getParam("delay") != null) {
      uri += "&delay=" + rc.request().getParam("delay");
    }
    client.get(uri)
      .rxSend()
      .map(resp -> {
        if (resp.statusCode() == 503) {
          this.openCircuit = true;
          return "Fallback";
        } else {
          this.openCircuit = false;
          return resp.bodyAsString();
        }
      })
      .map(name -> String.format("Hello, %s!", name))
      .map(msg -> new JsonObject().put("content", msg))
      .subscribe(
        payload -> rc.response().end(payload.encodePrettily()),
        rc::fail
      );
  }
}
