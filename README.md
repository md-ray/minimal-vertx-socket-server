# minimal-vertx-socket-server
Simple &amp; minimalistic chat server using vert.x

How to run:
- Run MainSimpleChat class
- Default port will be used: 10080

How to use chat:
- Telnet to port
- To connect to server, write <b>/connect##&lt;userid&gt; + enter</b>
- To send message to other userid, write <b>/send##&lt;dest_user_id&gt;##&lt;message_to_be_sent&gt; + enter</b>

How to run junit tests:
- Run MainSimpleChatTest
- Will open random port, establish server, try to connect and verify connection result
