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

import tech.pegasys.accountgenerator.core.KeyGenerator;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HSMKeyGenerator implements KeyGenerator {

  private static final Logger LOG = LogManager.getLogger();

  private final HSMCrypto crypto;
  private final HSMWallet wallet;

  public HSMKeyGenerator(final HSMCrypto crypto, final HSMWallet wallet) {
    this.crypto = crypto;
    this.wallet = wallet;
  }

  @Override
  public String generate() {
    String address = wallet.generate();
    LOG.info("Generated new key with address: " + address);
    return address;
  }

  @Override
  public String metaData(final String address) {
    final String createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    final StringBuilder sb = new StringBuilder();
    sb.append(String.format("[%s]\n", "metadata"));
    sb.append(String.format("%s = %s\n", "createdAt", createdAt));
    sb.append(String.format("%s = \"%s\"\n", "description", "HSM configuration"));
    sb.append(String.format("\n"));
    sb.append(String.format("[%s]\n", "signing"));
    sb.append(String.format("%s = \"%s\"\n", "type", "hsm-signer"));
    sb.append(String.format("%s = \"%s\"\n", "address", address));
    sb.append(String.format("%s = \"%s\"\n", "slot", wallet.getLabel()));
    return sb.toString();
  }

  @Override
  public void shutdown() {
    wallet.close();
    crypto.shutdown();
  }
}
