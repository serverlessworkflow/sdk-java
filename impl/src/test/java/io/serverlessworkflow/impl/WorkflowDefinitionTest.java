package io.serverlessworkflow.impl;

import static io.serverlessworkflow.api.WorkflowReader.readWorkflowFromClasspath;

import java.io.IOException;
import java.util.Map;

public class WorkflowDefinitionTest {

  public static void main(String[] args) throws IOException {

    System.out.println(
        WorkflowDefinition.builder(readWorkflowFromClasspath("callHttp.yaml"))
            .build()
            .execute(Map.of("petId", 10))
            .outputAsJavaObject());
  }
}
