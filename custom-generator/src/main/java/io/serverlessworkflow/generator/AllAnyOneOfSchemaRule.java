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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.jsonschema2pojo.Jsonschema2Pojo;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.exception.GenerationException;
import org.jsonschema2pojo.rules.RuleFactory;
import org.jsonschema2pojo.rules.SchemaRule;

class AllAnyOneOfSchemaRule extends SchemaRule {

  private RuleFactory ruleFactory;

  AllAnyOneOfSchemaRule(RuleFactory ruleFactory) {
    super(ruleFactory);
    this.ruleFactory = ruleFactory;
  }

  @Override
  public JType apply(
      String nodeName,
      JsonNode schemaNode,
      JsonNode parent,
      JClassContainer generatableType,
      Schema schema) {

    Optional<JType> refType = refType(nodeName, schemaNode, parent, generatableType, schema);
    Collection<JType> unionTypes = new LinkedHashSet<>();

    unionType("oneOf", nodeName, schemaNode, parent, generatableType, schema, unionTypes);
    unionType("anyOf", nodeName, schemaNode, parent, generatableType, schema, unionTypes);
    unionType("allOf", nodeName, schemaNode, parent, generatableType, schema, unionTypes);

    JType javaType;
    if (schemaNode.has("enum")) {
      javaType =
          ruleFactory.getEnumRule().apply(nodeName, schemaNode, parent, generatableType, schema);
    } else if (!schemaNode.has("properties") && unionTypes.isEmpty() && refType.isPresent()) {
      javaType = refType.get();

    } else {
      javaType =
          ruleFactory
              .getTypeRule()
              .apply(nodeName, schemaNode, parent, generatableType.getPackage(), schema);
      if (javaType instanceof JDefinedClass) {
        populateClass((JDefinedClass) javaType, refType, unionTypes);
      } else if (isCandidateForCreation(unionTypes)) {
        javaType = createUnionClass(nodeName, generatableType.getPackage(), refType, unionTypes);
      }
      schema.setJavaTypeIfEmpty(javaType);
    }
    return javaType;
  }

  private boolean isCandidateForCreation(Collection<JType> unionTypes) {
    return !unionTypes.isEmpty()
        && unionTypes.stream()
            .allMatch(
                o ->
                    o instanceof JClass
                        && !((JClass) o).isPrimitive()
                        && !o.name().equals("String"));
  }

  private JDefinedClass populateClass(
      JDefinedClass definedClass, Optional<JType> refType, Collection<JType> unionTypes) {
    JType clazzClass = definedClass.owner()._ref(Object.class);

    Optional<JFieldVar> valueField;
    if (!unionTypes.isEmpty()) {
      valueField =
          Optional.of(
              definedClass.field(
                  JMod.PRIVATE,
                  clazzClass,
                  ruleFactory.getNameHelper().getPropertyName("value", null),
                  null));

      definedClass._implements(
          definedClass.owner().ref(GeneratorUtils.ONE_OF_VALUE_PROVIDER_INTERFACE_NAME));

      GeneratorUtils.implementInterface(definedClass, valueField.orElseThrow());

      try {
        JDefinedClass serializer = generateSerializer(definedClass);
        definedClass.annotate(JsonSerialize.class).param("using", serializer);
      } catch (JClassAlreadyExistsException ex) {
        // already serialized aware
      }

      try {
        JDefinedClass deserializer = generateDeserializer(definedClass, unionTypes);
        definedClass.annotate(JsonDeserialize.class).param("using", deserializer);
      } catch (JClassAlreadyExistsException ex) {
        // already deserialized aware
      }
      for (JType unionType : unionTypes) {
        wrapIt(definedClass, valueField, unionType);
      }
    } else {
      valueField = Optional.empty();
    }

    refType.ifPresent(
        type -> {
          if (type instanceof JClass) {
            definedClass._extends((JClass) type);
          } else {
            wrapIt(definedClass, valueField, type);
          }
        });

    if (definedClass.constructors().hasNext()
        && definedClass.getConstructor(new JType[0]) == null) {
      definedClass.constructor(JMod.PUBLIC);
    }
    return definedClass;
  }

  private JDefinedClass generateSerializer(JDefinedClass relatedClass)
      throws JClassAlreadyExistsException {
    JDefinedClass definedClass = GeneratorUtils.serializerClass(relatedClass);
    GeneratorUtils.fillSerializer(
        definedClass,
        relatedClass,
        (method, valueParam, genParam) ->
            method
                .body()
                .staticInvoke(
                    definedClass.owner().ref(GeneratorUtils.SERIALIZE_HELPER_NAME),
                    "serializeOneOf")
                .arg(genParam)
                .arg(valueParam));
    return definedClass;
  }

