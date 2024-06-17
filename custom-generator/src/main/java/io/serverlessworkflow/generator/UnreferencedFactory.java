package io.serverlessworkflow.generator;

import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JType;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.rules.RuleFactory;

public class UnreferencedFactory extends RuleFactory {
  @Override
  public Rule<JClassContainer, JType> getSchemaRule() {
    return new AllAnyOneOfSchemaRule(this);
  }
}
