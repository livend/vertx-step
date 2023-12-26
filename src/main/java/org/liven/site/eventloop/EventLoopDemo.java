package org.liven.site.eventloop;

import javax.swing.undo.CannotUndoException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

public class EventLoopDemo {

    public static void main(String[] args) {
        EventLoop eventLoop = new EventLoop();

        new Thread(() -> {
            for (int i = 0; i < 500; i++) {
                delay(1);
                eventLoop.dispatch(new EventLoop.Event("tick", i));
            }
        }).start();

        new Thread(() -> {
            delay(2500);
            eventLoop.dispatch(new EventLoop.Event("hello", "beautiful world"));
            delay(800);
            eventLoop.dispatch(new EventLoop.Event("hello", "beautiful universe"));
            delay(5000);
            eventLoop.dispatch(new EventLoop.Event("stop", "stop"));
        }).start();

        eventLoop.dispatch(new EventLoop.Event("hello", "world!"));
        eventLoop.dispatch(new EventLoop.Event("foo", "bar"));

        eventLoop.on("hello", s -> System.out.println("hello " + s)).on("tick", i -> System.out.println("tick#" + i)).on("stop", (v) -> {
            System.out.println("rec stop msg " + v);
            eventLoop.stop();
        }).run();
    }


    private static void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}


final class EventLoop {

    private final ConcurrentLinkedDeque<Event> events = new ConcurrentLinkedDeque<>();

    private final ConcurrentHashMap<String, Consumer<Object>> handlers = new ConcurrentHashMap<>();

    // 事件绑定
    public EventLoop on(String key, Consumer<Object> handler) {
        handlers.put(key, handler);
        return this;
    }

    // 事件分发
    public void dispatch(Event event) {
        events.add(event);
    }

    public void stop() {
        Thread.currentThread().interrupt();
    }

    public void run() {
        while (!(events.isEmpty() && Thread.interrupted())) {
            if (!events.isEmpty()) {
                Event event = events.pop();
                if (handlers.containsKey(event.key)) {
                    handlers.get(event.key).accept(event.data);
                } else {
                    System.out.println("No handler for key " + event.key);
                }
            }
        }
        System.out.println("Bye");
    }


    public static final class Event {
        private final String key;
        private final Object data;

        public Event(String key, Object data) {
            this.key = key;
            this.data = data;
        }
    }
}



