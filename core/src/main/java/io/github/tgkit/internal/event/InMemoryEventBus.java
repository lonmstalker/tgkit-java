/*
 * Copyright 2025 TgKit Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.tgkit.internal.event;

import io.github.tgkit.internal.config.BotGlobalConfig;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InMemoryEventBus implements BotEventBus {
  private static final Logger log = LoggerFactory.getLogger(InMemoryEventBus.class);

  static {
    BotGlobalConfig.INSTANCE.events().bus(new InMemoryEventBus());
  }

  private final Thread workingThread;
  /* — Single-writer, поэтому одна thread-safe очередь достаточно — */
  private final BlockingQueue<BotEvent> ring;
  /* — Мапа подписчиков по классу события — */
  private final ConcurrentHashMap<Class<?>, CopyOnWriteArraySet<Sub>> subs =
      new ConcurrentHashMap<>();
  /* — Пул consumer-ов (virtual threads by default) — */
  private final ExecutorService pool;
  /* — control flags — */
  private volatile boolean alive = true;

  public InMemoryEventBus(@NonNull ExecutorService pool, int queueSize) {
    this.pool = pool;
    this.ring = new LinkedBlockingQueue<>(queueSize);
    this.workingThread = startLoop();
  }

  public InMemoryEventBus() {
    this(BotGlobalConfig.INSTANCE.executors().getIoExecutorService(), 1000);
  }

  /* === public API ============================================ */

  @Override
  public <E extends BotEvent> void publish(@NonNull E event) {
    ensureAlive();
    if (!ring.offer(event)) {
      throw new RejectedExecutionException("Back-pressure: queue full");
    }
  }

  @Override
  public <E extends BotEvent> @NonNull CompletableFuture<Void> publishAsync(@NonNull E e) {
    return CompletableFuture.runAsync(() -> publish(e), pool);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E extends BotEvent> @NonNull BotEventSubscription subscribe(
      @NonNull Class<E> type, @NonNull Consumer<E> handler) {
    ensureAlive();
    Sub s = new Sub(type, (Consumer<BotEvent>) handler);
    subs.computeIfAbsent(type, __ -> new CopyOnWriteArraySet<>()).add(s);
    return s;
  }

  @Override
  public void unsubscribe(@NonNull BotEventSubscription s) {
    if (!(s instanceof Sub sub)) {
      return;
    }
    Set<Sub> set = subs.get(sub.type);
    if (set != null) {
      set.remove(sub);
    }
  }

  @Override
  public int backlog() {
    return ring.size();
  }

  @Override
  public void shutdown() throws InterruptedException {
    alive = false;
    pool.shutdownNow();
    pool.awaitTermination(5, TimeUnit.SECONDS);
    workingThread.interrupt();
  }

  /* Dedicated virtual-thread читает очередь и раскидывает события. */
  private @NonNull Thread startLoop() {
    return Thread.startVirtualThread(
        () -> {
          while (alive || !ring.isEmpty()) {
            try {
              BotEvent ev = ring.poll(200, TimeUnit.MILLISECONDS);
              if (ev == null) {
                continue;
              }
              dispatch(ev);
            } catch (InterruptedException ignored) {
              Thread.currentThread().interrupt();
            }
          }
        });
  }

  private void dispatch(@NonNull BotEvent ev) {
    Class<?> c = ev.getClass();
    Set<Sub> direct = subs.get(c);

    if (direct != null) {
      direct.forEach(s -> invoke(s, ev));
    }

    // подписчики на суперклассы/интерфейсы (напр., Event.class)
    subs.forEach(
        (k, v) -> {
          if (k.isAssignableFrom(c) && k != c) {
            v.forEach(s -> invoke(s, ev));
          }
        });
  }

  private void invoke(@NonNull Sub s, @NonNull BotEvent ev) {
    try {
      s.handler.accept(ev);
    } catch (Throwable t) {
      log.error("[EventBus] consumer error: ", t);
    }
  }

  private void ensureAlive() {
    if (!alive) {
      throw new IllegalStateException("EventBus already shut down");
    }
  }

  private record Sub(Class<?> type, Consumer<BotEvent> handler) implements BotEventSubscription {}
}
