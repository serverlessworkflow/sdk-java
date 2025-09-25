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
package io.serverlessworkflow.api;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import io.serverlessworkflow.api.types.jackson.JacksonMixInModule;
import io.serverlessworkflow.serialization.BeanDeserializerModifierWithValidation;
import io.serverlessworkflow.serialization.URIDeserializer;
import io.serverlessworkflow.serialization.URISerializer;
import java.net.URI;

class ObjectMapperFactory {

  private static final ObjectMapper jsonMapper = configure(new ObjectMapper());

  private static final ObjectMapper yamlMapper =
      configure(new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES)));

  public static ObjectMapper jsonMapper() {
    return jsonMapper;
  }

  public static ObjectMapper yamlMapper() {
    return yamlMapper;
  }

  private static ObjectMapper configure(ObjectMapper mapper) {
    SimpleModule validationModule = new SimpleModule();
    validationModule.addDeserializer(URI.class, new URIDeserializer());
    validationModule.addSerializer(URI.class, new URISerializer());
    validationModule.setDeserializerModifier(new BeanDeserializerModifierWithValidation());

    return mapper
        .setDefaultPropertyInclusion(Include.NON_NULL)
        .configure(SerializationFeature.INDENT_OUTPUT, true)
        .configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false)
        .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
        .registerModule(validationModule)
        .registerModule(new JacksonMixInModule());
  }

  private ObjectMapperFactory() {}
}
