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

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.accountgenerator.generator.cavium.CaviumSubCommand;
import tech.pegasys.accountgenerator.generator.filebased.FileBasedSubCommand;
import tech.pegasys.accountgenerator.generator.hsm.HSMSubCommand;

import java.io.PrintWriter;

public class AccountGeneratorApp {

  public static void main(final String... args) {

    final AccountGeneratorBaseCommand baseCommand = new AccountGeneratorBaseCommand();
    final PrintWriter outputWriter = new PrintWriter(System.out, true, UTF_8);
    final PrintWriter errorWriter = new PrintWriter(System.err, true, UTF_8);
    final CommandlineParser cmdLineParser =
        new CommandlineParser(baseCommand, outputWriter, errorWriter);
    cmdLineParser.registerGenerator(new FileBasedSubCommand());
    cmdLineParser.registerGenerator(new HSMSubCommand());
    cmdLineParser.registerGenerator(new CaviumSubCommand());
    cmdLineParser.parseCommandLine(args);
  }
}
