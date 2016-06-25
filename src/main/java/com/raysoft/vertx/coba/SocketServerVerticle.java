package com.raysoft.vertx.coba;

/**
 * Created by saviourcat on 6/20/16.
 */

import com.raysoft.vertx.coba.util.CommonUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.net.NetServer;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import org.apache.log4j.Logger;

public class SocketServerVerticle extends AbstractVerticle {
    private Logger log = Logger.getLogger(SocketServerVerticle.class);

    @Override
    public void start(Future<Void> fut) {
        EventBus eb = vertx.eventBus();
        SharedData sd = vertx.sharedData();

        NetServer server = vertx.createNetServer();
        server.connectHandler(socket -> {
            // Handle the connection in here
            log.info("Incoming connection:" + socket.localAddress().port() + " -> " + socket.remoteAddress() + ":" + socket.remoteAddress().port());
            Buffer welcomeBuffer = Buffer.buffer();
            welcomeBuffer.appendString("Welcome to Chat\n");
            socket.write(welcomeBuffer);
            socket.handler(RecordParser.newDelimited("\n", buffer -> {

                String rawString = buffer.getString(0, buffer.length());
                log.debug("incoming string=" + rawString);
                if (rawString == null || rawString.trim().equals("")) {
                    return;
                }

                LocalMap<String, String> mapReverse = sd.getLocalMap("client.handler.reverse");
                LocalMap<String, String> mapClientHandler = sd.getLocalMap("client.handler");


                String[] params = CommonUtil.getParams(rawString);
                if (params[0].equals("/connect")) {
                    log.info("client attempt connect=" + params[1]);

                    String uid = params[1];
                    Buffer bufResp = Buffer.buffer();
                    if (uid.trim().indexOf(" ") != -1) {
                        bufResp.appendString("\\UID cannot contain space\n");
                        socket.write(bufResp);
                    } else if (uid.trim().indexOf("#") != -1) {
                        bufResp.appendString("\\UID cannot contain #\n");
                        socket.write(bufResp);
                    } else {

                        // map handler ID to clientid
                        mapClientHandler.put(uid, socket.writeHandlerID());
                        mapReverse.put(socket.writeHandlerID(), uid);

                        // event to newsignup bus
                        eb.send("newsignup", uid, ar -> {
                            if (ar.succeeded()) {
                                bufResp.appendString("\\connect##success##" + uid + "\n");
                                socket.write(bufResp);
                            } else {
                                bufResp.appendString("\\connect##failed##" + uid + "\n");
                                socket.write(bufResp);
                            }
                        });

                    }


                } else if (params[0].equals("/send")) {
                    String sender = mapReverse.get(socket.writeHandlerID());
                    DeliveryOptions options = new DeliveryOptions();
                    options.addHeader("sender", sender);
                    String inboxAddr = "inbox." + params[1];
                    eb.send(inboxAddr, params[2], options, ar -> {
                        if (ar.succeeded()) {
                            socket.write("\\send##" + params[1] + "##success##" + ar.result().body() + "\n");
                        } else {
                            socket.write("\\send##" + params[1] + "##failed\n");
                        }
                    });

                    log.debug("sending to eventbus, from " + sender + ", to " +  inboxAddr + " -> " + params[2]);
                } else if (params[0].equals("/ping")) {
                    String sender = mapReverse.get(socket.writeHandlerID());
                    socket.write("\\ping##" + sender + "\n");
                } else if (params[0].equals("/quit")) {
                    String sender = mapReverse.get(socket.writeHandlerID());
                    socket.write("\\ping##" + sender + "\n");
                    socket.close();
                }
            }));

            socket.closeHandler(v -> {
                log.info("The socket has been closed");
                LocalMap<String, String> mapReverse = sd.getLocalMap("client.handler.reverse");
                String uid = mapReverse.get(socket.writeHandlerID());
                mapReverse.remove(socket.writeHandlerID());

                LocalMap<String, String> mapClientHandler = sd.getLocalMap("client.handler");
                if (uid != null) {
                    mapClientHandler.remove(uid);
                }

                log.info("unregistering from bus=" + uid + ", sockethandler=" + socket.writeHandlerID());
            });
        });

        server.listen(config().getInteger("socket.port", 1234), "localhost", res -> {
            if (res.succeeded()) {
                log.info("Server is now listening!");
                fut.complete();
            } else {
                log.info("Failed to bind!");
                fut.failed();
            }
        });
    }
}