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
package io.github.tgkit.security.init;

import io.github.tgkit.api.config.BotGlobalConfig;
import io.github.tgkit.security.antispam.InMemoryDuplicateProvider;
import io.github.tgkit.security.audit.AsyncAuditBus;
import io.github.tgkit.security.captcha.MathCaptchaOperations;
import io.github.tgkit.security.captcha.provider.MathCaptchaProvider;
import io.github.tgkit.security.config.BotSecurityGlobalConfig;
import io.github.tgkit.security.ratelimit.impl.InMemoryRateLimiter;
import io.github.tgkit.security.secret.EnvSecretStore;
import java.time.Duration;
import java.util.Set;
import org.apache.commons.lang3.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Базовая инициализация security-модуля.<br>
 * Аналогично {@link io.github.tgkit.internal.init.BotCoreInitializer}.
 */
public final class BotSecurityInitializer {

  private static final Logger log = LoggerFactory.getLogger(BotSecurityInitializer.class);
  private static volatile boolean started;

  private BotSecurityInitializer() {}

  public synchronized void init() {
    if (started) {
      log.warn(
          "[sec-init] BotSecurityInitializer уже вызывался, повторная инициализация игнорируется");
      return;
    }
    log.info("[sec-init] Старт инициализации security-подсистемы…");

    // ── CAPTCHA ───────────────────────────────────────────────────────────
    BotSecurityGlobalConfig.INSTANCE
        .captcha()
        .provider(
            MathCaptchaProvider.builder()
                .ttl(Duration.ofMinutes(5))
                .numberRange(Range.of(1, 10))
                .wrongCount(2)
                .allowedOps(MathCaptchaOperations.OPERATIONS)
                .build());

    // ── Rate-Limiter (in-mem) ─────────────────────────────────────────────
    BotSecurityGlobalConfig.INSTANCE.rateLimit().backend(new InMemoryRateLimiter(2_000));

    // ── Anti-Spam ─────────────────────────────────────────────────────────
    BotSecurityGlobalConfig.INSTANCE
        .antiSpam()
        .duplicateProvider(new InMemoryDuplicateProvider(Duration.ofMinutes(1), 1_000))
        .blacklistDomains(Set.of());

    // ── Secrets-Store (ENV vars) ──────────────────────────────────────────
    BotSecurityGlobalConfig.INSTANCE.secrets().store(new EnvSecretStore());

    // ── Audit ──────────────────────────────────────────
    BotSecurityGlobalConfig.INSTANCE
        .audit()
        .bus(new AsyncAuditBus(BotGlobalConfig.INSTANCE.executors().getIoExecutorService(), 100));

    log.info("[sec-init] Security-подсистема успешно инициализирована ✅");
    started = true;
  }
}
