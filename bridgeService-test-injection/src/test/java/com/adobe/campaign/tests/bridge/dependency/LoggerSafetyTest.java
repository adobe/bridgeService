/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.dependency;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertTrue;

/**
 * Regression guard: ensures no Java source file calls LogManager.getLogger() without an explicit
 * class argument. The no-arg form relies on stack-walking which fails in a fat/shaded JAR.
 */
public class LoggerSafetyTest {

    private static final Pattern BARE_GET_LOGGER = Pattern.compile("LogManager\\.getLogger\\(\\s*\\)");

    @DataProvider(name = "sourceTrees")
    public Object[][] sourceTrees() {
        String l_root = System.getProperty("bridgeservice.root.dir");
        return new Object[][]{
                {"integroBridgeService/src/main/java", l_root},
                {"bridgeService-data/src/main/java", l_root}
        };
    }

    @Test(groups = "static-analysis", dataProvider = "sourceTrees")
    public void testNoParamlessGetLogger(String in_relativeSourceDir, String in_rootDir) throws IOException {
        Path l_sourceRoot = Paths.get(in_rootDir, in_relativeSourceDir);
        assertTrue(Files.exists(l_sourceRoot),
                "Source tree not found: " + l_sourceRoot.toAbsolutePath());

        List<String> l_violations = new ArrayList<>();

        try (Stream<Path> l_files = Files.walk(l_sourceRoot)) {
            List<Path> l_javaFiles = l_files
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            for (Path lt_file : l_javaFiles) {
                List<String> lt_lines = Files.readAllLines(lt_file);
                for (int i = 0; i < lt_lines.size(); i++) {
                    if (BARE_GET_LOGGER.matcher(lt_lines.get(i)).find()) {
                        l_violations.add(l_sourceRoot.relativize(lt_file) + ":" + (i + 1));
                    }
                }
            }
        }

        assertTrue(l_violations.isEmpty(),
                "Found parameterless LogManager.getLogger() calls (use getLogger(MyClass.class) instead):\n"
                        + String.join("\n", l_violations));
    }
}
