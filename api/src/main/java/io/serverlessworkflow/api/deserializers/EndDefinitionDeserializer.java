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
package io.serverlessworkflow.api.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.serverlessworkflow.api.end.ContinueAs;
import io.serverlessworkflow.api.end.End;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import io.serverlessworkflow.api.produce.ProduceEvent;
import io.serverlessworkflow.api.start.Start;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EndDefinitionDeserializer extends StdDeserializer<End> {

  private static final long serialVersionUID = 510l;

  @SuppressWarnings("unused")
  private WorkflowPropertySource context;

  public EndDefinitionDeserializer() {
    this(End.class);
  }

  public EndDefinitionDeserializer(Class<?> vc) {
    super(vc);
  }

  public EndDefinitionDeserializer(WorkflowPropertySource context) {
    this(Start.class);
    this.context = context;
  }

  @Override
  public End deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    JsonNode node = jp.getCodec().readTree(jp);

    End end = new End();

    if (node.isBoolean()) {
      end.setProduceEvents(null);
      end.setCompensate(false);
      end.setTerminate(false);
      end.setContinueAs(null);
      return node.asBoolean() ? end : null;
    } else {
      if (node.get("produceEvents") != null) {
        List<ProduceEvent> produceEvents = new ArrayList<>();
        for (final JsonNode nodeEle : node.get("produceEvents")) {
          produceEvents.add(mapper.treeToValue(nodeEle, ProduceEvent.class));
        }
        end.setProduceEvents(produceEvents);
      }

      if (node.get("terminate") != null) {
        end.setTerminate(node.get("terminate").asBoolean());
      } else {
        end.setTerminate(false);
      }

      if (node.get("compensate") != null) {
        end.setCompensate(node.get("compensate").asBoolean());
      } else {
        end.setCompensate(false);
      }

      if (node.get("continueAs") != null) {
        end.setContinueAs(mapper.treeToValue(node.get("continueAs"), ContinueAs.class));
      }

      return end;
    }
  }
}
