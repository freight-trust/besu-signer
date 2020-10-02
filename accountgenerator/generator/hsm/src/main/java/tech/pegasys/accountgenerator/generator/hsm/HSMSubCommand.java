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
package tech.pegasys.accountgenerator.generator.hsm;

import tech.pegasys.accountgenerator.AccountGeneratorSubCommand;
import tech.pegasys.accountgenerator.KeyGeneratorInitializationException;
import tech.pegasys.accountgenerator.core.KeyGeneratorProvider;

import java.nio.file.Path;

import com.google.common.base.MoreObjects;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** HSM-based authentication related sub-command */
@Command(
    name = HSMSubCommand.COMMAND_NAME,
    description = "Generate a key stored in an HSM.",
    mixinStandardHelpOptions = true)
public class HSMSubCommand extends AccountGeneratorSubCommand {

  public static final String COMMAND_NAME = "hsm-account-generator";

  public HSMSubCommand() {}

  @SuppressWarnings("unused") // Picocli injects reference to command spec
  @Spec
  private CommandLine.Model.CommandSpec spec;

  @Option(
      names = {"-l", "--library"},
      description = "The HSM PKCS11 library used to sign transactions.",
      paramLabel = "<LIBRARY_PATH>",
      required = true)
  private Path libraryPath;

  @Option(
      names = {"-s", "--slot-label"},
      description = "The HSM slot used to sign transactions.",
      paramLabel = "<SLOT_LABEL>",
      required = true)
  private String slotLabel;

  @Option(
      names = {"-p", "--slot-pin"},
      description = "The crypto user pin of the HSM slot used to sign transactions.",
      paramLabel = "<SLOT_PIN>",
      required = true)
  private String slotPin;

  @Override
  public KeyGeneratorProvider createGeneratorFactory(Path directory)
      throws KeyGeneratorInitializationException {
    return new HSMKeyGeneratorFactory(libraryPath.toString(), slotLabel, slotPin, directory);
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("library", libraryPath)
        .add("slot", slotLabel)
        .toString();
  }
}
