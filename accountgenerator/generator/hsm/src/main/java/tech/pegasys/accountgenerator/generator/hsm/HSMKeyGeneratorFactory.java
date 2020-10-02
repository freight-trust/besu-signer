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
package tech.pegasys.accountgenerator.generator.hsm;

import tech.pegasys.accountgenerator.core.KeyGeneratorProvider;

import java.nio.file.Path;

public class HSMKeyGeneratorFactory implements KeyGeneratorProvider {

  private final HSMCrypto crypto;
  private final HSMWallet wallet;
  private final String slotLabel;
  private final String slotPin;
  private final Path directory;
  private boolean initialized = false;

  public HSMKeyGeneratorFactory(
      final String library, final String slotLabel, final String slotPin, final Path directory) {
    this.slotLabel = slotLabel;
    this.slotPin = slotPin;
    crypto = new HSMCrypto(library);
    wallet = new HSMWallet(this.crypto, this.slotLabel);
    this.directory = directory;
  }

  public void initialize() {
    crypto.initialize();
    wallet.open(slotPin);
    initialized = true;
  }

  @Override
  public void shutdown() {
    wallet.close();
    crypto.shutdown();
    initialized = false;
  }

  public HSMWallet getWallet() {
    return wallet;
  }

  public String getSlotLabel() {
    return slotLabel;
  }

  @Override
  public HSMKeyGenerator getGenerator() {
    if (!initialized) initialize();
    return new HSMKeyGenerator(crypto, wallet);
  }

  @Override
  public Path getDirectory() {
    return directory;
  }
}
