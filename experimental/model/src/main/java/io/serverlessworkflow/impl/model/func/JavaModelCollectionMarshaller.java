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
package io.serverlessworkflow.impl.model.func;

import io.serverlessworkflow.impl.marshaller.CustomObjectMarshaller;
import io.serverlessworkflow.impl.marshaller.WorkflowInputBuffer;
import io.serverlessworkflow.impl.marshaller.WorkflowOutputBuffer;
import java.util.Collection;

public class JavaModelCollectionMarshaller implements CustomObjectMarshaller<JavaModelCollection> {

  @Override
  public void write(WorkflowOutputBuffer buffer, JavaModelCollection object) {
    buffer.writeObject(object.asJavaObject());
  }

  @Override
  public JavaModelCollection read(
      WorkflowInputBuffer buffer, Class<? extends JavaModelCollection> clazz) {
    return new JavaModelCollection((Collection<?>) buffer.readObject());
  }

  @Override
  public Class<JavaModelCollection> getObjectClass() {
    return JavaModelCollection.class;
  }

  @Override
  public int priority() {
    return Integer.MAX_VALUE;
  }
}
