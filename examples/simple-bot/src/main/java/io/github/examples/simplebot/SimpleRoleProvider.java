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
package io.github.examples.simplebot;

import static io.github.tgkit.api.update.UpdateUtils.resolveChatId;
import static io.github.tgkit.api.update.UpdateUtils.resolveUserId;

import io.github.tgkit.api.user.BotUserInfo;
import io.github.tgkit.api.user.BotUserProvider;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.telegram.telegrambots.meta.api.objects.Update;

public class SimpleRoleProvider implements BotUserProvider {

  @Override
  public @NonNull BotUserInfo resolve(@NonNull Update update) {
    var userId = resolveUserId(update);
    var chatId = resolveChatId(update);
    return new SimpleInfo(chatId, userId, null, Set.of());
  }

  private record SimpleInfo(
      @Nullable Long chatId,
      @Nullable Long userId,
      @Nullable Long internalUserId,
      Set<String> roles)
      implements BotUserInfo {}
}
