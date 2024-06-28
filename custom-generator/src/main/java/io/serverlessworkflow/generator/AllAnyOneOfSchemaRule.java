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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashSet;
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
    Collection<JType> unionTypes = new HashSet<>();

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
    unionTypes.forEach(unionType -> wrapIt(definedClass, unionType));
    refType.ifPresent(
        type -> {
          if (type instanceof JClass) {
            definedClass._extends((JClass) type);
          } else {
            wrapIt(definedClass, type);
          }
        });
    if (definedClass.constructors().hasNext()
        && definedClass.getConstructor(new JType[0]) == null) {
      definedClass.constructor(JMod.PUBLIC);
    }
    return definedClass;
  }

  private JDefinedClass createUnionClass(
      String nodeName, JPackage container, Optional<JType> refType, Collection<JType> unionTypes) {
    String className = ruleFactory.getNameHelper().getUniqueClassName(nodeName, null, container);
    try {
      return populateClass(container._class(className), refType, unionTypes);
    } catch (JClassAlreadyExistsException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private void wrapIt(JDefinedClass definedClass, JType unionType) {
    JFieldVar instanceField =
        GeneratorUtils.addOptionalGetter(
            definedClass, unionType, ruleFactory.getNameHelper(), unionType.name());
    JMethod constructor = definedClass.constructor(JMod.PUBLIC);
    constructor
        .body()
        .assign(
            JExpr._this().ref(instanceField), constructor.param(unionType, instanceField.name()));
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
