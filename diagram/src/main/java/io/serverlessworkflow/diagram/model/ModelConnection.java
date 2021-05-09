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
package io.serverlessworkflow.diagram.model;

import io.serverlessworkflow.diagram.utils.WorkflowDiagramUtils;

public class ModelConnection {
    private String left;
    private String right;
    private String desc;

    public ModelConnection(String left, String right, String desc) {
        this.left = left.replaceAll("\\s", "");
        this.right = right.replaceAll("\\s", "");
        this.desc = desc;
    }

    @Override
    public String toString() {
        StringBuilder retBuff = new StringBuilder();
        retBuff.append(System.lineSeparator());
        retBuff.append(left.equals(WorkflowDiagramUtils.wfStart) ? WorkflowDiagramUtils.startEnd : left);
        retBuff.append(WorkflowDiagramUtils.connection);
        retBuff.append(right.equals(WorkflowDiagramUtils.wfEnd) ? WorkflowDiagramUtils.startEnd : right);
        if (desc != null && desc.trim().length() > 0) {
            retBuff.append(WorkflowDiagramUtils.description).append(desc);
        }
        retBuff.append(System.lineSeparator());

        return retBuff.toString();
    }

    public String getLeft() {
        return left;
    }

    public void setLeft(String left) {
        this.left = left;
    }

    public String getRight() {
        return right;
    }

    public void setRight(String right) {
        this.right = right;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
