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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.maven.api.Dialect;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelDialectManager;
import org.apache.maven.cling.invoker.LookupContext;
import org.jline.console.CmdDesc;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.impl.JlineCommandRegistry;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

/**
 * JLine command registry that adds the {@code translate} command to {@code mvnsh}.
 *
 * <p>Usage inside {@code mvnsh}:
 * <pre>
 *   translate --from xml --to yaml [path]
 *   translate --to yaml              # implies --from xml (the default)
 *   translate --to xml ./child       # translate a sub-directory
 * </pre>
 *
 * <p>The {@code path} argument (optional) is resolved relative to the current
 * working directory. When omitted the current working directory is used.
 *
 * @since 0.4.0
 */
class MasonShellCommandRegistry extends JlineCommandRegistry {

    private static final String CMD_TRANSLATE = "translate";

    private final LookupContext context;

    MasonShellCommandRegistry(LookupContext context) {
        this.context = context;
        Map<String, CommandMethods> commands = new HashMap<>();
        commands.put(CMD_TRANSLATE, new CommandMethods(this::translate, this::translateCompleter));
        registerCommands(commands);
    }

    @Override
    public String name() {
        return "Mason";
    }

    @Override
    public List<String> commandInfo(String command) {
        if (CMD_TRANSLATE.equals(command)) {
            return List.of("Translate a pom file between dialects (xml, yaml, toml, json, hocon).");
        }
        return List.of();
    }

    @Override
    public CmdDesc commandDescription(List<String> args) {
        return null; // tab-tip description — can be expanded later
    }

    // -------------------------------------------------------------------------

    /**
     * Implementation of the {@code translate} command.
     *
     * <p>Parses {@code --from <dialect>} and {@code --to <dialect>} from the args,
     * plus an optional trailing path. Delegates to {@link ModelDialectManager}.
     */
    private void translate(CommandInput input) {
        try {
            String[] args = input.args();
            String fromId = "xml"; // default source is always XML (the canonical form)
            String toId = null;
            Path targetDir = context.cwd.get();

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--from":
                        if (i + 1 < args.length) fromId = args[++i];
                        break;
                    case "--to":
                        if (i + 1 < args.length) toId = args[++i];
                        break;
                    default:
                        // trailing positional arg = target directory
                        targetDir = context.cwd.get().resolve(args[i]);
                        break;
                }
            }

            if (toId == null) {
                context.logger.error("translate: missing required option --to <dialect>");
                context.logger.error("Usage: translate [--from <dialect>] --to <dialect> [path]");
                return;
            }

            ModelDialectManager mgr = context.lookup.lookup(ModelDialectManager.class);

            Set<Dialect> available = mgr.getAvailableDialects();
            Dialect from = resolveDialect(fromId, available);
            Dialect to = resolveDialect(toId, available);

            if (from == null) {
                context.logger.error(
                        "translate: unknown source dialect '" + fromId + "'. Available: " + dialectNames(available));
                return;
            }
            if (to == null) {
                context.logger.error(
                        "translate: unknown target dialect '" + toId + "'. Available: " + dialectNames(available));
                return;
            }
            if (from.equals(to)) {
                context.logger.warn(
                        "translate: source and target dialects are the same (" + fromId + ") — nothing to do");
                return;
            }

            Optional<Model> model = mgr.readModel(targetDir, from, null);
            if (model.isEmpty()) {
                context.logger.error("translate: no " + fromId + " pom found in " + targetDir);
                return;
            }

            Optional<Path> written = mgr.writeModel(targetDir, to, model.get(), null);
            if (written.isEmpty()) {
                context.logger.error("translate: writer for dialect '" + toId + "' refused " + targetDir);
                return;
            }

            context.writer.accept("Translated to " + written.get());

        } catch (Exception e) {
            saveException(e);
        }
    }

    private List<Completer> translateCompleter(String name) {
        List<String> dialectIds;
        try {
            ModelDialectManager mgr = context.lookup.lookup(ModelDialectManager.class);
            dialectIds = mgr.getAvailableDialects().stream()
                    .map(Dialect::id)
                    .sorted()
                    .toList();
        } catch (Exception e) {
            dialectIds = List.of("xml", "yaml", "toml", "json", "hocon");
        }
        return List.of(new ArgumentCompleter(new StringsCompleter("--from", "--to"), new StringsCompleter(dialectIds)));
    }

    // -------------------------------------------------------------------------

    private static Dialect resolveDialect(String id, Set<Dialect> available) {
        return available.stream()
                .filter(d -> d.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static String dialectNames(Set<Dialect> dialects) {
        return dialects.stream().map(Dialect::id).sorted().toList().toString();
    }
}
