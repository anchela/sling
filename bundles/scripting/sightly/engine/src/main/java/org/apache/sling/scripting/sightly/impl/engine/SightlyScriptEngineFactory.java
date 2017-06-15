/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.apache.sling.scripting.sightly.compiler.SightlyCompiler;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTL template engine factory
 */
@Component(
        service = ScriptEngineFactory.class,
        property = {
                "extensions=html",
                "names=htl",
                "names=HTL",
                Constants.SERVICE_DESCRIPTION + "=HTL Templating Engine",
                "compatible.javax.script.name=sly"
        }
)
public class SightlyScriptEngineFactory extends AbstractScriptEngineFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SightlyScriptEngineFactory.class);

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    @Reference
    private SightlyEngineConfiguration sightlyEngineConfiguration;

    @Reference
    private ClassLoaderWriter classLoaderWriter;

    @Reference
    private SightlyCompiler sightlyCompiler;

    @Reference
    private SightlyJavaCompilerService sightlyJavaCompilerService;

    public final static String SHORT_NAME = "sightly";

    public final static String LANGUAGE_NAME = "The HTL Templating Language";

    public final static String LANGUAGE_VERSION = "1.3";

    public final static String EXTENSION = "html";

    public static final String SIGHTLY_CONFIG_FILE = "/sightly.config";

    public SightlyScriptEngineFactory() {
        setNames("htl", "HTL", SHORT_NAME);
        setExtensions(EXTENSION);
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    @Override
    public String getLanguageVersion() {
        return LANGUAGE_VERSION;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new SightlyScriptEngine(this, sightlyCompiler, sightlyJavaCompilerService, sightlyEngineConfiguration);
    }

    protected ClassLoader getClassLoader() {
        return dynamicClassLoaderManager.getDynamicClassLoader();
    }

    @Activate
    protected void activate() {
        InputStream is;
        boolean newVersion = true;
        String versionInfo = null;
        String newVersionString = sightlyEngineConfiguration.getEngineVersion();
        try {
            is = classLoaderWriter.getInputStream(SIGHTLY_CONFIG_FILE);
            if (is != null) {
                versionInfo = IOUtils.toString(is, "UTF-8");
                if (newVersionString.equals(versionInfo)) {
                    newVersion = false;
                } else {
                    LOGGER.info("Detected stale classes generated by Apache Sling Scripting HTL engine version {}.", versionInfo);
                }
                IOUtils.closeQuietly(is);
            }
        } catch (IOException e) {
            // do nothing; if we didn't find any previous version information we're considering our version to be new
        }
        if (newVersion) {
            OutputStream os = classLoaderWriter.getOutputStream(SIGHTLY_CONFIG_FILE);
            try {
                IOUtils.write(sightlyEngineConfiguration.getEngineVersion(), os, "UTF-8");
            } catch (IOException e) {
                // ignore
            } finally {
                IOUtils.closeQuietly(os);
            }
            String scratchFolder = sightlyEngineConfiguration.getScratchFolder();
            boolean scratchFolderDeleted = classLoaderWriter.delete(scratchFolder);
            if (scratchFolderDeleted) {
                if (StringUtils.isNotEmpty(versionInfo)) {
                    LOGGER.info("Deleted stale classes generated by Apache Sling Scripting HTL engine version {}.", versionInfo);
                }
            }
        }
    }
}
