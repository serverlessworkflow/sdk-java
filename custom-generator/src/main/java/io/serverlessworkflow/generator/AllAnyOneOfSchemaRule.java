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
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import jakarta.validation.ConstraintViolationException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
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

  private static final String REF = "$ref";
  private static final String PATTERN = "pattern";

  private enum Format {
    URI_TEMPLATE("^[A-Za-z][A-Za-z0-9+\\-.]*://.*");

    private final String pattern;

    Format(String pattern) {
      this.pattern = pattern;
    }

    public static Format parse(String str) {
      if (str != null) {
        switch (str) {
          case "uri-template":
            return URI_TEMPLATE;
        }
      }
      return null;
    }

    String pattern() {

      return pattern;
    }
  }

  private static class JTypeWrapper implements Comparable<JTypeWrapper> {

    private final JType type;
    private final JsonNode node;

    public JTypeWrapper(JType type, JsonNode node) {
      this.type = type;
      this.node = node;
    }

    public JType getType() {
      return type;
    }

    public JsonNode getNode() {
      return node;
    }

    @Override
    public int compareTo(JTypeWrapper other) {
      return typeToNumber() - other.typeToNumber();
    }

    private int typeToNumber() {
      if (type.name().equals("Object")) {
        return 6;
      } else if (type.name().equals("String")) {
        return node.has(PATTERN) || node.has(REF) ? 4 : 5;
      } else if (type.isPrimitive()) {
        return 3;
      } else if (type.isReference()) {
        return 2;
      } else if (type.isArray()) {
        return 1;
      } else {
        return 0;
      }
    }
  }

  @Override
  public JType apply(
      String nodeName,
      JsonNode schemaNode,
      JsonNode parent,
      JClassContainer generatableType,
      Schema schema) {

    Optional<JType> refType = refType(nodeName, schemaNode, parent, generatableType, schema);
    List<JTypeWrapper> unionTypes = new ArrayList<>();

    unionType("oneOf", nodeName, schemaNode, parent, generatableType, schema, unionTypes);
    unionType("anyOf", nodeName, schemaNode, parent, generatableType, schema, unionTypes);
    unionType("allOf", nodeName, schemaNode, parent, generatableType, schema, unionTypes);

    Collections.sort(unionTypes);

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
        populateClass(schema, (JDefinedClass) javaType, refType, unionTypes);
      } else if (!unionTypes.isEmpty()) {
        javaType =
            createUnionClass(
                schema, nodeName, schemaNode, generatableType.getPackage(), refType, unionTypes);
      }
      schema.setJavaTypeIfEmpty(javaType);
    }
    return javaType;
  }

  private JDefinedClass populateClass(
      Schema parentSchema,
      JDefinedClass definedClass,
      Optional<JType> refType,
      Collection<JTypeWrapper> unionTypes) {
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

      Collection<JTypeWrapper> stringTypes = new ArrayList<>();
      for (JTypeWrapper unionType : unionTypes) {
        if (isStringType(unionType.getType())) {
          stringTypes.add(unionType);
        } else {
          wrapIt(parentSchema, definedClass, valueField, unionType.getType(), unionType.getNode());
        }
      }
      if (!stringTypes.isEmpty()) {
        wrapStrings(parentSchema, definedClass, valueField, stringTypes);
      }

    } else {
      valueField = Optional.empty();
    }

    refType.ifPresent(
        type -> {
          if (type instanceof JClass) {
            definedClass._extends((JClass) type);
          } else {
            wrapIt(parentSchema, definedClass, valueField, type, null);
          }
        });

    if (definedClass.constructors().hasNext()
        && definedClass.getConstructor(new JType[0]) == null) {
      definedClass.constructor(JMod.PUBLIC);
    }
    return definedClass;
  }

  private static boolean isStringType(JType type) {
    return type.name().equals("String");
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
      JDefinedClass relatedClass, Collection<JTypeWrapper> unionTypes)
      throws JClassAlreadyExistsException {
    JDefinedClass definedClass = GeneratorUtils.deserializerClass(relatedClass);
    GeneratorUtils.fillDeserializer(
        definedClass,
        relatedClass,
        (method, parserParam) -> {
          JBlock body = method.body();
          JInvocation list = definedClass.owner().ref(List.class).staticInvoke("of");
          unionTypes.forEach(c -> list.arg(((JClass) c.getType()).dotclass()));
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
      Schema parentSchema,
      String nodeName,
      JsonNode schemaNode,
      JPackage container,
      Optional<JType> refType,
      Collection<JTypeWrapper> unionTypes) {
    try {
      return populateClass(
          parentSchema,
          container._class(
              ruleFactory.getNameHelper().getUniqueClassName(nodeName, schemaNode, container)),
          refType,
          unionTypes);
    } catch (JClassAlreadyExistsException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private void wrapIt(
      Schema parentSchema,
      JDefinedClass definedClass,
      Optional<JFieldVar> valueField,
      JType unionType,
      JsonNode node) {
    JFieldVar instanceField = getInstanceField(parentSchema, definedClass, unionType, node);
    JMethod constructor = definedClass.constructor(JMod.PUBLIC);
    JVar instanceParam = constructor.param(unionType, instanceField.name());
    JBlock body = constructor.body();
    valueField.ifPresent(v -> body.assign(JExpr._this().ref(v), instanceParam));
    body.assign(JExpr._this().ref(instanceField), instanceParam);
  }

  private void wrapStrings(
      Schema parentSchema,
      JDefinedClass definedClass,
      Optional<JFieldVar> valueField,
      Collection<JTypeWrapper> stringTypes) {
    Iterator<JTypeWrapper> iter = stringTypes.iterator();
    JTypeWrapper first = iter.next();
    JMethod constructor = definedClass.constructor(JMod.PUBLIC);

    JBlock body = constructor.body();
    String pattern = pattern(first.getNode(), parentSchema);
    if (pattern == null && iter.hasNext()) {
      pattern = ".*";
    }
    JFieldVar instanceField =
        getInstanceField(parentSchema, definedClass, first.getType(), first.getNode());
    JVar instanceParam = constructor.param(first.type, instanceField.name());
    valueField.ifPresent(v -> body.assign(JExpr._this().ref(v), instanceParam));
    if (pattern != null) {
      JConditional condition =
          body._if(getPatternCondition(pattern, body, instanceField, instanceParam, definedClass));
      condition._then().assign(JExpr._this().ref(instanceField), instanceParam);
      while (iter.hasNext()) {
        JTypeWrapper item = iter.next();
        instanceField =
            getInstanceField(parentSchema, definedClass, item.getType(), item.getNode());
        pattern = pattern(item.getNode(), parentSchema);
        if (pattern == null) {
          pattern = ".*";
        }
        condition =
            condition._elseif(
                getPatternCondition(pattern, body, instanceField, instanceParam, definedClass));
        condition._then().assign(JExpr._this().ref(instanceField), instanceParam);
      }
      condition
          ._else()
          ._throw(
              JExpr._new(definedClass.owner()._ref(ConstraintViolationException.class))
                  .arg(
                      definedClass
                          .owner()
                          .ref(String.class)
                          .staticInvoke("format")
                          .arg("%s does not match any pattern")
                          .arg(instanceParam))
                  .arg(JExpr._null()));
    } else {
      body.assign(JExpr._this().ref(instanceField), instanceParam);
    }
  }

  private JFieldVar getInstanceField(
      Schema parentSchema, JDefinedClass definedClass, JType type, JsonNode node) {
    JFieldVar instanceField =
        definedClass.field(
            JMod.PRIVATE,
            type,
            ruleFactory
                .getNameHelper()
                .getPropertyName(getTypeName(node, type, parentSchema), node));
    GeneratorUtils.buildMethod(
        definedClass, instanceField, ruleFactory.getNameHelper(), instanceField.name());
    return instanceField;
  }

  private static String getFromNode(JsonNode node, String fieldName) {
    if (node != null) {
      JsonNode item = node.get(fieldName);
      if (item != null) {
        return item.asText();
      }
    }

    return null;
  }

  private JInvocation getPatternCondition(
      String pattern,
      JBlock body,
      JFieldVar instanceField,
      JVar instanceParam,
      JDefinedClass definedClass) {
    JFieldVar patternField =
        definedClass.field(
            JMod.PRIVATE | JMod.STATIC | JMod.FINAL,
            Pattern.class,
            instanceField.name() + "_" + "Pattern",
            definedClass.owner().ref(Pattern.class).staticInvoke("compile").arg(pattern));
    return JExpr.invoke(JExpr.invoke(patternField, "matcher").arg(instanceParam), "matches");
  }

  private void unionType(
      String prefix,
      String nodeName,
      JsonNode schemaNode,
      JsonNode parent,
      JClassContainer generatableType,
      Schema parentSchema,
      Collection<JTypeWrapper> types) {
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
            new JTypeWrapper(
                schema.isGenerated()
                    ? schema.getJavaType()
                    : apply(nodeName, oneOf, parent, generatableType.getPackage(), schema),
                oneOf));
      }
    }
  }

  private Optional<JType> refType(
      String nodeName,
      JsonNode schemaNode,
      JsonNode parent,
      JClassContainer generatableType,
      Schema parentSchema) {
    if (schemaNode.has(REF)) {
      String ref = schemaNode.get(REF).asText();
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

  private JsonNode schemaRef(JsonNode schemaNode, Schema parentSchema) {
    String ref = getFromNode(schemaNode, REF);
    return ref != null
        ? ruleFactory
            .getSchemaStore()
            .create(
                parentSchema, ref, ruleFactory.getGenerationConfig().getRefFragmentPathDelimiters())
            .getContent()
        : null;
  }

  private String getTypeName(JsonNode node, JType type, Schema parentSchema) {
    final String title = "title";
    String name = getFromNode(node, title);
    if (name == null) {
      name = getFromNode(schemaRef(node, parentSchema), title);
    }
    if (name == null) {
      name = type.name();
    }
    return name;
  }

  private String pattern(JsonNode node, Schema parentSchema) {
    String pattern = pattern(node);
    return pattern != null ? pattern : pattern(schemaRef(node, parentSchema));
  }

  private String pattern(JsonNode node) {
    Format format = Format.parse(getFromNode(node, "format"));
    return format != null ? format.pattern() : getFromNode(node, PATTERN);
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
