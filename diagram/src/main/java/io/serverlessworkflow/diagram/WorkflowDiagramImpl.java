/*
 * Copyright 2020-Present The Serverless Workflow Specification Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.serverlessworkflow.diagram;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.interfaces.WorkflowDiagram;
import io.serverlessworkflow.diagram.utils.WorkflowToPlantuml;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

public class WorkflowDiagramImpl implements WorkflowDiagram {


    @SuppressWarnings("unused")
    private String source;
    private Workflow workflow;
    private boolean showLegend = false;

    @Override
    public WorkflowDiagram setWorkflow(Workflow workflow) {
        this.workflow = workflow;
        this.source = Workflow.toJson(workflow);
        return this;
    }

    @Override
    public WorkflowDiagram setSource(String source) {
        this.source = source;
        this.workflow = Workflow.fromSource(source);
        return this;
    }

    @Override
    public String getSvgDiagram() throws Exception {
        if(workflow == null) {
            throw new IllegalAccessException("Unable to get diagram - no workflow set.");
        }
        String diagramSource = WorkflowToPlantuml.convert(workflow, showLegend);
        SourceStringReader reader = new SourceStringReader(diagramSource);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        reader.generateImage(os, new FileFormatOption(FileFormat.SVG));
        os.close();
        return new String(os.toByteArray(), Charset.forName("UTF-8"));
    }

    @Override
    public WorkflowDiagram showLegend(boolean showLegend) {
        this.showLegend = showLegend;
        return this;
    }
}
