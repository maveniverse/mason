/*
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
 */
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
