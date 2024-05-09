/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.service.exceptions.*;
import com.adobe.campaign.tests.bridge.service.utils.ServiceTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class IntegroAPI {
    public static final String ERROR_CALL_TIMEOUT = "The call you made exceeds the set timeout limit.";
    public static final String ERROR_CONTENT_TYPE = "application/problem+json";
    public static final String SYSTEM_UP_MESSAGE = "All systems up";
    public static final String ERROR_IBS_INTERNAL = "Internal IBS error. Please file a bug report with the project and provide this JSON in the report.";
    public static final String ERROR_PAYLOAD_INCONSISTENCY = "We detected an inconsistency in your payload.";
    public static final String UPLOADED_FILE_REF = "uploaded_file";
    public static final String JAVA_CALL_REF = "call_part";
    protected static final String ERROR_JSON_TRANSFORMATION = "JSON Transformation issue : Problem processing request. The given json could not be mapped to a Java Call";
    protected static final String ERROR_CALLING_JAVA_METHOD = "Error during call of target Java Class and Method.";
    protected static final String ERROR_JAVA_OBJECT_NOT_FOUND = "Could not find the given class or method.";
    protected static final String ERROR_IBS_CONFIG = "The provided class and method for setting environment variables is not valid.";
    protected static final String ERROR_IBS_RUNTIME = "Problems with payload.";
    protected static final String ERROR_AMBIGUOUS_METHOD = "No unique method could be identified that matches your request.";
    protected static final String ERROR_JAVA_OBJECT_NOT_ACCESSIBLE = "The java object you want to call is inaccessible. This is very possibly a scope problem.";
    private static final Logger log = LogManager.getLogger();
    public static final String ERROR_BAD_MULTI_PART_REQUEST = "When sending a multi-part request, you need to at least have a payload for the callContent.";
    public static final String STD_UPLOAD_DIR = "upload";

    public static void startServices(int port) {

        if (!ServiceTools.isPortFree(port)) {
            throw new IBSConfigurationException("The port " + port + " is not currently free.");
        }

        if (Boolean.parseBoolean(ConfigValueHandlerIBS.SSL_ACTIVE.fetchValue())) {
            File l_file = new File(ConfigValueHandlerIBS.SSL_KEYSTORE_PATH.fetchValue());
            if (!l_file.exists()) {
                log.error("Could not find the Keystore file path {}", l_file.getAbsolutePath());
            }
            secure(ConfigValueHandlerIBS.SSL_KEYSTORE_PATH.fetchValue(),
                    ConfigValueHandlerIBS.SSL_KEYSTORE_PASSWORD.fetchValue(),
                    ConfigValueHandlerIBS.SSL_TRUSTSTORE_PATH.fetchValue(),
                    ConfigValueHandlerIBS.SSL_TRUSTSTORE_PASSWORD.fetchValue());
        } else {
            port(port);
        }

        get("/test", (req, res) -> {
            res.type("application/json");
            Map<String, String> status = new HashMap<>();
            status.put("overALLSystemState", SYSTEM_UP_MESSAGE);
            status.put("deploymentMode", ConfigValueHandlerIBS.DEPLOYMENT_MODEL.fetchValue());
            status.put("bridgeServiceVersion", ConfigValueHandlerIBS.PRODUCT_VERSION.fetchValue());

            if (ConfigValueHandlerIBS.PRODUCT_USER_VERSION.isSet()) {
                status.put("hostVersion", ConfigValueHandlerIBS.PRODUCT_USER_VERSION.fetchValue());
            }

            return BridgeServiceFactory.transformMapTosResult(status);
        });

        post("/service-check", (req, res) -> {
            ServiceAccess l_serviceAccess = BridgeServiceFactory.createServiceAccess(req.body());

            return BridgeServiceFactory.transformServiceAccessResult(
                    l_serviceAccess.checkAccessibilityOfExternalResources());
        });

        File uploadDir = new File(STD_UPLOAD_DIR);
        uploadDir.mkdir(); // create the upload directory if it doesn't exist
        //staticFiles.externalLocation("upload");

        post("/call", (req, res) -> {

            boolean isMultiPart = false;
            JavaCalls fetchedFromJSON;

            //Extract multipart information
            if (req.contentType() != null && req.contentType().toLowerCase().startsWith("multipart/form-data")) {
                req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("./temp"));
                Map<String, Path> fileRefs = new HashMap<>();
                isMultiPart = true;
                //Extract file information
                for (Part p : req.raw().getParts().stream().filter(p -> p.getSubmittedFileName() != null)
                        .collect(Collectors.toList())) {

                    Path tempFile = Files.createTempFile(uploadDir.toPath(), "", "");
                    ThreadContext.put(p.getName(), tempFile.getFileName().toString());
                    fileRefs.put(p.getName(), tempFile);

                    try (InputStream is = p.getInputStream()) {
                        // https://github.com/tipsy/spark-file-upload/blob/master/src/main/java/UploadExample.java
                        Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                ThreadContext.put(UPLOADED_FILE_REF, String.join(",",
                        fileRefs.values().stream().map(p -> p.getFileName().toString()).collect(Collectors.toList())));

                List<Part> l_parts = req.raw().getParts().stream().filter(t -> t.getSubmittedFileName() == null)
                        .collect(Collectors.toList());

                if (l_parts.size() != 1) {
                    throw new IBSPayloadException(
                            ERROR_BAD_MULTI_PART_REQUEST);
                }

                fetchedFromJSON = BridgeServiceFactory.createJavaCalls(
                        new String(l_parts.get(0).getInputStream().readAllBytes(), StandardCharsets.UTF_8));

                //Store file in context
                fileRefs.forEach((k, v) -> fetchedFromJSON.getLocalClassLoader().getCallResultCache().put(k, v.toFile()));

            } else {
                fetchedFromJSON = BridgeServiceFactory.createJavaCalls(req.body());
            }


            fetchedFromJSON.addHeaders(req.headers().stream().collect(Collectors.toMap(k -> k, req::headers)));

            return BridgeServiceFactory.transformJavaCallResultsToJSON(fetchedFromJSON.submitCalls(),
                    fetchedFromJSON.fetchSecrets());
        });

        after((req, res) -> {
            res.type("application/json");
        });

        exception(JsonProcessingException.class, (e, req, res) -> {
            int statusCode = 404;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);
            res.body(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_JSON_TRANSFORMATION, statusCode)));
        });

        exception(IBSPayloadException.class, (e, req, res) -> {
            int statusCode = 404;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);
            res.body(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_PAYLOAD_INCONSISTENCY, statusCode)));
        });

        exception(AmbiguousMethodException.class, (e, req, res) -> {
            int statusCode = 404;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);
            res.body(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_AMBIGUOUS_METHOD, statusCode, false)));
        });

        exception(IBSConfigurationException.class, (e, req, res) -> {
            int statusCode = 500;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);
            res.body(BridgeServiceFactory.createExceptionPayLoad(new ErrorObject(e, ERROR_IBS_CONFIG, statusCode)));
        });

        exception(IBSRunTimeException.class, (e, req, res) -> {
            int statusCode = 500;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);
            res.body(BridgeServiceFactory.createExceptionPayLoad(new ErrorObject(e, ERROR_IBS_RUNTIME, statusCode)));
        });

        exception(TargetJavaMethodCallException.class, (e, req, res) -> {
            int statusCode = 500;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);
            res.body(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_CALLING_JAVA_METHOD, statusCode)));
        });

        exception(NonExistentJavaObjectException.class, (e, req, res) -> {
            int statusCode = 404;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);
            res.body(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_JAVA_OBJECT_NOT_FOUND, statusCode, false)));
        });

        exception(JavaObjectInaccessibleException.class, (e, req, res) -> {
            int statusCode = 404;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);
            res.body(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_JAVA_OBJECT_NOT_ACCESSIBLE, statusCode, false)));
        });

        exception(IBSTimeOutException.class, (e, req, res) -> {
            int statusCode = 408;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);
            res.body(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_CALL_TIMEOUT, statusCode, false)));
        });

        //Internal exception
        exception(Exception.class, (e, req, res) -> {
            int statusCode = 500;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);
            res.body(BridgeServiceFactory.createExceptionPayLoad(new ErrorObject(e, ERROR_IBS_INTERNAL, statusCode)));
        });

        afterAfter((req, res) -> {
            if (ThreadContext.containsKey(UPLOADED_FILE_REF)) {
                Arrays.stream(ThreadContext.get(UPLOADED_FILE_REF).split(",")).forEach(f -> {
                    log.debug("Cleaning up file {}. succeeded {}.", f, (new File(uploadDir.getName(), f)).delete());
                });

            }
        });
    }

    protected enum DeploymentMode {
        TEST, PRODUCTION
    }

}
