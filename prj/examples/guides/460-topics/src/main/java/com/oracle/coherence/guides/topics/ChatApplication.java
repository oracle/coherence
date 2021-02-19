/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.topics;

import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.Element;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tangosol.net.ValueTypeAssertion.withType;

/**
 * A driver for the topics tutorial.
 *
 * @author Tim Middleton 2021.02.16
 */
public class ChatApplication implements Runnable {

    /**
     * Indicates that the application is ready.
     */
    private boolean ready = false;

    /**
     * User Id for the chat.
     */
    private String userId;

    // tag::properties[]
    /**
     * Topic for public messages.
     */
    private NamedTopic<ChatMessage> topicPublic;

    /**
     * Topic for private messages.
     */
    private NamedTopic<ChatMessage> topicPrivate;

    /**
     * Publisher for public messages.
     */
    private Publisher<ChatMessage> publisherPublic;

    /**
     * Publisher for private messages.
     */
    private Publisher<ChatMessage> publisherPrivate;

    /**
     * Subscriber for public messages.
     */
    private Subscriber<ChatMessage> subscriberPublic;

    /**
     * Subscriber for private messages.
     */
    private Subscriber<ChatMessage> subscriberPrivate;
    // end::properties[]

    /**
     * Date formatter.
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

    /**
     * {@link InputStream} to be used for {@link Scanner} input.
     */
    private InputStream inputStream;

    /**
     * Number of messages sent.
     */
    private AtomicInteger messagesSent = new AtomicInteger(0);

    /**
     * Number of messages received.
     */
    private AtomicInteger messagesReceived = new AtomicInteger(0);

    /**
     * Usage string.
     */
    private static final String USAGE = "Commands:\nquit - Quit the chat\n"
            + "help - Display help\nsend - Send public "
            + "message\nsendpm userId - Send "
            + "private message\n";

    /**
     * Main entry point.
     *
     * @param args array of arguments arg[0] must be the userId
     */
    public static void main(String[] args) {
        // tag::systemProperties[]
        System.setProperty("coherence.cacheconfig", "topics-cache-config.xml");
        System.setProperty("coherence.distributed.localstorage", "false");
        System.setProperty("coherence.log.level", "2");
        // end::systemProperties[]
        if (args.length != 1) {
            throw new IllegalArgumentException("User id must be supplied");
        }
        ChatApplication chat = new ChatApplication(args[0]);
        chat.run();
    }

    /**
     * Constructs a {@link ChatApplication} using the default {@link InputStream}.
     *
     * @param userId user id for the chat
     */
    public ChatApplication(String userId) {
        this(userId, System.in);
    }

