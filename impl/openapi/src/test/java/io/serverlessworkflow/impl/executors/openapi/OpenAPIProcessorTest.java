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
package io.serverlessworkflow.impl.executors.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

public class OpenAPIProcessorTest {

  @Test
  public void testGetPetByIdSwaggerV2() {
    String json = readResource("schema/swagger/petstore.json");
    testGetPetById(json);
  }

  @Test
  public void testGetPetByIdOpenAPI() {
    String json = readResource("schema/openapi/petstore.json");
    testGetPetById(json);
  }

  public void testGetPetById(String json) {
    OperationDefinition definition = new OpenAPIProcessor("getPetById").parse(json);
    assertEquals("GET", definition.getMethod());
    assertEquals("/pet/{petId}", definition.getPath());
    assertTrue(checkServer(definition.getServers(), "https://petstore.swagger.io/v2"));
    assertEquals(1, definition.getParameters().size());
    ParameterDefinition param = definition.getParameters().get(0);
    assertEquals("path", param.getIn());
    assertEquals("petId", param.getName());
    assertTrue(param.getRequired());
  }

  @Test
  public void testAddPetByIdSwaggerV2() {
    String swaggerJson = readResource("schema/swagger/petstore.json");
    testAddPetById(swaggerJson);
  }

  @Test
  public void testAddPetByIdOpenAPI() {
    String json = readResource("schema/openapi/petstore.json");
    testAddPetById(json);
  }

  public void testAddPetById(String json) {
    OperationDefinition definition = new OpenAPIProcessor("addPet").parse(json);

    assertEquals("POST", definition.getMethod());
    assertEquals("/pet", definition.getPath());
    assertTrue(checkServer(definition.getServers(), "https://petstore.swagger.io/v2"));
    assertEquals(6, definition.getParameters().size());

    ParameterDefinition param = definition.getParameters().get(0);
    assertEquals("body", param.getIn());
    assertEquals("id", param.getName());
    assertFalse(param.getRequired());
    param = definition.getParameters().get(1);
    assertEquals("body", param.getIn());
    assertEquals("category", param.getName());
    assertFalse(param.getRequired());
    param = definition.getParameters().get(2);
    assertEquals("body", param.getIn());
    assertEquals("name", param.getName());
    assertTrue(param.getRequired());
    param = definition.getParameters().get(3);
    assertEquals("body", param.getIn());
    assertEquals("photoUrls", param.getName());
    assertTrue(param.getRequired());
    param = definition.getParameters().get(4);
    assertEquals("body", param.getIn());
    assertEquals("tags", param.getName());
    assertFalse(param.getRequired());
    param = definition.getParameters().get(5);
    assertEquals("body", param.getIn());
    assertEquals("status", param.getName());
    assertFalse(param.getRequired());
  }

  @Test
  public void testGetInventorySwaggerV2() {
    String swaggerJson = readResource("schema/swagger/petstore.json");
    testGetInventory(swaggerJson);
  }

  @Test
  public void testGetInventoryOpenAPI() {
    String json = readResource("schema/openapi/petstore.json");
    testGetInventory(json);
  }

  public void testGetInventory(String json) {
    OperationDefinition definition = new OpenAPIProcessor("getInventory").parse(json);

    assertEquals("GET", definition.getMethod());
    assertEquals("/store/inventory", definition.getPath());
    assertTrue(checkServer(definition.getServers(), "https://petstore.swagger.io/v2"));
    assertEquals(0, definition.getParameters().size());
  }

  @Test
  public void testPlaceOrderSwaggerV2() {
    String json = readResource("schema/swagger/petstore.json");
    testPlaceOrder(json);
  }

  @Test
  public void testPlaceOrderOpenAPI() {
    String json = readResource("schema/openapi/petstore.json");
    testPlaceOrder(json);
  }

  public void testPlaceOrder(String json) {
    OperationDefinition definition = new OpenAPIProcessor("placeOrder").parse(json);

    assertEquals("POST", definition.getMethod());
    assertEquals("/store/order", definition.getPath());
    assertTrue(checkServer(definition.getServers(), "https://petstore.swagger.io/v2"));
    assertEquals(6, definition.getParameters().size());
    ParameterDefinition param = definition.getParameters().get(0);
    assertEquals("body", param.getIn());
    assertEquals("id", param.getName());
    assertFalse(param.getRequired());

    param = definition.getParameters().get(1);
    assertEquals("body", param.getIn());
    assertEquals("petId", param.getName());
    assertFalse(param.getRequired());
    param = definition.getParameters().get(2);
    assertEquals("body", param.getIn());
    assertEquals("quantity", param.getName());
    assertFalse(param.getRequired());
    param = definition.getParameters().get(3);
    assertEquals("body", param.getIn());
    assertEquals("shipDate", param.getName());
    assertFalse(param.getRequired());
    param = definition.getParameters().get(4);
    assertEquals("body", param.getIn());
    assertEquals("status", param.getName());
    assertFalse(param.getRequired());
    param = definition.getParameters().get(5);
    assertEquals("body", param.getIn());
    assertEquals("complete", param.getName());
    assertFalse(param.getRequired());
  }

  @Test
  public void testLoginUserSwaggerV2() {
    String json = readResource("schema/swagger/petstore.json");
    testLoginUser(json);
  }

  @Test
  public void testLoginUserOpenAPI() {
    String json = readResource("schema/openapi/petstore.json");
    testLoginUser(json);
  }

  public void testLoginUser(String json) {
    OperationDefinition definition = new OpenAPIProcessor("loginUser").parse(json);

    assertEquals("GET", definition.getMethod());
    assertEquals("/user/login", definition.getPath());
    assertTrue(checkServer(definition.getServers(), "https://petstore.swagger.io/v2"));
    assertEquals(2, definition.getParameters().size());
    ParameterDefinition param1 = definition.getParameters().get(0);
    assertEquals("query", param1.getIn());
    assertEquals("username", param1.getName());
    assertTrue(param1.getRequired());
    ParameterDefinition param2 = definition.getParameters().get(1);
    assertEquals("query", param2.getIn());
    assertEquals("password", param2.getName());
    assertTrue(param2.getRequired());
  }

  private boolean checkServer(List<String> servers, String expected) {
    for (String server : servers) {
      if (server.equals(expected)) {
        return true;
      }
    }
    return false;
  }

  public static String readResource(String path) {
    try (InputStream is =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
      if (is == null) {
        throw new IllegalArgumentException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read resource: " + path, e);
    }
  }
}
