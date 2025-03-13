/*******************************************************************************
 * Copyright (c) 2025 Guillaume Nodet
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at:
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.maveniverse.maven.mason.hocon;

/**
 * Simple class to store source location information for Maven model elements.
 *
 * @param line   the line number in the source file (1-based)
 * @param column the column number in the source file (1-based)
 * @param source the source identifier (usually a file path)
 */
public record HoconLocation(int line, int column, String source) {

    @Override
    public String toString() {
        return source + " [line " + line + ", column " + column + "]";
    }
}
