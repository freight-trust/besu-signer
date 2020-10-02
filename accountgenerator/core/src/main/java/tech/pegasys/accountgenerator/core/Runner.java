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
package tech.pegasys.accountgenerator.core;

import tech.pegasys.accountgenerator.core.http.HttpResponseFactory;
import tech.pegasys.accountgenerator.core.http.HttpServerService;
import tech.pegasys.accountgenerator.core.http.JsonRpcErrorHandler;
import tech.pegasys.accountgenerator.core.http.JsonRpcHandler;
import tech.pegasys.accountgenerator.core.http.LogErrorHandler;
import tech.pegasys.accountgenerator.core.http.RequestMapper;
import tech.pegasys.accountgenerator.core.http.UpcheckHandler;
import tech.pegasys.accountgenerator.core.jsonrpc.JsonDecoder;
import tech.pegasys.accountgenerator.core.requesthandler.DefaultHandler;
import tech.pegasys.accountgenerator.core.requesthandler.GenerateAccountHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Properties;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Runner {

  private static final Logger LOG = LogManager.getLogger();
  private static final String JSON = HttpHeaderValues.APPLICATION_JSON.toString();
  private static final String TEXT = HttpHeaderValues.TEXT_PLAIN.toString() + "; charset=utf-8";

  private final KeyGeneratorProvider keyGeneratorProvider;
  private final HttpResponseFactory responseFactory = new HttpResponseFactory();
  private final JsonDecoder jsonDecoder;
  private final Path dataPath;
  private final Vertx vertx;
  private final HttpServerService httpServerService;

  public Runner(
      final KeyGeneratorProvider keyGeneratorProvider,
      final HttpServerOptions serverOptions,
      final JsonDecoder jsonDecoder,
      final Path dataPath,
      final Vertx vertx) {
    this.keyGeneratorProvider = keyGeneratorProvider;
    this.jsonDecoder = jsonDecoder;
    this.dataPath = dataPath;
    this.vertx = vertx;
    this.httpServerService = new HttpServerService(router(), serverOptions);
  }

  public void start() {
    vertx.deployVerticle(httpServerService, this::httpServerServiceDeployment);
  }

  private Router router() {

    final Router router = Router.router(vertx);

    // Handler for JSON-RPC requests
    router
        .route(HttpMethod.POST, "/")
        .produces(JSON)
        .handler(BodyHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new JsonRpcErrorHandler(new HttpResponseFactory(), jsonDecoder))
        .blockingHandler(new JsonRpcHandler(responseFactory, createRequestMapper(), jsonDecoder));

    // Handler for UpCheck endpoint
    router
        .route(HttpMethod.GET, "/upcheck")
        .produces(TEXT)
        .handler(BodyHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new LogErrorHandler())
        .handler(new UpcheckHandler());

    return router;
  }

  private RequestMapper createRequestMapper() {

    final RequestMapper requestMapper = new RequestMapper(new DefaultHandler());

    if (keyGeneratorProvider != null) {
      final GenerateAccountHandler generateAccountHandler =
          new GenerateAccountHandler(keyGeneratorProvider, responseFactory);
      requestMapper.addHandler("eth_generateAccount", generateAccountHandler);
    }
    return requestMapper;
  }

  private void httpServerServiceDeployment(final AsyncResult<String> result) {
    if (result.succeeded()) {
      LOG.info("JsonRpcHttpService Vertx deployment id is: {}", result.result());

      if (dataPath != null) {
        writePortsToFile(httpServerService);
      }
    } else {
      deploymentFailed(result.cause());
    }
  }

  private void deploymentFailed(final Throwable cause) {
    LOG.error("Vertx deployment failed", cause);
    vertx.close();
    System.exit(1);
  }

  private void writePortsToFile(final HttpServerService httpService) {
    final File portsFile = new File(dataPath.toFile(), "accountgenerator.ports");
    portsFile.deleteOnExit();

    final Properties properties = new Properties();
    properties.setProperty("http-jsonrpc", String.valueOf(httpService.actualPort()));

    LOG.info(
        "Writing accountgenerator.ports file: {}, with contents: {}",
        portsFile.getAbsolutePath(),
        properties);
    try (final FileOutputStream fileOutputStream = new FileOutputStream(portsFile)) {
      properties.store(
          fileOutputStream,
          "This file contains the ports used by the running instance of Web3Provider. This file will be deleted after the node is shutdown.");
    } catch (final Exception e) {
      LOG.warn("Error writing ports file", e);
    }
  }
}
