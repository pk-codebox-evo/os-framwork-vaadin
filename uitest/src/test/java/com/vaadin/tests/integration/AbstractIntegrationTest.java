/*
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.tests.integration;

import com.vaadin.testbench.By;
import com.vaadin.testbench.elements.UIElement;
import com.vaadin.testbench.parallel.TestNameSuffix;
import com.vaadin.tests.tb3.SingleBrowserTestPhantomJS2;

/**
 * Base class for integration tests. Integration tests use the
 * {@literal deployment.url} parameter to determine the base deployment url
 * (http://hostname:123)
 *
 * @author Vaadin Ltd
 */
@TestNameSuffix(property = "server-name")
public abstract class AbstractIntegrationTest
        extends SingleBrowserTestPhantomJS2 {
    @Override
    protected String getBaseURL() {
        String deploymentUrl = System.getProperty("deployment.url");
        if (deploymentUrl == null || deploymentUrl.equals("")) {
            throw new RuntimeException(
                    "Deployment url must be given as deployment.url");
        }
        return deploymentUrl;
    }

    @Override
    protected void openTestURL() {
        super.openTestURL();

        waitForApplication();
    }

    protected void waitForApplication() {
        if (!isElementPresent(UIElement.class)) {
            // Wait for UI element.
            waitForElementPresent(By.vaadin("//com.vaadin.ui.UI"));
        }
    }
}
