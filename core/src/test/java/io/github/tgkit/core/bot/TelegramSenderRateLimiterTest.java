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
package io.github.tgkit.internal.bot;

import static org.junit.jupiter.api.Assertions.*;

import io.github.tgkit.testkit.TestBotBootstrap;
import org.junit.jupiter.api.Test;

class TelegramSenderRateLimiterTest {

  static {
    TestBotBootstrap.initOnce();
  }

  @Test
  void permitsPerSecond() {
    GuavaRateLimiterWrapper limiter = new GuavaRateLimiterWrapper(2);
    long start = System.currentTimeMillis();
    limiter.acquire();
    limiter.acquire();
    limiter.acquire();
    long elapsed = System.currentTimeMillis() - start;
    assertTrue(elapsed >= 1000);
    limiter.close();
  }

  @Test
  void independentLimiters() {
    GuavaRateLimiterWrapper first = new GuavaRateLimiterWrapper(1);
    GuavaRateLimiterWrapper second = new GuavaRateLimiterWrapper(1);

    long start = System.currentTimeMillis();
    first.acquire();
    second.acquire();
    first.acquire();
    long elapsed = System.currentTimeMillis() - start;

    assertTrue(elapsed >= 1000 && elapsed < 2000);
  }
}
