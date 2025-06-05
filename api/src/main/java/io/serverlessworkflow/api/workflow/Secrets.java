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

import java.util.Collections;
import java.util.List;

public class Secrets {
    private String refValue;
    private List<String> secretDefs;

    public Secrets() {
    }

    public Secrets(String refValue) {
        this.refValue = refValue;
    }

    public Secrets(List<String> secretDefs) {
        this.secretDefs = secretDefs;
    }

    public String getRefValue() {
        return refValue;
    }

    public void setRefValue(String refValue) {
        this.refValue = refValue;
    }

    public List<String> getSecretDefs() {
        if (secretDefs == null) {
            return Collections.emptyList();
        }
        return secretDefs;
    }

    public void setSecretDefs(List<String> secretDefs) {
        this.secretDefs = secretDefs;
    }
}
