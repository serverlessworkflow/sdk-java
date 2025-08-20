/*
 * Copyright 2020-Present The Serverless Workflow Specification Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.acme;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class EmailDrafts {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static EmailDraft parse(String json) {
    try {
      var node = MAPPER.readTree(json);
      var subject = node.path("subject").asText("");
      var bodyPlain = node.path("body_plain").asText("");
      var links = new ArrayList<String>();
      node.path("links").forEach(n -> links.add(n.asText()));
      return new EmailDraft(subject, bodyPlain, links);
    } catch (IOException e) {
      return new EmailDraft("", "", List.of());
    }
  }
}
