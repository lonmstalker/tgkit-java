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
module io.github.tgkit.validator {
  requires io.github.tgkit.api;
  requires telegrambots;
  requires telegrambots.meta;
  requires static org.checkerframework.checker.qual;
  requires language.detector;
  requires org.apache.tika.langdetect.optimaize;
  requires com.google.common;

  exports io.github.tgkit.validator.impl;
  exports io.github.tgkit.validator.language;
  exports io.github.tgkit.validator.moderation;
}
