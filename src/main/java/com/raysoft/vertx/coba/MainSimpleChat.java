package com.raysoft.vertx.coba;

import io.vertx.core.Vertx;

/**
 * Created by saviourcat on 6/20/16.
 */
public class MainSimpleChat {
    public static void main(String[] args) {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4jLogDelegateFactory");

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new SocketServerVerticle());
        vertx.deployVerticle(new MessagingVerticle());
    }
}
