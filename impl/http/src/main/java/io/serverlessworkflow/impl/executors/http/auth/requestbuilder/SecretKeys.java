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

class SecretKeys {

  private SecretKeys() {}

  static final String GRANT = "grant";
  static final String USER = "username";
  static final String CLIENT = "client";
  static final String PASSWORD = "password";
  static final String ID = "id";
  static final String SECRET = "secret";
  static final String ISSUERS = "issuers";
  static final String AUDIENCES = "audiences";
  static final String ENDPOINTS = "endpoints";
  static final String TOKEN = "token";
  static final String AUTHORITY = "authority";
  static final String SCOPES = "scopes";
  static final String REQUEST = "request";
  static final String ENCODING = "encoding";
  static final String AUTHENTICATION = "authentication";
}
