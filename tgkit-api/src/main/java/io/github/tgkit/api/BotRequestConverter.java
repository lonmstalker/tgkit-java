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
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Преобразователь {@link Update} в необходимый тип данных запроса.
 *
 * <p><b>Стабильность:</b> API находится в стадии эксперимента и может изменяться.
 *
 * @param <T> тип результата преобразования
 */
@FunctionalInterface
public interface BotRequestConverter<T> {

  /**
   * Конвертирует обновление Telegram в нужный тип.
   *
   * @param update объект {@link Update}, полученный от Telegram
   * @param type тип запроса, определённый библиотекой
   * @return сконвертированный объект
   */
  @NonNull T convert(@NonNull Update update, @NonNull BotRequestType type);
}