  private JDefinedClass generateDeserializer(
      JDefinedClass relatedClass, Collection<JType> unionTypes)
      throws JClassAlreadyExistsException {
    JDefinedClass definedClass = GeneratorUtils.deserializerClass(relatedClass);
    GeneratorUtils.fillDeserializer(
        definedClass,
        relatedClass,
        (method, parserParam) -> {
          JBlock body = method.body();
          JInvocation list = definedClass.owner().ref(List.class).staticInvoke("of");
          unionTypes.forEach(c -> list.arg(((JClass) c).dotclass()));
          body._return(
              definedClass
                  .owner()
                  .ref(GeneratorUtils.DESERIALIZE_HELPER_NAME)
                  .staticInvoke("deserializeOneOf")
                  .arg(parserParam)
                  .arg(relatedClass.dotclass())
                  .arg(list));
        });
    return definedClass;
  }

  private JDefinedClass createUnionClass(
      String nodeName, JPackage container, Optional<JType> refType, Collection<JType> unionTypes) {
    try {
      return populateClass(
          container._class(
              ruleFactory.getNameHelper().getUniqueClassName(nodeName, null, container)),
          refType,
          unionTypes);
    } catch (JClassAlreadyExistsException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private void wrapIt(JDefinedClass definedClass, Optional<JFieldVar> valueField, JType unionType) {
    final String name = unionType.name();
    JFieldVar instanceField =
        definedClass.field(
            JMod.PRIVATE, unionType, ruleFactory.getNameHelper().getPropertyName(name, null));
    GeneratorUtils.buildMethod(definedClass, instanceField, ruleFactory.getNameHelper(), name);
    JMethod constructor = definedClass.constructor(JMod.PUBLIC);
    JVar instanceParam = constructor.param(unionType, instanceField.name());
    JBlock body = constructor.body();
    valueField.ifPresent(v -> body.assign(JExpr._this().ref(v), instanceParam));
    body.assign(JExpr._this().ref(instanceField), instanceParam);
  }

  private void unionType(
      String prefix,
      String nodeName,
      JsonNode schemaNode,
      JsonNode parent,
      JClassContainer generatableType,
      Schema parentSchema,
      Collection<JType> types) {
    if (schemaNode.has(prefix)) {
      int i = 0;
      for (JsonNode oneOf : (ArrayNode) schemaNode.get(prefix)) {
        String ref = parentSchema.getId().toString() + '/' + prefix + '/' + i++;
        Schema schema =
            ruleFactory
                .getSchemaStore()
                .create(
                    URI.create(ref),
                    ruleFactory.getGenerationConfig().getRefFragmentPathDelimiters());
        types.add(
            schema.isGenerated()
                ? schema.getJavaType()
                : apply(nodeName, oneOf, parent, generatableType.getPackage(), schema));
      }
    }
  }

  private Optional<JType> refType(
      String nodeName,
      JsonNode schemaNode,
      JsonNode parent,
      JClassContainer generatableType,
      Schema parentSchema) {
    if (schemaNode.has("$ref")) {
      String ref = schemaNode.get("$ref").asText();
      Schema schema =
          ruleFactory
              .getSchemaStore()
              .create(
                  parentSchema,
                  ref,
                  ruleFactory.getGenerationConfig().getRefFragmentPathDelimiters());

      return Optional.of(
          schema.isGenerated()
              ? schema.getJavaType()
              : apply(
                  nameFromRef(ref, nodeName),
                  schema.getContent(),
                  parent,
                  generatableType,
                  schema));
    }
    return Optional.empty();
  }

  private String nameFromRef(String ref, String nodeName) {
    if ("#".equals(ref)) {
      return nodeName;
    }
    String nameFromRef;
    if (!ref.contains("#")) {
      nameFromRef = Jsonschema2Pojo.getNodeName(ref, ruleFactory.getGenerationConfig());
    } else {
      String[] nameParts = ref.split("[/\\#]");
      nameFromRef = nameParts[nameParts.length - 1];
    }

    try {
      return URLDecoder.decode(nameFromRef, "utf-8");
    } catch (UnsupportedEncodingException e) {
      throw new GenerationException("Failed to decode ref: " + ref, e);
    }
  }
}
