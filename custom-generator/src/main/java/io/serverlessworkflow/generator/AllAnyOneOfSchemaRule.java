package io.serverlessworkflow.generator;

import static org.apache.commons.lang3.StringUtils.*;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
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

    if (!schemaNode.has("properties") && unionTypes.isEmpty() && refType.isPresent()) {
      return refType.get();
    }

    JType javaType =
        schemaNode.has("enum")
            ? ruleFactory.getEnumRule().apply(nodeName, schemaNode, parent, generatableType, schema)
            : ruleFactory
                .getTypeRule()
                .apply(nodeName, schemaNode, parent, generatableType.getPackage(), schema);

    if (javaType instanceof JDefinedClass) {
      JDefinedClass definedClass = (JDefinedClass) javaType;
      refType.ifPresent(
          type -> {
            if (type instanceof JClass) {
              definedClass._extends((JClass) type);
            } else {
              wrapIt(definedClass, type);
            }
          });
      unionTypes.forEach(unionType -> wrapIt(definedClass, unionType));
    }
    schema.setJavaTypeIfEmpty(javaType);

    return javaType;
  }

  private void wrapIt(JDefinedClass definedClass, JType unionType) {
    JFieldVar instanceField =
        definedClass.field(
            JMod.PRIVATE,
            unionType,
            ruleFactory.getNameHelper().getPropertyName(unionType.name(), null));
    instanceField.annotate(JsonUnwrapped.class);
    JMethod method =
        definedClass.method(
            JMod.PUBLIC,
            unionType,
            ruleFactory.getNameHelper().getGetterName(unionType.name(), unionType, null));
    method.body()._return(instanceField);
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
        types.add(
            apply(
                nodeName,
                oneOf,
                parent,
                generatableType.getPackage(),
                ruleFactory
                    .getSchemaStore()
                    .create(
                        URI.create(parentSchema.getId().toString() + '/' + prefix + '/' + i++),
                        ruleFactory.getGenerationConfig().getRefFragmentPathDelimiters())));
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
      String[] nameParts = split(ref, "/\\#");
      nameFromRef = nameParts[nameParts.length - 1];
    }

    try {
      return URLDecoder.decode(nameFromRef, "utf-8");
    } catch (UnsupportedEncodingException e) {
      throw new GenerationException("Failed to decode ref: " + ref, e);
    }
  }
}
