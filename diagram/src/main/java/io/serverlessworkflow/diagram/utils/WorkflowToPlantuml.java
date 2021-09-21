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
package io.serverlessworkflow.diagram.utils;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.diagram.config.ThymeleafConfig;
import io.serverlessworkflow.diagram.model.WorkflowDiagramModel;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

public class WorkflowToPlantuml {
  public static String convert(Workflow workflow, boolean showLegend) {
    TemplateEngine plantUmlTemplateEngine = ThymeleafConfig.templateEngine;
    Context context = new Context();
    context.setVariable("diagram", new WorkflowDiagramModel(workflow, showLegend));

    return plantUmlTemplateEngine.process("workflow-template", context);
  }
}
