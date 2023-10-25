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
package io.serverlessworkflow.api.workflow;

import io.serverlessworkflow.api.events.EventDefinition;
import java.util.List;

public class Events {
  private String refValue;
  private List<EventDefinition> eventDefs;

  public Events() {}

  public Events(List<EventDefinition> eventDefs) {
    this.eventDefs = eventDefs;
  }

  public Events(String refValue) {
    this.refValue = refValue;
  }

  public String getRefValue() {
    return refValue;
  }

  public void setRefValue(String refValue) {
    this.refValue = refValue;
  }

  public List<EventDefinition> getEventDefs() {
    return eventDefs;
  }

  public void setEventDefs(List<EventDefinition> eventDefs) {
    this.eventDefs = eventDefs;
  }
}
