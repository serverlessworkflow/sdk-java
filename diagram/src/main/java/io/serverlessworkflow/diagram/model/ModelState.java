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
package io.serverlessworkflow.diagram.model;

import io.serverlessworkflow.diagram.utils.WorkflowDiagramUtils;

import java.util.ArrayList;
import java.util.List;

public class ModelState {
    private String name;
    private String noSpaceName;
    private List<String> stateInfo = new ArrayList<>();

    public ModelState(String name) {
        this.name = name;
        this.noSpaceName = name.replaceAll("\\s", "");
    }

    public void addInfo(String info) {
        stateInfo.add(info);
    }

    @Override
    public String toString() {
        StringBuffer retBuff = new StringBuffer();
        retBuff.append(System.lineSeparator());
        for(String info : stateInfo) {
            retBuff.append(noSpaceName).append(WorkflowDiagramUtils.description)
                    .append(info).append(System.lineSeparator());
        }
        retBuff.append(System.lineSeparator());

        return retBuff.toString();
    }
}
