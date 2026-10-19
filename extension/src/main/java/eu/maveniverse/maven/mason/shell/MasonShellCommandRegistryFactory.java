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
package eu.maveniverse.maven.mason.shell;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.LookupContext;
import org.apache.maven.cling.invoker.mvnsh.ShellCommandRegistryFactory;
import org.jline.console.CommandRegistry;

/**
 * Registers Mason shell commands (e.g. {@code translate}) into {@code mvnsh}.
 *
 * <p>This factory is discovered by {@code mvnsh} via DI ({@link ShellCommandRegistryFactory}),
 * so the commands become available automatically when the Mason extension is active.
 *
 * @since 0.4.0
 */
@Named("mason")
@Singleton
public class MasonShellCommandRegistryFactory implements ShellCommandRegistryFactory {

    @Override
    public CommandRegistry createShellCommandRegistry(LookupContext context) {
        return new MasonShellCommandRegistry(context);
    }
}
