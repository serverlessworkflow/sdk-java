package io.serverlessworkflow.resources;

import java.nio.file.Path;

public class DefaultResourceLoaderFactory implements ResourceLoaderFactory {

  public static final ResourceLoaderFactory get() {
    return factory;
  }

  private static final ResourceLoaderFactory factory = new DefaultResourceLoaderFactory();

  private DefaultResourceLoaderFactory() {}

  @Override
  public ResourceLoader getResourceLoader(Path path) {
    return new DefaultResourceLoader(path);
  }
}
