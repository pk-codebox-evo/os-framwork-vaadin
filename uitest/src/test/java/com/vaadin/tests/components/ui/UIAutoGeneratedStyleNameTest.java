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
package com.vaadin.tests.components.ui;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.tests.tb3.MultiBrowserTest;

/**
 * Test to check auto-generated style name for UI div and overlays div.
 *
 * @author Vaadin Ltd
 */
public class UIAutoGeneratedStyleNameTest extends MultiBrowserTest {

    @Test
    public void testUiStyleName() {
        openTestURL();

        Assert.assertTrue(
                "UI div element doesn't contain autogenerated style name",
                containsStyle(getDriver().findElement(By.className("v-app")),
                        UIAutoGeneratedStyleName.class.getSimpleName()
                                .toLowerCase(Locale.ENGLISH)));

        Assert.assertTrue(
                "Overlays div element doesn't contain autogenerated style name",
                containsStyle(
                        getDriver().findElement(
                                By.className("v-overlay-container")),
                        UIAutoGeneratedStyleName.class.getSimpleName()
                                .toLowerCase(Locale.ENGLISH)));
    }

    private boolean containsStyle(WebElement element, String style) {
        return element.getAttribute("class").contains(style);
    }
}
