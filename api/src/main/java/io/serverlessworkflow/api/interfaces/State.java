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
package io.serverlessworkflow.api.interfaces;

import io.serverlessworkflow.api.end.End;
import io.serverlessworkflow.api.error.Error;
import io.serverlessworkflow.api.filters.StateDataFilter;
import io.serverlessworkflow.api.states.DefaultState.Type;
import io.serverlessworkflow.api.timeouts.TimeoutsDefinition;
import io.serverlessworkflow.api.transitions.Transition;

import java.util.List;
import java.util.Map;

public interface State {

    String getId();

    String getName();

    Type getType();

    End getEnd();

    StateDataFilter getStateDataFilter();

    Transition getTransition();

    List<Error> getOnErrors();

    String getCompensatedBy();

    Map<String, String> getMetadata();

    TimeoutsDefinition getTimeouts();
}