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
package io.serverlessworkflow.generator;

import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import org.jsonschema2pojo.util.NameHelper;

public class GeneratorUtils {

  public static JFieldVar addGetter(
      JDefinedClass definedClass, JType type, NameHelper nameHelper, String name) {
    JFieldVar instanceField =
        definedClass.field(JMod.PRIVATE, type, nameHelper.getPropertyName(name, null));
    JMethod method =
        definedClass.method(JMod.PUBLIC, type, nameHelper.getGetterName(name, type, null));
    method.body()._return(instanceField);
    return instanceField;
  }

  private GeneratorUtils() {}
}
