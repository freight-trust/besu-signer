/*
 * Copyright 2018 ConsenSys AG.
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
package tech.pegasys.accountgenerator.generator.filebased;

import static tech.pegasys.accountgenerator.DefaultCommandValues.MANDATORY_FILE_FORMAT_HELP;

import tech.pegasys.accountgenerator.AccountGeneratorSubCommand;
import tech.pegasys.accountgenerator.KeyGeneratorInitializationException;
import tech.pegasys.accountgenerator.core.KeyGeneratorProvider;

import java.nio.file.Path;

import com.google.common.base.MoreObjects;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** File-based authentication related sub-command */
@Command(
    name = FileBasedSubCommand.COMMAND_NAME,
    description = "Generate a key stored in an encrypted V3 Keystore file.",
    mixinStandardHelpOptions = true)
public class FileBasedSubCommand extends AccountGeneratorSubCommand {

  public static final String COMMAND_NAME = "file-based-account-generator";

  public FileBasedSubCommand() {}

  @SuppressWarnings("unused") // Picocli injects reference to command spec
  @Spec
  private CommandLine.Model.CommandSpec spec;

  @Option(
      names = {"-p", "--password-file"},
      description = "The path to a file containing the password used to decrypt the keyfile.",
      required = true,
      paramLabel = MANDATORY_FILE_FORMAT_HELP,
      arity = "1")
  private Path passwordFilePath;

  @Override
  public KeyGeneratorProvider createGeneratorFactory(Path directory)
      throws KeyGeneratorInitializationException {
    return new FileBasedKeyGeneratorFactory(passwordFilePath, directory);
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("passwordFilePath", passwordFilePath).toString();
  }
}
