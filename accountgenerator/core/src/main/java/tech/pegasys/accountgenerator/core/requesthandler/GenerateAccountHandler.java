/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.accountgenerator.core.requesthandler;

import tech.pegasys.accountgenerator.core.KeyGenerator;
import tech.pegasys.accountgenerator.core.KeyGeneratorProvider;
import tech.pegasys.accountgenerator.core.http.HttpResponseFactory;
import tech.pegasys.accountgenerator.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.accountgenerator.core.jsonrpc.response.JsonRpcError;
import tech.pegasys.accountgenerator.core.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.accountgenerator.core.jsonrpc.response.JsonRpcSuccessResponse;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GenerateAccountHandler implements JsonRpcRequestHandler {

  private static final Logger LOG = LogManager.getLogger();

  private final KeyGeneratorProvider keyGeneratorProvider;
  private final HttpResponseFactory responder;

  public GenerateAccountHandler(
      final KeyGeneratorProvider keyGeneratorProvider, final HttpResponseFactory responder) {
    this.keyGeneratorProvider = keyGeneratorProvider;
    this.responder = responder;
  }

  @Override
  public void handle(final RoutingContext context, final JsonRpcRequest request) {
    LOG.debug("Generating account request {}, {}", request.getId(), request.getMethod());
    final Path directory = keyGeneratorProvider.getDirectory();
    final KeyGenerator generator = keyGeneratorProvider.getGenerator();
    final String address;
    try {
      address = generator.generate();
    } catch (Exception ex) {
      LOG.trace(ex);
      responder.create(
          context.request(),
          HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
          new JsonRpcErrorResponse(JsonRpcError.FAILED_TO_GENERATE_ACCOUNT));
      return;
    }
    if (address == null) {
      responder.create(
          context.request(),
          HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
          new JsonRpcErrorResponse(JsonRpcError.FAILED_TO_GENERATE_ACCOUNT));
      return;
    }
    try {
      final String metaData = generator.metaData(address);
      final String toml = address.substring(2).toLowerCase();
      final Path tomlFile = directory.resolve(String.format("%s.toml", toml));
      Files.write(Files.createFile(tomlFile), metaData.getBytes(Charset.defaultCharset()));
      final JsonRpcSuccessResponse response = new JsonRpcSuccessResponse(request.getId(), address);
      responder.create(context.request(), HttpResponseStatus.OK.code(), response);
    } catch (Exception ex) {
      LOG.trace(ex);
      responder.create(
          context.request(),
          HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
          new JsonRpcErrorResponse(JsonRpcError.FAILED_TO_GENERATE_TOML_FILE));
    }
  }
}
