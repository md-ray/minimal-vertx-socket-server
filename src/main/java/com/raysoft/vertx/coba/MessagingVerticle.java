package com.raysoft.vertx.coba;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import org.apache.log4j.Logger;

/**
 * Created by saviourcat on 6/20/16.
 */
public class MessagingVerticle extends AbstractVerticle {
    private Logger log = Logger.getLogger(MessagingVerticle.class);
    private EventBus eb;
    private LocalMap<String, String> socketHandlersMap;

    @Override
    public void start(Future<Void> fut) {

        eb = vertx.eventBus();
        SharedData sd = vertx.sharedData();
        socketHandlersMap = sd.getLocalMap("client.handler");

        // Message consumer for new connected users
        eb.consumer("newsignup", message -> {
            String uid = message.body().toString();
            String inboxAddr = "inbox." + uid;
            eb.consumer(inboxAddr, message2 -> {
                handleInbox(uid, message2.headers().get("sender"), message2.body().toString(), message2);
            });
            message.reply("OK");
        });
    }

    private void handleInbox(String uid, String sender, String message, Message<Object> msgObject) {
        String socketHandlerId = socketHandlersMap.get(uid);
        if (socketHandlerId == null) {
            log.info("Discarding (harusnya tulis ke redis) inbox message=" + message);
            msgObject.reply("pending");
        } else {
            log.info("Sending message to " + sender + " -> " + message);
            Buffer buffer = Buffer.buffer();
            buffer.appendString("\\inbox##"+sender+"##" + message + "\n");
            eb.send(socketHandlerId, buffer);
            msgObject.reply("immediate");
        }
    }
}
