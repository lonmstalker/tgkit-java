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
package io.github.tgkit.security;

import io.github.tgkit.security.antispam.AntiSpamInterceptor;
import io.github.tgkit.security.antispam.DuplicateProvider;
import io.github.tgkit.security.antispam.InMemoryDuplicateProvider;
import io.github.tgkit.security.captcha.CaptchaProvider;
import io.github.tgkit.security.captcha.InMemoryMathCaptchaProviderStore;
import io.github.tgkit.security.captcha.MathCaptchaOperations;
import io.github.tgkit.security.captcha.RedisMathCaptchaProviderStore;
import io.github.tgkit.security.captcha.provider.MathCaptchaProvider;
import io.github.tgkit.security.ratelimit.RateLimiter;
import io.github.tgkit.security.ratelimit.impl.InMemoryRateLimiter;
import io.github.tgkit.security.ratelimit.impl.RedisRateLimiter;
import java.time.Duration;
import java.util.Set;
import org.apache.commons.lang3.Range;
import org.checkerframework.checker.nullness.qual.NonNull;
import redis.clients.jedis.JedisPool;

/**
 * Утилита для создания типовых компонентов безопасности: проверка на дубликаты, CAPTCHA и
 * ограничители скорости.
 *
 * <p>Пример использования:
 *
 * <pre>{@code
 * RateLimiter limiter = BotSecurity.inMemoryRateLimiter();
 * AntiSpamInterceptor interceptor = BotSecurity.antiSpamInterceptor(Set.of("example.com"));
 * }</pre>
 */
public final class BotSecurity {

  private BotSecurity() {}

  /** Создаёт хранитель дубликатов сообщений в памяти. */
  public static @NonNull DuplicateProvider inMemoryDuplicateProvider(
      @NonNull Duration ttl, long maxSize) {
    return InMemoryDuplicateProvider.builder().ttl(ttl).maxSize(maxSize).build();
  }

  /** Математическая CAPTCHA с хранением в памяти. */
  public static @NonNull CaptchaProvider inMemoryCaptchaProvider(
      @NonNull Duration ttl, long maxSize) {
    return MathCaptchaProvider.builder()
        .wrongCount(1)
        .ttl(ttl)
        .store(new InMemoryMathCaptchaProviderStore(ttl, maxSize))
        .numberRange(Range.of(5, 25))
        .allowedOps(MathCaptchaOperations.OPERATIONS)
        .build();
  }

  /** Математическая CAPTCHA с хранением в Redis. */
  public static @NonNull CaptchaProvider redisCaptchaProvider(
      @NonNull Duration ttl, @NonNull JedisPool pool) {
    return MathCaptchaProvider.builder()
        .wrongCount(1)
        .ttl(ttl)
        .numberRange(Range.of(5, 25))
        .allowedOps(MathCaptchaOperations.OPERATIONS)
        .store(new RedisMathCaptchaProviderStore(pool))
        .build();
  }

  /** Простейший ограничитель скорости в памяти. */
  public static @NonNull RateLimiter inMemoryRateLimiter() {
    return new InMemoryRateLimiter(1000);
  }

  /** Ограничитель скорости на базе Redis. */
  public static @NonNull RateLimiter redisRateLimiter(@NonNull JedisPool pool) {
    return new RedisRateLimiter(pool);
  }

  /** Упрощённый билдёр для {@link AntiSpamInterceptor}. */
  public static @NonNull AntiSpamInterceptor antiSpamInterceptor(@NonNull Set<String> badDomains) {
    return AntiSpamInterceptor.builder()
        .badDomains(badDomains)
        .flood(inMemoryRateLimiter())
        .dup(inMemoryDuplicateProvider(Duration.ofSeconds(10), 10))
        .captcha(inMemoryCaptchaProvider(Duration.ofSeconds(30), 100))
        .build();
  }
}
