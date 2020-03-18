/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.panel.util;

import java.util.Set;

/**
 * Defines additional menu options that an {@link ExportableJTable} can perform on
 * right click.
 */
public interface AdditionalMenuOptions
    {
    /**
     * Return the array of {@link MenuOption}s to display.
     *
     * @return the array of {@link MenuOption}s to display
     */
    public MenuOption[] getMenuOptions();

    /**
     * Set the {@link MenuOption}s to display.
     *
     * @param menuOptions the {@link MenuOption}s to display
     */
    public void setMenuOptions(MenuOption[] menuOptions);
    }
