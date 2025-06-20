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
package io.github.tgkit.api;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Адаптер бота, выполняющий обработку входящих {@link Update} и формирующий ответ в виде метода
 * Telegram API.
 */
@FunctionalInterface
public interface BotAdapter {

  /**
   * Обработать входящее обновление Telegram.
   *
   * @param update полученное от Telegram обновление
   * @return метод Telegram API для отправки пользователю или {@code null}, если ответа не требуется
   */
  @Nullable BotApiMethod<?> handle(@NonNull Update update) throws Exception;
}
