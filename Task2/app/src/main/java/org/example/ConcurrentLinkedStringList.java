package org.example;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

class ConcurrentLinkedStringList implements Iterable<String> {
    static class Node {
        final String value;
        final AtomicReference<Node> next = new AtomicReference<>(null);
        final ReentrantLock lock = new ReentrantLock();
        Node(String v) { this.value = v; }
    }

    private final Node head = new Node(null);

    public void addFirst(String s) {
        Node n = new Node(s);
        head.lock.lock();
        Node f = head.next.get();
        if (f != null) f.lock.lock();
        n.next.set(f);
        head.next.set(n);
        if (f != null) f.lock.unlock();
        head.lock.unlock();
    }

    public void bubblePass(long insideMs, long betweenMs, AtomicLong steps) {
        Node prev = head;
        Node a = prev.next.get();
        while (a != null) {
            Node b = a.next.get();
            if (b == null) break;
            prev.lock.lock();
            a.lock.lock();
            b.lock.lock();
            App.sleep(insideMs);
            steps.incrementAndGet();
            if (a.value.compareTo(b.value) > 0) {
                Node bn = b.next.get();
                prev.next.set(b);
                b.next.set(a);
                a.next.set(bn);
            }
            b.lock.unlock();
            a.lock.unlock();
            prev.lock.unlock();
            App.sleep(betweenMs);
            prev = prev.next.get();
            a = prev.next.get();
        }
    }

    public Iterator<String> iterator() {
        List<String> snap = new ArrayList<>();
        Node cur = head.next.get();
        while (cur != null) {
            snap.add(cur.value);
            cur = cur.next.get();
        }
        return snap.iterator();
    }
}