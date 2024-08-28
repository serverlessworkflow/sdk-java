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
package io.serverlessworkflow.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.Collection;

public class DeserializeHelper {

  public static <T> T deserializeOneOf(
      JsonParser p, Class<T> targetClass, Collection<Class<?>> unionTypes) throws IOException {
    TreeNode node = p.readValueAsTree();
    JsonProcessingException ex =
        new JsonMappingException(p, "Problem deserializing " + targetClass);
    for (Class<?> unionType : unionTypes) {
      try {
        Object object = p.getCodec().treeToValue(node, unionType);
        return targetClass.getConstructor(unionType).newInstance(object);
      } catch (IOException | ReflectiveOperationException | ConstraintViolationException io) {
        ex.addSuppressed(io);
      }
    }
    throw ex;
  }

  public static <T> T deserializeItem(JsonParser p, Class<T> targetClass, Class<?> valueClass)
      throws IOException {
    TreeNode node = p.readValueAsTree();
    String fieldName = node.fieldNames().next();
    try {
      return targetClass
          .getConstructor(String.class, valueClass)
          .newInstance(fieldName, p.getCodec().treeToValue(node.get(fieldName), valueClass));
    } catch (ReflectiveOperationException e) {
      throw new IOException(e);
    }
  }
}
