/*
 * Copyright 2018 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openshift.booster;


import io.vertx.core.Future;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.CorsHandler;

public class NameServiceVerticle extends AbstractVerticle {

    private static final String DEFAULT_NAME = "World";

    @Override
    public void start(Future<Void> future) {
        Router router = Router.router(vertx);
        router.route().handler(CorsHandler.create("*"));
        router.get("/api/name")
            .handler(this::getName);
        router.get("/health").handler(rc -> rc.response().end("ok"));

        vertx.createHttpServer()
            .requestHandler(router::accept)
            .rxListen(config().getInteger("http.port", 8080))
            .toCompletable()
            .subscribe(CompletableHelper.toObserver(future));
    }

    private void getName(RoutingContext rc) {
        String from = rc.request().getParam("from");
        String delay = rc.request().getParam("delay");

        final String fromSuffix = from != null ? " from " + from : "";
        final String name = DEFAULT_NAME + fromSuffix;

        if (delay != null && !delay.trim().isEmpty()) {
            int processingDelay;
            try {
                processingDelay = Integer.parseInt(delay);
            } catch (NumberFormatException e) {
                processingDelay = 150;
            }

            final long round = Math.round((Math.random() * 200) + processingDelay);
            vertx.setTimer(round, l -> rc.response().end(name));
        } else {
            rc.response().end(name);
        }
    }
}
