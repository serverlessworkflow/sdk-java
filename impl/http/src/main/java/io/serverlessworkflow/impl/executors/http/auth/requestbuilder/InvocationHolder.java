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
package io.serverlessworkflow.impl.executors.http.auth.requestbuilder;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import java.io.Closeable;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

class InvocationHolder implements Callable<Response>, Closeable {

  private final Client client;
  private final Supplier<Response> call;

  InvocationHolder(Client client, Supplier<Response> call) {
    this.client = client;
    this.call = call;
  }

  public Response call() {
    return call.get();
  }

  public void close() {
    if (client != null) {
      client.close();
    }
  }
}
