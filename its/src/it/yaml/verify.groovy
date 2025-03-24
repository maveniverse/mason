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

File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()

String log = buildLog.text

assert log.contains('from child/pom.yaml')
