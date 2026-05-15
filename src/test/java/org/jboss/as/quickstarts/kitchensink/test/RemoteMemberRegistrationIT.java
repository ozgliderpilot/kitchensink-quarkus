/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.kitchensink.test;

import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import jakarta.ws.rs.core.Response;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class RemoteMemberRegistrationIT {

    public static final String JOHN_ID = "6652c45dc4b9f77f03cc3c9f";
    public static final String UNKNOWN_ID = "5652c45dc4b9f77f03cc3c9e";

    @Inject
    Logger log;

    protected URI getHTTPEndpoint() {
        String host = getServerHost();
        if (host == null) {
            host = "http://localhost:8081";
        }
        try {
            return new URI(host + "/rest/members");
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getServerHost() {
        String host = System.getenv("SERVER_HOST");
        if (host == null) {
            host = System.getProperty("server.host");
        }
        return host;
    }

    @Test
    public void testRegister() throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("name", "Jane Doe Jr")
                .add("email", "jane_Jr@mailinator.com")
                .add("phoneNumber", "2125551235").build();
        HttpRequest request = HttpRequest.newBuilder(getHTTPEndpoint())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        HttpResponse response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("", response.body().toString() );
    }

    @Test
    public void testList() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/rest/members")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("size()", greaterThan(0));
    }

    @Test
    public void testGet() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/rest/members/"+ JOHN_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("name", equalTo("John Smith"));
    }

    @Test
    public void testNotFoundMember() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/rest/members/" + UNKNOWN_ID)
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode())
                .body(blankOrNullString());
    }

    @Test
    public void testDuplicatedEmail() {
        Member member = new Member();
        member.setName("Jane Doe Jr");
        member.setEmail("jane_Jr@mailinator.com");
        member.setPhoneNumber("2125551235");
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(member)
                .when()
                .post("/rest/members")
                .then()
                .statusCode(Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    public void testMalformedRequest() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("bad request")
                .when().post("/rest/members")
                .then()
                    .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testInvalidPhoneNumber() {
        Member member = new Member();
        member.setName("Jane Doe Jr");
        member.setEmail(UUID.randomUUID() + "@mailinator.com");
        member.setPhoneNumber("1234");
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(member)
                .when()
                .post("/rest/members")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("phoneNumber", equalTo("size must be between 10 and 12"));
    }

    @Test
    public void testNameWithNumbers() {
        Member member = new Member();
        member.setName("Jane Doe Jr4");
        member.setEmail(UUID.randomUUID() + "@mailinator.com");
        member.setPhoneNumber("1234567890");
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(member)
                .when()
                .post("/rest/members")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("name", equalTo("Must not contain numbers"));
    }

    @Test
    public void testTooLongName() {
        Member member = new Member();
        member.setName("Vitaliy Vladislavovich Baschlykoff");
        member.setEmail(UUID.randomUUID() + "@mailinator.com");
        member.setPhoneNumber("1234567890");
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(member)
                .when()
                .post("/rest/members")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("name", equalTo("size must be between 1 and 25"));
    }

    @Test
    public void testLongNameAndInvalidEmail() {
        Member member = new Member();
        member.setName("Vitaliy Vladislavovich Baschlykoff");
        member.setEmail(UUID.randomUUID() + "mailinator.com");
        member.setPhoneNumber("1234567890");
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(member)
                .when()
                .post("/rest/members")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("name", equalTo("size must be between 1 and 25"));
    }

}
