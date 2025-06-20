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

import io.github.tgkit.internal.BotAdapter;
import io.github.tgkit.internal.annotation.BotCommand;
import io.github.tgkit.internal.annotation.CheckReturnValue;
import io.github.tgkit.internal.loader.ClasspathScanner;
import io.github.tgkit.plugin.BotPlugin;
import io.github.tgkit.plugin.BotPluginContext;
import io.github.tgkit.plugin.internal.BotPluginContextDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;

/** Fluent builder for creating and starting bots. */
public final class BotBuilder {
  private BotBuilder() {}

  public static @NonNull BotBuilderImpl builder() {
    return new BotBuilderImpl();
  }

  /** Actual implementation of the builder. */
  public static final class BotBuilderImpl {
    private final List<String> packages = new ArrayList<>();
    private final List<Supplier<BotPlugin>> plugins = new ArrayList<>();
    private String token;
    private boolean polling = true;
    private boolean started;

    @CheckReturnValue
    public @NonNull BotBuilderImpl token(@NonNull String token) {
      this.token = Objects.requireNonNull(token);
      return this;
    }

    @CheckReturnValue
    public @NonNull BotBuilderImpl withPolling() {
      this.polling = true;
      return this;
    }

    @CheckReturnValue
    public @NonNull BotBuilderImpl withWebhook() {
      this.polling = false;
      return this;
    }

    @CheckReturnValue
    public @NonNull BotBuilderImpl scan(@NonNull String pkg) {
      packages.add(Objects.requireNonNull(pkg));
      return this;
    }

    @CheckReturnValue
    public @NonNull BotBuilderImpl plugin(@NonNull Supplier<BotPlugin> supplier) {
      plugins.add(Objects.requireNonNull(supplier));
      return this;
    }

    /**
     * Создаёт и запускает бота.
     *
     * @return инициализированный бот
     */
    @CheckReturnValue
    public synchronized @NonNull Bot start() {
      if (started) {
        throw new IllegalStateException("start() already called");
      }
      Objects.requireNonNull(token, "token must be set");
      started = true;

      BotConfig config = BotConfig.builder().build();
      BotAdapter adapter = u -> null;
      Bot bot;
      if (polling) {
        bot = BotFactory.INSTANCE.from(token, config, adapter);
      } else {
        SetWebhook hook = new SetWebhook();
        bot = BotFactory.INSTANCE.from(token, config, adapter, hook);
      }

      BotCommandRegistry registry = bot.registry();
      for (String pkg : packages) {
        Set<Class<?>> cmdClasses = ClasspathScanner.findAnnotated(BotCommand.class, pkg);
        for (Class<?> cls : cmdClasses) {
          try {
            Object instance = cls.getDeclaredConstructor().newInstance();
            registry.add((io.github.tgkit.internal.BotCommand<?>) instance);
          } catch (Exception e) {
            throw new IllegalStateException("Cannot init command " + cls, e);
          }
        }

        Set<Class<?>> pluginClasses =
            ClasspathScanner.findAnnotated(io.github.tgkit.plugin.annotation.BotPlugin.class, pkg);
        for (Class<?> cls : pluginClasses) {
          plugins.add(
              () -> {
                try {
                  return (BotPlugin) cls.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                  throw new IllegalStateException("Cannot init plugin " + cls, e);
                }
              });
        }
      }

      BotPluginContext ctx = new BotPluginContextDefault(ClassLoader.getSystemClassLoader());
      for (Supplier<BotPlugin> supplier : plugins) {
        BotPlugin p = supplier.get();
        try {
          p.onLoad(ctx);
          p.start();
        } catch (Exception e) {
          throw new IllegalStateException("Plugin start failed", e);
        }
      }

      return bot;
    }
  }
}
