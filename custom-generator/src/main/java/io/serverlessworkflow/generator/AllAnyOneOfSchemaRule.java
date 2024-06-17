package io.serverlessworkflow.generator;

import static org.apache.commons.lang3.StringUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JType;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Optional;
import org.jsonschema2pojo.Jsonschema2Pojo;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.exception.GenerationException;
import org.jsonschema2pojo.rules.RuleFactory;
import org.jsonschema2pojo.rules.SchemaRule;

public class AllAnyOneOfSchemaRule extends SchemaRule {

  private RuleFactory ruleFactory;

  protected AllAnyOneOfSchemaRule(RuleFactory ruleFactory) {
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
    Optional<JType> oneOfType = oneOfType(nodeName, schemaNode, parent, generatableType, schema);

    Optional<JType> justOne = justOne(refType, oneOfType);

    if (!schemaNode.has("properties") && justOne.isPresent()) {
      return justOne.get();
    }

    JType javaType =
        schemaNode.has("enum")
            ? ruleFactory.getEnumRule().apply(nodeName, schemaNode, parent, generatableType, schema)
            : ruleFactory
                .getTypeRule()
                .apply(nodeName, schemaNode, parent, generatableType.getPackage(), schema);

    if (javaType instanceof JDefinedClass) {
      JDefinedClass definedClass = (JDefinedClass) javaType;
      if (justOne.filter(JClass.class::isInstance).isPresent()) {
        definedClass._extends((JClass) justOne.get());
      } else {
        wrapIt(definedClass, refType, oneOfType);
      }
    }
    schema.setJavaTypeIfEmpty(javaType);

    return javaType;
  }

  @SafeVarargs
  private void wrapIt(JDefinedClass definedClass, Optional<JType>... optionals) {
    for (Optional<JType> optional : optionals) {
      optional.ifPresent(c -> wrapIt(definedClass, c));
    }
  }

  private void wrapIt(JDefinedClass definedClass, JType type) {
    // TODO include all paremeters of given type into the defined class
  }

  @SafeVarargs
  private Optional<JType> justOne(Optional<JType>... optionals) {

    Optional<JType> result = Optional.empty();
    for (Optional<JType> optional : optionals) {
      if (optional.isPresent()) {
        if (result.isPresent()) {
          return Optional.empty();
        } else {
          result = optional;
        }
      }
    }
    return result;
  }

  private Optional<JType> oneOfType(
      String nodeName,
      JsonNode schemaNode,
      JsonNode parent,
      JClassContainer generatableType,
      Schema parentSchema) {
    if (schemaNode.has("oneOf")) {
      int i = 0;
      for (JsonNode oneOf : (ArrayNode) schemaNode.get("oneOf")) {
          apply(
              nodeName,
              oneOf,
              parent,
              generatableType.getPackage(),
              ruleFactory
                  .getSchemaStore()
                  .create(
                      URI.create(parentSchema.getId().toString() + "/oneOf/" + i++),
                      ruleFactory.getGenerationConfig().getRefFragmentPathDelimiters()));
      }
    }
    return Optional.empty();
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
