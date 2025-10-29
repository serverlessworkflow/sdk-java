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
package io.serverlessworkflow.impl.resources;

import io.serverlessworkflow.api.types.Endpoint;
import io.serverlessworkflow.api.types.EndpointUri;
import io.serverlessworkflow.api.types.ExternalResource;
import io.serverlessworkflow.api.types.UriTemplate;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.expressions.ExpressionDescriptor;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class DefaultResourceLoader implements ResourceLoader {

  private final Optional<Path> workflowPath;
  private final WorkflowApplication application;

  private final AtomicReference<URITemplateResolver> templateResolver =
      new AtomicReference<URITemplateResolver>();

  private Map<ExternalResourceHandler, CachedResource> resourceCache = new ConcurrentHashMap<>();

  protected DefaultResourceLoader(WorkflowApplication application, Path workflowPath) {
    this.application = application;
    this.workflowPath = Optional.ofNullable(workflowPath);
  }

  private URITemplateResolver templateResolver() {
    URITemplateResolver result = templateResolver.get();
    if (result == null) {
      result =
          ServiceLoader.load(URITemplateResolver.class)
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Need an uri template resolver to resolve uri template"));
      templateResolver.set(result);
    }
    return result;
  }

  private ExternalResourceHandler fileResource(String pathStr) {
    Path path = Path.of(pathStr);
    if (path.isAbsolute()) {
      return new FileResource(path);
    } else {
      return workflowPath
          .<ExternalResourceHandler>map(p -> new FileResource(p.resolve(path)))
          .orElseGet(() -> new ClasspathResource(pathStr));
    }
  }

  private ExternalResourceHandler buildFromURI(URI uri) {
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

  @Override
  public <T> T load(
      ExternalResource resource,
      Function<ExternalResourceHandler, T> function,
      WorkflowContext workflowContext,
      TaskContext taskContext,
      WorkflowModel model) {
    ExternalResourceHandler resourceHandler =
        buildFromURI(
            uriSupplier(resource.getEndpoint())
                .apply(
                    workflowContext,
                    taskContext,
                    model == null ? application.modelFactory().fromNull() : model));
    return (T)
        resourceCache
            .compute(
                resourceHandler,
                (k, v) ->
                    v == null || k.shouldReload(v.lastReload())
                        ? new CachedResource(Instant.now(), function.apply(k))
                        : v)
            .content();
  }

  @Override
  public WorkflowValueResolver<URI> uriSupplier(Endpoint endpoint) {
    if (endpoint.getEndpointConfiguration() != null) {
      EndpointUri uri = endpoint.getEndpointConfiguration().getUri();
      if (uri.getLiteralEndpointURI() != null) {
        return getURISupplier(uri.getLiteralEndpointURI());
      } else if (uri.getExpressionEndpointURI() != null) {
        return new ExpressionURISupplier(
            application
                .expressionFactory()
                .resolveString(ExpressionDescriptor.from(uri.getExpressionEndpointURI())));
      }
    } else if (endpoint.getRuntimeExpression() != null) {
      return new ExpressionURISupplier(
          application
              .expressionFactory()
              .resolveString(ExpressionDescriptor.from(endpoint.getRuntimeExpression())));
    } else if (endpoint.getUriTemplate() != null) {
      return getURISupplier(endpoint.getUriTemplate());
    }
    throw new IllegalArgumentException("Invalid endpoint definition " + endpoint);
  }

  private WorkflowValueResolver<URI> getURISupplier(UriTemplate template) {
    if (template.getLiteralUri() != null) {
      return (w, t, n) -> template.getLiteralUri();
    } else if (template.getLiteralUriTemplate() != null) {
      return (w, t, n) ->
          templateResolver().resolveTemplates(template.getLiteralUriTemplate(), w, t, n);
    }
    throw new IllegalArgumentException("Invalid uritemplate definition " + template);
  }

  private class ExpressionURISupplier implements WorkflowValueResolver<URI> {
    private WorkflowValueResolver<String> expr;

    public ExpressionURISupplier(WorkflowValueResolver<String> expr) {
      this.expr = expr;
    }

    @Override
    public URI apply(WorkflowContext workflow, TaskContext task, WorkflowModel node) {
      return URI.create(expr.apply(workflow, task, node));
    }
  }

  public void close() {
    resourceCache.clear();
  }
}
