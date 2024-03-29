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
package io.serverlessworkflow.spi.test.providers;

import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TestWorkflowPropertySource implements WorkflowPropertySource {

  private Properties source = new Properties();

  @Override
  public Properties getPropertySource() {
    Map<String, String> propertySourcetMap = new HashMap<>();
    propertySourcetMap.put("wfname", "test-wf");
    propertySourcetMap.put("delaystate.name", "delay-state");
    propertySourcetMap.put("delaystate.timedelay", "PT5S");
    propertySourcetMap.put("delaystate.type", "DELAY");

    source.putAll(propertySourcetMap);

    return source;
  }

  @Override
  public void setPropertySource(Properties source) {
    this.source = source;
  }
}
