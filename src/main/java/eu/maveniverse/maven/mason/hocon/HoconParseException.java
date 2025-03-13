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

public class HoconParseException extends RuntimeException {
    public HoconParseException(String message) {
        super(message);
    }

    public HoconParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
