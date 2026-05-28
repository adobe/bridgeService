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
import io.javalin.Javalin;
import io.javalin.community.ssl.SslPlugin;
import io.javalin.config.MultipartConfig;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.Part;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.adobe.campaign.tests.bridge.service.BridgeServiceFactory.*;

public class IntegroAPI {
    public static final String ERROR_CONTENT_TYPE = "application/problem+json";
    public static final String SYSTEM_UP_MESSAGE = "All systems up";
    public static final String UPLOADED_FILE_REF = "uploaded_file";
    public static final String JAVA_CALL_REF = "call_part";
    public static final String ERROR_BAD_MULTI_PART_REQUEST = "When sending a multi-part request, you need to at least have a payload for the callContent.";
    public static final String STD_UPLOAD_DIR = "upload";
    private static final Logger log = LogManager.getLogger(IntegroAPI.class);

    public static Javalin startServices(int in_port) {

        if (!ServiceTools.isPortFree(in_port)) {
            throw new IBSConfigurationException("The port " + in_port + " is not currently free.");
        }

        IBSPluginManager.loadPlugins();

        File uploadDir = new File(STD_UPLOAD_DIR);
        uploadDir.mkdir();

        Javalin l_app = Javalin.create(config -> {
            config.jetty.multipartConfig = new MultipartConfig();
            if (Boolean.parseBoolean(ConfigValueHandlerIBS.SSL_ACTIVE.fetchValue())) {
                File l_keystoreFile = new File(ConfigValueHandlerIBS.SSL_KEYSTORE_PATH.fetchValue());
                if (!l_keystoreFile.exists()) {
                    log.error("Could not find the Keystore file path {}", l_keystoreFile.getAbsolutePath());
                }
                SslPlugin l_ssl = new SslPlugin(sslConfig -> {
                    sslConfig.keystoreFromPath(
                            ConfigValueHandlerIBS.SSL_KEYSTORE_PATH.fetchValue(),
                            ConfigValueHandlerIBS.SSL_KEYSTORE_PASSWORD.fetchValue());
                    sslConfig.redirect = true;
                });
                config.registerPlugin(l_ssl);
            }
        });

        l_app.get("/test", ctx -> {
            Map<String, String> l_status = new HashMap<>();
            l_status.put("overALLSystemState", SYSTEM_UP_MESSAGE);
            l_status.put("deploymentMode", ConfigValueHandlerIBS.DEPLOYMENT_MODEL.fetchValue());
            l_status.put("bridgeServiceVersion", ConfigValueHandlerIBS.PRODUCT_VERSION.fetchValue());

            if (ConfigValueHandlerIBS.PRODUCT_USER_VERSION.isSet()) {
                l_status.put("hostVersion", ConfigValueHandlerIBS.PRODUCT_USER_VERSION.fetchValue());
            }

            ctx.contentType("application/json");
            ctx.result(BridgeServiceFactory.transformMapTosResult(l_status));
        });

        l_app.post("/service-check", ctx -> {
            ServiceAccess l_serviceAccess = BridgeServiceFactory.createServiceAccess(ctx.body());
            ctx.contentType("application/json");
            ctx.result(BridgeServiceFactory.transformServiceAccessResult(
                    l_serviceAccess.checkAccessibilityOfExternalResources()));
        });

        l_app.post("/call", ctx -> {
            boolean l_isMultiPart = ctx.isMultipartFormData();
            JavaCalls l_fetchedFromJSON;

            if (l_isMultiPart) {
                ctx.req().setAttribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("./temp"));
                Map<String, Path> l_fileRefs = new HashMap<>();
                Collection<Part> l_parts = ctx.req().getParts();

                for (Part lt_part : l_parts.stream().filter(p -> p.getSubmittedFileName() != null)
                        .collect(Collectors.toList())) {
                    Path lt_tempFile = Files.createTempFile(uploadDir.toPath(), "", "");
                    ThreadContext.put(lt_part.getName(), lt_tempFile.getFileName().toString());
                    l_fileRefs.put(lt_part.getName(), lt_tempFile);
                    try (InputStream lt_is = lt_part.getInputStream()) {
                        Files.copy(lt_is, lt_tempFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                ThreadContext.put(UPLOADED_FILE_REF, String.join(",",
                        l_fileRefs.values().stream().map(p -> p.getFileName().toString()).collect(Collectors.toList())));

                List<Part> l_callParts = l_parts.stream()
                        .filter(p -> p.getSubmittedFileName() == null)
                        .collect(Collectors.toList());
                if (l_callParts.size() != 1) {
                    throw new IBSPayloadException(ERROR_BAD_MULTI_PART_REQUEST);
                }
                String l_callPayload;
                try (InputStream lt_is = l_callParts.get(0).getInputStream()) {
                    l_callPayload = new String(lt_is.readAllBytes(), StandardCharsets.UTF_8);
                }

                l_fetchedFromJSON = BridgeServiceFactory.createJavaCalls(l_callPayload);
                l_fileRefs.forEach((k, v) -> l_fetchedFromJSON.getLocalClassLoader().getCallResultCache().put(k, v.toFile()));

            } else {
                l_fetchedFromJSON = BridgeServiceFactory.createJavaCalls(ctx.body());
            }

            l_fetchedFromJSON.addHeaders(ctx.headerMap());

            ctx.contentType("application/json");
            ctx.result(BridgeServiceFactory.transformJavaCallResultsToJSON(l_fetchedFromJSON.submitCalls(),
                    l_fetchedFromJSON.fetchSecrets()));
        });

        if (ConfigValueHandlerIBS.MCP_ENABLED.is("true")) {
            MCPRequestHandler l_mcpHandler = new MCPRequestHandler();
            l_app.post("/mcp", l_mcpHandler::handle);
            log.info("MCP endpoint enabled at POST /mcp");
            l_app.get("/.well-known/oauth-authorization-server", ctx -> {
                ctx.status(404);
                ctx.result("{\"error\":\"not_found\",\"error_description\":\"This server does not support OAuth\"}");
            });
        }

        l_app.exception(JsonProcessingException.class, (e, ctx) -> {
            ctx.status(404);
            ctx.contentType(ERROR_CONTENT_TYPE);
            ctx.result(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_JSON_TRANSFORMATION, 404)));
        });

        l_app.exception(IBSPayloadException.class, (e, ctx) -> {
            ctx.status(404);
            ctx.contentType(ERROR_CONTENT_TYPE);
            ctx.result(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_PAYLOAD_INCONSISTENCY, 404)));
        });

        l_app.exception(AmbiguousMethodException.class, (e, ctx) -> {
            ctx.status(404);
            ctx.contentType(ERROR_CONTENT_TYPE);
            ctx.result(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_AMBIGUOUS_METHOD, 404, false)));
        });

        l_app.exception(IBSConfigurationException.class, (e, ctx) -> {
            ctx.status(500);
            ctx.contentType(ERROR_CONTENT_TYPE);
            ctx.result(BridgeServiceFactory.createExceptionPayLoad(new ErrorObject(e, ERROR_IBS_CONFIG, 500)));
        });

        l_app.exception(IBSRunTimeException.class, (e, ctx) -> {
            ctx.status(500);
            ctx.contentType(ERROR_CONTENT_TYPE);
            ctx.result(BridgeServiceFactory.createExceptionPayLoad(new ErrorObject(e, ERROR_IBS_RUNTIME, 500)));
        });

        l_app.exception(TargetJavaMethodCallException.class, (e, ctx) -> {
            ctx.status(500);
            ctx.contentType(ERROR_CONTENT_TYPE);
            ctx.result(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_CALLING_JAVA_METHOD, 500)));
        });

        l_app.exception(NonExistentJavaObjectException.class, (e, ctx) -> {
            ctx.status(404);
            ctx.contentType(ERROR_CONTENT_TYPE);
            ctx.result(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_JAVA_OBJECT_NOT_FOUND, 404, false)));
        });

        l_app.exception(JavaObjectInaccessibleException.class, (e, ctx) -> {
            ctx.status(404);
            ctx.contentType(ERROR_CONTENT_TYPE);
            ctx.result(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_JAVA_OBJECT_NOT_ACCESSIBLE, 404, false)));
        });

        l_app.exception(IBSTimeOutException.class, (e, ctx) -> {
            ctx.status(408);
            ctx.contentType(ERROR_CONTENT_TYPE);
            ctx.result(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_CALL_TIMEOUT, 408, false)));
        });

        l_app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.contentType(ERROR_CONTENT_TYPE);
            ctx.result(BridgeServiceFactory.createExceptionPayLoad(new ErrorObject(e, ERROR_IBS_INTERNAL, 500)));
        });

        l_app.after(ctx -> {
            if (ThreadContext.containsKey(UPLOADED_FILE_REF)) {
                for (String lt_fileName : ThreadContext.get(UPLOADED_FILE_REF).split(",")) {
                    log.debug("Cleaning up file {}. succeeded {}.", lt_fileName,
                            (new File(uploadDir.getName(), lt_fileName)).delete());
                }
                ThreadContext.remove(UPLOADED_FILE_REF);
            }
        });

        l_app.start(in_port);
        return l_app;
    }

    protected enum DeploymentMode {
        TEST, PRODUCTION
    }
}
