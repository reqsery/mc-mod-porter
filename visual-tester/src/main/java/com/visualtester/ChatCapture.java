package com.visualtester;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Thread-safe store for all chat messages received during a test session. */
public class ChatCapture {
    private static final List<String> messages = new CopyOnWriteArrayList<>();
    private static final int MAX_MESSAGES = 1000;

    public static void addMessage(String message) {
        messages.add(message);
        if (messages.size() > MAX_MESSAGES) messages.remove(0);
    }

    public static List<String> getMessages() {
        return new ArrayList<>(messages);
    }

    public static void clear() { messages.clear(); }
}
