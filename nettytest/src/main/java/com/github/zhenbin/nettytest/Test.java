package com.github.zhenbin.nettytest;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Test {
    public static void main(String[] args) {
        Lock lock = new ReentrantLock();
        lock.lock();
        System.out.println("Locked");
        lock.unlock();
        lock.unlock();
        System.out.println("unlocked");
    }
}
