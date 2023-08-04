/*
 * Copyright (C) 2011-2023 Thomas Akehurst
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
package com.github.tomakehurst.wiremock.http;

import static com.github.tomakehurst.wiremock.http.Response.response;
import static com.google.common.base.MoreObjects.firstNonNull;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.InputStreamSource;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.store.BlobStore;
import com.github.tomakehurst.wiremock.store.SettingsStore;
import com.github.tomakehurst.wiremock.store.files.BlobStoreFileSource;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.collect.Maps;

import javax.script.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class StubResponseRenderer implements ResponseRenderer {

  private final BlobStore filesBlobStore;
  private final FileSource filesFileSource;
  private final SettingsStore settingsStore;
  private final ProxyResponseRenderer proxyResponseRenderer;
  private final List<ResponseTransformer> responseTransformers;

  private Map<String, String> responseScriptNameVsContent;

  public StubResponseRenderer(
      BlobStore filesBlobStore,
      SettingsStore settingsStore,
      ProxyResponseRenderer proxyResponseRenderer,
      List<ResponseTransformer> responseTransformers) {
    this.filesBlobStore = filesBlobStore;
    this.settingsStore = settingsStore;
    this.proxyResponseRenderer = proxyResponseRenderer;
    this.responseTransformers = responseTransformers;

    filesFileSource = new BlobStoreFileSource(filesBlobStore);
    responseScriptNameVsContent = Maps.newHashMap();
  }

  @Override
  public Response render(ServeEvent serveEvent) {
    ResponseDefinition responseDefinition = serveEvent.getResponseDefinition();
    if (!responseDefinition.wasConfigured()) {
      return Response.notConfigured();
    }

    Response response = buildResponse(serveEvent);
    return applyTransformations(
        responseDefinition.getOriginalRequest(),
        responseDefinition,
        response,
        responseTransformers);
  }

  private Response buildResponse(ServeEvent serveEvent) {
    if (serveEvent.getResponseDefinition().isProxyResponse()) {
      return proxyResponseRenderer.render(serveEvent);
    } else {
      Response.Builder responseBuilder = renderDirectly(serveEvent);
      return responseBuilder.build();
    }
  }

  private Response applyTransformations(
      Request request,
      ResponseDefinition responseDefinition,
      Response response,
      List<ResponseTransformer> transformers) {
    if (transformers.isEmpty()) {
      return response;
    }

    ResponseTransformer transformer = transformers.get(0);
    Response newResponse =
        transformer.applyGlobally() || responseDefinition.hasTransformer(transformer)
            ? transformer.transform(
                request, response, filesFileSource, responseDefinition.getTransformerParameters())
            : response;

    return applyTransformations(
        request, responseDefinition, newResponse, transformers.subList(1, transformers.size()));
  }

  private Response.Builder renderDirectly(ServeEvent serveEvent) {
    ResponseDefinition responseDefinition = serveEvent.getResponseDefinition();

    HttpHeaders headers = responseDefinition.getHeaders();
    StubMapping stubMapping = serveEvent.getStubMapping();
    if (serveEvent.getWasMatched() && stubMapping != null) {
      headers =
          firstNonNull(headers, new HttpHeaders())
              .plus(new HttpHeader("Matched-Stub-Id", stubMapping.getId().toString()));

      if (stubMapping.getName() != null) {
        headers = headers.plus(new HttpHeader("Matched-Stub-Name", stubMapping.getName()));
      }
    }

    GlobalSettings settings = settingsStore.get();
    Response.Builder responseBuilder =
        response()
            .status(responseDefinition.getStatus())
            .statusMessage(responseDefinition.getStatusMessage())
            .headers(headers)
            .fault(responseDefinition.getFault())
            .configureDelay(
                settings.getFixedDelay(),
                settings.getDelayDistribution(),
                responseDefinition.getFixedDelayMilliseconds(),
                responseDefinition.getDelayDistribution())
            .chunkedDribbleDelay(responseDefinition.getChunkedDribbleDelay());

    if (responseDefinition.specifiesBodyFile()) {
      if (responseDefinition.isResponseBodyAScript()){
        executeScriptAndGenerateResponse(responseDefinition, responseBuilder);
      }else {
        final InputStreamSource bodyStreamSource =
                filesBlobStore.getStreamSource(responseDefinition.getBodyFileName());
        responseBuilder.body(bodyStreamSource);
      }
    } else if (responseDefinition.specifiesBodyContent()) {
      if (responseDefinition.specifiesBinaryBodyContent()) {
        responseBuilder.body(responseDefinition.getByteBody());
      } else {
        responseBuilder.body(responseDefinition.getByteBody());
      }
    }

    return responseBuilder;
  }

  private void executeScriptAndGenerateResponse(ResponseDefinition responseDefinition, Response.Builder responseBuilder) {
    //start script engine
    ScriptEngineManager engineManager = new ScriptEngineManager();
    ScriptEngine engine = engineManager.getEngineByName("nashorn");
    try {
      addRequestToScriptContext(responseDefinition, engine);
      // Evaluate the JavaScript code
      var response = executeScript(responseDefinition, engine);
      responseBuilder.body(response.toString());
    } catch (ScriptException | IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  private Object executeScript(ResponseDefinition responseDefinition, ScriptEngine engine) throws ScriptException, IOException {
    if (responseScriptNameVsContent.containsKey(responseDefinition.getBodyFileName())){
      return engine.eval(responseScriptNameVsContent.get(responseDefinition.getBodyFileName()));
    } else {
      String script = loadScript(responseDefinition);
      return engine.eval(script);
    }
  }

  private static void addRequestToScriptContext(ResponseDefinition responseDefinition, ScriptEngine engine) {
    Bindings bindings = engine.createBindings();
    bindings.put("request", responseDefinition.getOriginalRequest());
    engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
  }

  private String loadScript(ResponseDefinition responseDefinition) throws IOException {
    InputStreamSource bodyStreamSource =
            filesBlobStore.getStreamSource(responseDefinition.getBodyFileName());
    String script = new String(bodyStreamSource.getStream().readAllBytes());
    responseScriptNameVsContent.put(responseDefinition.getBodyFileName(), script);
    return script;
  }
}
