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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;
import java.io.IOException;
import org.jsonschema2pojo.util.NameHelper;

public class GeneratorUtils {

  public static final String SERIALIZE_HELPER_NAME =
      "io.serverlessworkflow.serialization.SerializeHelper";
  public static final String DESERIALIZE_HELPER_NAME =
      "io.serverlessworkflow.serialization.DeserializeHelper";
  public static final String ONE_OF_VALUE_PROVIDER_INTERFACE_NAME =
      "io.serverlessworkflow.api.OneOfValueProvider";

  @FunctionalInterface
  public interface SerializerFiller {
    void accept(JMethod method, JVar valueParam, JVar genParam);
  }

  @FunctionalInterface
  public interface DeserializerFiller {
    void accept(JMethod method, JVar parserParam);
  }

  public static JDefinedClass serializerClass(JDefinedClass relatedClass)
      throws JClassAlreadyExistsException {
    return createClass(relatedClass, JsonSerializer.class, "Serializer");
  }

  public static JDefinedClass deserializerClass(JDefinedClass relatedClass)
      throws JClassAlreadyExistsException {
    return createClass(relatedClass, JsonDeserializer.class, "Deserializer");
  }

  public static JMethod implementInterface(JDefinedClass definedClass, JFieldVar valueField) {
    JMethod method = definedClass.method(JMod.PUBLIC, Object.class, "get");
    method.annotate(Override.class);
    method.body()._return(valueField);
    return method;
  }

  public static JMethod buildMethod(
      JDefinedClass definedClass, JFieldVar instanceField, NameHelper nameHelper, String name) {
    JMethod method =
        definedClass.method(
            JMod.PUBLIC,
            instanceField.type(),
            nameHelper.getGetterName(name, instanceField.type(), null));
    method.body()._return(instanceField);
    return method;
  }

  public static void fillSerializer(
      JDefinedClass definedClass, JDefinedClass relatedClass, SerializerFiller filler) {
    JMethod method = definedClass.method(JMod.PUBLIC, void.class, "serialize");
    method.annotate(Override.class);
    method._throws(IOException.class);
    JVar valueParam = method.param(relatedClass, "value");
    JVar genParam = method.param(JsonGenerator.class, "gen");
    method.param(SerializerProvider.class, "serializers");
    filler.accept(method, valueParam, genParam);
  }

  public static void fillDeserializer(
      JDefinedClass definedClass, JDefinedClass relatedClass, DeserializerFiller filler) {
    JMethod method = definedClass.method(JMod.PUBLIC, relatedClass, "deserialize");
    method.annotate(Override.class);
    method._throws(IOException.class);
    JVar parserParam = method.param(JsonParser.class, "parser");
    method.param(DeserializationContext.class, "dctx");
    filler.accept(method, parserParam);
  }

  private static JDefinedClass createClass(
      JDefinedClass relatedClass, Class<?> serializerClass, String suffix)
      throws JClassAlreadyExistsException {
    JDefinedClass definedClass =
        relatedClass._package()._class(JMod.NONE, relatedClass.name() + suffix);
    definedClass._extends(definedClass.owner().ref(serializerClass).narrow(relatedClass));
    return definedClass;
  }

  private GeneratorUtils() {}
}
