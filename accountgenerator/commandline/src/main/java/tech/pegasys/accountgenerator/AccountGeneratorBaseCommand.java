/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.accountgenerator;

import static tech.pegasys.accountgenerator.DefaultCommandValues.MANDATORY_HOST_FORMAT_HELP;
import static tech.pegasys.accountgenerator.DefaultCommandValues.MANDATORY_PATH_FORMAT_HELP;
import static tech.pegasys.accountgenerator.DefaultCommandValues.MANDATORY_PORT_FORMAT_HELP;

import tech.pegasys.accountgenerator.config.PicoCliTlsServerOptions;
import tech.pegasys.accountgenerator.core.config.Config;
import tech.pegasys.accountgenerator.core.config.TlsOptions;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Optional;

import com.google.common.base.MoreObjects;
import org.apache.logging.log4j.Level;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

@SuppressWarnings("FieldCanBeLocal") // because Picocli injected fields report false positives
@Command(
    description =
        "This command runs the AccountGenerator.\n"
            + "Documentation can be found at https://docs.accountgenerator.pegasys.tech.",
    abbreviateSynopsis = true,
    name = "accountgenerator",
    sortOptions = false,
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    header = "Usage:",
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n%n",
    optionListHeading = "%nOptions:%n",
    footerHeading = "%n",
    subcommands = {HelpCommand.class},
    footer = "AccountGenerator is licensed under the Apache License 2.0")
public class AccountGeneratorBaseCommand implements Config {

  @Option(
      names = {"--data-path"},
      description = "The path to a directory to store temporary files",
      paramLabel = MANDATORY_PATH_FORMAT_HELP,
      arity = "1")
  private Path dataPath;

  @Option(
      names = {"--logging", "-l"},
      paramLabel = "<LOG VERBOSITY LEVEL>",
      description =
          "Logging verbosity levels: OFF, FATAL, WARN, INFO, DEBUG, TRACE, ALL (default: INFO)")
  private final Level logLevel = Level.INFO;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--http-listen-host"},
      description = "Host for JSON-RPC HTTP to listen on (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_HOST_FORMAT_HELP,
      arity = "1")
  private String httpListenHost = InetAddress.getLoopbackAddress().getHostAddress();

  @Option(
      names = {"--http-listen-port"},
      description = "Port for JSON-RPC HTTP to listen on (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_PORT_FORMAT_HELP,
      arity = "1")
  private final Integer httpListenPort = 8545;

  @Option(
      names = {"--directory", "-d"},
      description = "The path to a directory to store generated toml files",
      paramLabel = MANDATORY_PATH_FORMAT_HELP,
      arity = "1")
  private Path directory;

  @ArgGroup(exclusive = false)
  private PicoCliTlsServerOptions picoCliTlsServerOptions;

  @Override
  public Path getDataPath() {
    return dataPath;
  }

  @Override
  public Level getLogLevel() {
    return logLevel;
  }

  @Override
  public String getHttpListenHost() {
    return httpListenHost;
  }

  @Override
  public Integer getHttpListenPort() {
    return httpListenPort;
  }

  @Override
  public Path getDirectory() {
    return directory;
  }

  @Override
  public Optional<TlsOptions> getTlsOptions() {
    return Optional.ofNullable(picoCliTlsServerOptions);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("dataPath", dataPath)
        .add("logLevel", logLevel)
        .add("httpListenHost", httpListenHost)
        .add("httpListenPort", httpListenPort)
        .add("directory", dataPath)
        .toString();
  }

  void validateArgs() {}
}
