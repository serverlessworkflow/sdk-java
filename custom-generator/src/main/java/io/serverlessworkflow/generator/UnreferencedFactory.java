package io.serverlessworkflow.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JType;
import java.util.Iterator;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.rules.RuleFactory;
import org.jsonschema2pojo.rules.SchemaRule;

public class UnreferencedFactory extends RuleFactory {
  @Override
  public Rule<JClassContainer, JType> getSchemaRule() {
    return new MySchemaRule(this);
  }

  private class MySchemaRule extends SchemaRule {

    public MySchemaRule(UnreferencedFactory jsonSchemaRuleFactory) {
      super(jsonSchemaRuleFactory);
    }

    @Override
    public JType apply(
        String nodeName,
        JsonNode schemaNode,
        JsonNode parent,
        JClassContainer generatableType,
        Schema schema) {
      JType result = super.apply(nodeName, schemaNode, parent, generatableType, schema);
      final JsonNode definitions = schemaNode.get("$defs");
      if (definitions != null && definitions.isObject()) {
        final ObjectNode objectNode = (ObjectNode) definitions;
        final Iterator<String> nodeIterator = objectNode.fieldNames();
        while (nodeIterator.hasNext()) {
          final String name = nodeIterator.next();
          try {
            getSchemaRule()
                .apply(
                    name,
                    (ObjectNode) objectNode.get(name),
                    schemaNode,
                    generatableType.getPackage(),
                    getSchemaStore()
                        .create(
                            schema,
                            "#/$defs/" + name,
                            getGenerationConfig().getRefFragmentPathDelimiters()));
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      }
      return result;
    }
  }
}
