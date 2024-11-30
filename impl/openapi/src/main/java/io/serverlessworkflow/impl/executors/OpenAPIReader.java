package io.serverlessworkflow.impl.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Map;

public class OpenAPIReader {

    public static String getHost(JsonNode jsonNode) {
        JsonNode host = jsonNode.get("host");
        if (host == null) {
            return null;
        }
        String scheme = getScheme(jsonNode);
        return scheme + "://" + host.asText();
    }

    private static String getScheme(JsonNode jsonNode) {
        ArrayNode array = jsonNode.withArrayProperty("schemes");
        if (array != null && !array.isEmpty()) {
            // TODO: should get the first scheme?
            return array.get(0).asText();
        }
        // TODO: should the http be the default scheme?
        return "http";
    }

    public static JsonNode readOperation(JsonNode jsonNode, String operationId) {
        JsonNode paths = jsonNode.get("paths");
        for (Map.Entry<String, JsonNode> entry : paths.properties()) {
            for (Map.Entry<String, JsonNode> httpMethod : entry.getValue().properties()) {
                if (httpMethod.getValue().get("operationId").asText().equals(operationId)) {
                    return httpMethod.getValue();
                }
            }
        }

        return null;
    }
}
