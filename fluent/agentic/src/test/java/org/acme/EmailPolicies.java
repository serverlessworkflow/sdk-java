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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class EmailPolicies {

  public static PolicyDecision policyCheck(EmailDraft d) {
    var notes = new ArrayList<String>();
    var subject = safeTrim(d.subject());
    var body = safeTrim(d.bodyPlain());

    if (subject.isEmpty()) notes.add("Missing subject");
    if (body.isEmpty()) notes.add("Missing body");

    var secret = Pattern.compile("(?i)(api[-_]?key|secret|password|token)\\s*[:=]\\s*\\S+");
    if (secret.matcher(body).find() || secret.matcher(subject).find()) {
      notes.add("Suspected secret detected");
      return new PolicyDecision(Decision.BLOCKED, d, notes);
    }

    var allow = Set.of("example.com", "acme.com");
    var badLinks = new ArrayList<String>();
    for (String url : d.links() == null ? List.<String>of() : d.links()) {
      try {
        String host = URI.create(url).getHost();
        if (host == null || allow.stream().noneMatch(host::endsWith)) {
          badLinks.add(url);
        }
      } catch (IllegalArgumentException ignored) {
        badLinks.add(url);
      }
    }
    if (!badLinks.isEmpty()) {
      notes.add("Non-allowed or malformed links: " + badLinks);
    }

    var decision = notes.isEmpty() ? Decision.AUTO_SEND : Decision.REVIEW;
    return new PolicyDecision(decision, new EmailDraft(subject, body, d.links()), notes);
  }

  private static String safeTrim(String s) {
    return s == null ? "" : s.trim();
  }

  public enum Decision {
    AUTO_SEND,
    REVIEW,
    BLOCKED
  }
}
