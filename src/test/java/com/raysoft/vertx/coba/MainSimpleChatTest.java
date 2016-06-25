package com.raysoft.vertx.coba;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Created by saviourcat on 6/22/16.
 */
@RunWith(VertxUnitRunner.class)
public class MainSimpleChatTest {
    private Vertx vertx;
    private int port;
    private String strExpected = "";

    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject().put("socket.port", port)
                );
        vertx.deployVerticle(MessagingVerticle.class.getName());
        vertx.deployVerticle(SocketServerVerticle.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testMyApplication(TestContext context) {
        final Async async = context.async();

        NetClientOptions options = new NetClientOptions().setConnectTimeout(5000);
        NetClient client = vertx.createNetClient(options);
        System.out.println("coba connect");
        client.connect(port, "localhost", res -> {
            context.assertTrue(res.succeeded());
            if (res.succeeded()) {
                System.out.println("Connected!");

                NetSocket socket = res.result();
                writeAndExpect(socket, null, "Welcome");
                socket.handler(RecordParser.newDelimited("\n", buffer -> {
                    String rawString = buffer.getString(0, buffer.length()).trim();
                    System.out.println("raw resp=" + rawString);
                    context.assertTrue(rawString.startsWith(strExpected));
                }));

                vertx.setTimer(1000, id -> {
                    writeAndExpect(socket, "/connect##user001\n", "\\connect");
                    vertx.setTimer(1000, id2 -> {
                        async.complete();
                    });
                });

                System.out.println("after tulis socket");
            } else {
                System.out.println("Failed to connect: " + res.cause().getMessage());
            }
        });



    }

    private void writeAndExpect(NetSocket socket, String strCommand, String strExpected) {
        this.strExpected = strExpected;
        if (strCommand != null && !strCommand.trim().equals("")) {
            socket.write(strCommand);
        }

    }


}