    /**
     * Constructs a {@link ChatApplication}.
     *
     * @param userId      user id for the chat
     * @param inputStream the {@link InputStream} to use for input
     */
    public ChatApplication(String userId, InputStream inputStream) {
        this.userId = userId;
        this.inputStream = inputStream;

        // tag::sessionCreate[]
        Session session = Session.create();
        // end::sessionCreate[]

        // tag::public[]
        // create public topic where everyone subscribes as anonymous and gets all messages
        // sent while they are connected
        topicPublic = session.getTopic("public-messages", withType(ChatMessage.class));  // <1>
        publisherPublic = topicPublic.createPublisher();  // <2>
        subscriberPublic = topicPublic.createSubscriber();  // <3>
        // end::public[]

        // tag::private[]
        // create private topic where messages are send to individuals and are via a subscriber group.
        // Subscribers will get messages sent offline if they have previously connected
        topicPrivate = session.getTopic("private-messages", withType(ChatMessage.class));  // <1>
        publisherPrivate = topicPrivate.createPublisher();  // <2>
        subscriberPrivate = topicPrivate.createSubscriber(Subscriber.Name.of(userId));  // <3>
        // end::private[]
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(inputStream);
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));

        // tag::subscribers[]
        // subscription for anonymous subscriber/ public messages
        subscriberPublic.receive().handleAsync((v, err) -> receive(v, err, subscriberPublic));

        // subscription for subscriber group / private durable messages
        subscriberPrivate.receive().handleAsync((v, err) -> receive(v, err, subscriberPrivate));
        // end::subscribers[]

        System.out.println("User: " + userId);

        // tag::join[]
        // generate a join message and send synchronously
        publisherPublic.send(new ChatMessage(userId, null, ChatMessage.Type.JOIN, null)).join();
        // end::join[]

        try {
            log(USAGE);
            ready = true;
            while (true) {
                System.out.print("Chat (" + userId + ")> ");
                System.out.flush();
                String line = scanner.nextLine();

                if ("quit".equals(line)) {
                    break;
                    // tag::sendPublic[]
                } else if (line.startsWith("send ")) {
                    // send public message synchronously
                    publisherPublic.send(new ChatMessage(userId, null, ChatMessage.Type.MESSAGE, line.substring(5)))
                            .handle(this::handleSend);  // <1>
                    // end::sendPublic[]
                    // tag::sendPrivate[]
                } else if (line.startsWith("sendpm ")) {
                    // send private durable message
                    String[] parts = line.split(" ");
                    // extract the target user and message
                    if (parts.length < 3) {
                        log("Usage: sendpm user message");
                    } else {
                        String user = parts[1];
                        String message = line.replaceAll(parts[0] + " " + parts[1] + " ", "");
                        publisherPrivate.send(new ChatMessage(userId, user, ChatMessage.Type.MESSAGE, message))
                                .handle(this::handleSend); // <1>
                        // end::sendPrivate[]
                    }
                } else {
                    log(USAGE);
                }
            }
        } finally {
            cleanup();
        }
    }

    /**
     * Cleanup resources before exiting.
     */
    // tag::cleanup[]
    private void cleanup() {
        // generate a leave message
        // tag::leave[]
        if (topicPublic.isActive()) {
            publisherPublic.send(new ChatMessage(userId, null, ChatMessage.Type.LEAVE, null)).join();
        }
        // end::leave[]

        publisherPublic.flush().join();
        publisherPublic.close();
        subscriberPublic.close();
        topicPublic.close();

        publisherPrivate.flush().join();
        publisherPrivate.close();
        subscriberPrivate.close();
        topicPrivate.close();
    }
    // end::cleanup[]

    /**
     * Generate a message to display. If the message returned is null then it should not be displayed.
     *
     * @param chatMessage the {@link ChatMessage} to process
     * @return a message to display or null if none
     */
    private String getMessageLog(ChatMessage chatMessage) {
        String fromUserId = chatMessage.getFromUserId();
        String toUserId = chatMessage.getToUserId();

        // ignore message if it is a message from this user or it is a
        // private message not for this user.
        if (fromUserId.equals(userId)
                || (toUserId != null && !toUserId.equals(userId))) {
            return null;
        }

        ChatMessage.Type type = chatMessage.getType();

        StringBuilder sb = new StringBuilder(DATE_FORMAT.format(new Date(chatMessage.getDate())))
                .append(" ")
                .append(fromUserId);

        if (type.equals(ChatMessage.Type.JOIN)) {
            sb.append(" joined the chat");
        } else if (type.equals(ChatMessage.Type.LEAVE)) {
            sb.append(" left the chat");
        } else {
            if (toUserId != null) {
                sb.append(" (Private)");
            }
            sb.append(" - ").append(chatMessage.getMessage());
        }
        return sb.toString();
    }

    // tag::receive[]
    /**
     * Receive a message from a given {@link Subscriber} and once processed, re-subscribe.
     * @param element    {@link Element} received
     * @param throwable  {@link Throwable} if any errors
     * @param subscriber {@link Subscriber} to re-subscribe to
     * @return void
     */
    public Void receive(Element<ChatMessage> element, Throwable throwable, Subscriber<ChatMessage> subscriber) {
        if (throwable != null) {
            if (throwable instanceof CancellationException) {
                // exiting process, ignore.
            } else {
                log(throwable.getMessage());
            }
        } else {
            ChatMessage chatMessage = element.getValue();  // <1>
            String message = getMessageLog(chatMessage);  // <2>
            // ensure we don't display a message from ourselves
            if (message != null) {
                messagesReceived.incrementAndGet();
                log(message);
            }
            subscriber.receive().handleAsync((v, err) -> receive(v, err, subscriber));  // <1>
        }
        return null;
    }
    // end::receive[]

    /**
     * Handle a send error
     * @param v   Void
     * @param err the {@link Throwable}
     * @return Void
     */
    private Void handleSend(Void v, Throwable err) {
        if (err == null) {
            messagesSent.incrementAndGet();
        } else {
            log("Error sending message + err.printStackTrace()");
        }
        return null;
    }

    /**
     * Log a message and synchronize so we can get overlapping output.
     *
     * @param message the message to log
     */
    private synchronized void log(String message) {
        System.out.println(message);
        System.out.flush();
    }

    /**
     * Returns the messages sent.
     *
     * @return the messages sent
     */
    public int getMessagesSent() {
        return messagesSent.get();
    }

    /**
     * Returns the messages received.
     *
     * @return the messages received
     */
    public int getMessagesReceived() {
        return messagesReceived.get();
    }

    /**
     * Indicates if the application is ready.
     * @return if the application is ready
     */
    public boolean isReady() {
        return ready;
    }
}
