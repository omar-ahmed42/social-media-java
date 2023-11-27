package com.omarahmed42.socialmedia.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public interface AsyncService {
    <T> Future<T> get(Supplier<Future<T>> supplier);
    <T> CompletableFuture<T> getCompletable(Supplier<Future<T>> supplier);

}
