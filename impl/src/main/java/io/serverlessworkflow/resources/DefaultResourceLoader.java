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
package io.serverlessworkflow.resources;

import io.serverlessworkflow.api.types.Endpoint;
import io.serverlessworkflow.api.types.EndpointUri;
import io.serverlessworkflow.api.types.ExternalResource;
import io.serverlessworkflow.api.types.UriTemplate;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

public class DefaultResourceLoader implements ResourceLoader {

  private final Optional<Path> workflowPath;

  protected DefaultResourceLoader(Path workflowPath) {
    this.workflowPath = Optional.ofNullable(workflowPath);
  }

  @Override
  public StaticResource loadStatic(ExternalResource resource) {
    return processEndpoint(resource.getEndpoint());
  }

  @Override
  public DynamicResource loadDynamic(
      WorkflowContext workflow, ExternalResource resource, ExpressionFactory factory) {
    throw new UnsupportedOperationException("Dynamic loading of resources is not suppported");
  }

  private StaticResource buildFromString(String uri) {
    return fileResource(uri);
  }

  private StaticResource fileResource(String pathStr) {
    Path path = Path.of(pathStr);
    if (path.isAbsolute()) {
      return new FileResource(path);
    } else {
      return workflowPath
          .<StaticResource>map(p -> new FileResource(p.resolve(path)))
          .orElseGet(() -> new ClasspathResource(pathStr));
    }
  }

  private StaticResource buildFromURI(URI uri) {
    String scheme = uri.getScheme();
    if (scheme == null || scheme.equalsIgnoreCase("file")) {
      return fileResource(uri.getPath());
    } else if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
      try {
        return new HttpResource(uri.toURL());
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException(e);
      }
    } else {
      throw new UnsupportedOperationException("Unsupported scheme " + scheme);
    }
  }

  private StaticResource processEndpoint(Endpoint endpoint) {
    if (endpoint.getEndpointConfiguration() != null) {
      EndpointUri uri = endpoint.getEndpointConfiguration().getUri();
      if (uri.getLiteralEndpointURI() != null) {
        return getURI(uri.getLiteralEndpointURI());
      } else if (uri.getExpressionEndpointURI() != null) {
        throw new UnsupportedOperationException(
            "Expression not supported for loading a static resource");
      }
    } else if (endpoint.getRuntimeExpression() != null) {
      throw new UnsupportedOperationException(
          "Expression not supported for loading a static resource");
    } else if (endpoint.getUriTemplate() != null) {
      return getURI(endpoint.getUriTemplate());
    }
    throw new IllegalArgumentException("Invalid endpoint definition " + endpoint);
  }

  private StaticResource getURI(UriTemplate template) {
    if (template.getLiteralUri() != null) {
      return buildFromURI(template.getLiteralUri());
    } else if (template.getLiteralUriTemplate() != null) {
      return buildFromString(template.getLiteralUriTemplate());
    } else {
      throw new IllegalStateException("Invalid endpoint definition" + template);
    }
  }
}
