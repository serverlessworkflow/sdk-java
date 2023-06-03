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
package io.serverlessworkflow.api.validation;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;

public class WorkflowSchemaLoader {
  // note update this when supporting new version
  private static final String BASE_SCHEMA_URL = "https://serverlessworkflow.io/schemas/0.8/";

  public static Schema getWorkflowSchema() {
    try {
      SchemaLoader schemaLoader =
          SchemaLoader.builder()
              .schemaJson(readJsonFromUrl(BASE_SCHEMA_URL + "workflow.json"))
              .resolutionScope(BASE_SCHEMA_URL)
              .draftV7Support()
              .build();
      return schemaLoader.load().build();
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to read schema: " + e.getMessage());
    }
  }

  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
    InputStream is = new URL(url).openStream();
    try {
      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
      String jsonText = readAll(rd);
      JSONObject json = new JSONObject(jsonText);
      return json;
    } finally {
      is.close();
    }
  }
}
