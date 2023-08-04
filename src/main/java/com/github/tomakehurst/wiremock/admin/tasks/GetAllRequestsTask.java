/*
 * Copyright (C) 2016-2023 Thomas Akehurst
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
package com.github.tomakehurst.wiremock.admin.tasks;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.jsonResponse;
import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;

import com.github.tomakehurst.wiremock.admin.AdminTask;
import com.github.tomakehurst.wiremock.admin.LimitAndSinceDatePaginator;
import com.github.tomakehurst.wiremock.admin.model.GetServeEventsResult;
import com.github.tomakehurst.wiremock.admin.model.ServeEventQuery;
import com.github.tomakehurst.wiremock.common.InvalidInputException;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.common.url.PathParams;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

public class GetAllRequestsTask implements AdminTask {

  @Override
  public ResponseDefinition execute(Admin admin, ServeEvent serveEvent, PathParams pathParams) {
    ServeEventQuery query = ServeEventQuery.fromRequest(serveEvent.getRequest());
    GetServeEventsResult serveEventsResult = admin.getServeEvents(query);
    LimitAndSinceDatePaginator paginator;
    try {
      paginator =
          LimitAndSinceDatePaginator.fromRequest(
              serveEventsResult.getRequests(), serveEvent.getRequest());
    } catch (InvalidInputException e) {
      return jsonResponse(e.getErrors(), HTTP_BAD_REQUEST);
    }

    GetServeEventsResult result =
        new GetServeEventsResult(paginator, serveEventsResult.isRequestJournalDisabled());

    return responseDefinition()
        .withStatus(HTTP_OK)
        .withBody(Json.write(result))
        .withHeader("Content-Type", "application/json")
        .build();
  }
}
