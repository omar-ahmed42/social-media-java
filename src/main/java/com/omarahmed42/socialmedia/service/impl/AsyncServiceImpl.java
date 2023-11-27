package com.omarahmed42.socialmedia.service.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.omarahmed42.socialmedia.service.AsyncService;

@Service
public class AsyncServiceImpl implements AsyncService {

    @Async
    public <T> Future<T> get(Supplier<Future<T>> supplier) {
        return supplier.get();
    }

    @Async
    public <T> CompletableFuture<T> getCompletable(Supplier<Future<T>> supplier) {
        return (CompletableFuture<T>) supplier.get();
    }
}
