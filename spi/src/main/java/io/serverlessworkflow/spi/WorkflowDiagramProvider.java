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
package io.serverlessworkflow.spi;

import io.serverlessworkflow.api.interfaces.WorkflowDiagram;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowDiagramProvider {
  private WorkflowDiagram workflowDiagram;

  private static Logger logger = LoggerFactory.getLogger(WorkflowDiagramProvider.class);

  public WorkflowDiagramProvider() {
    ServiceLoader<WorkflowDiagram> foundWorkflowDiagrams =
        ServiceLoader.load(WorkflowDiagram.class);
    Iterator<WorkflowDiagram> it = foundWorkflowDiagrams.iterator();
    if (it.hasNext()) {
      workflowDiagram = it.next();
      logger.info("Found workflow diagram: " + workflowDiagram.toString());
    }
  }

  private static class LazyHolder {

    static final WorkflowDiagramProvider INSTANCE = new WorkflowDiagramProvider();
  }

  public static WorkflowDiagramProvider getInstance() {
    return WorkflowDiagramProvider.LazyHolder.INSTANCE;
  }

  public WorkflowDiagram get() {
    return workflowDiagram;
  }
}
