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
package tech.pegasys.accountgenerator.generator.filebased;

import tech.pegasys.accountgenerator.core.KeyGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import com.google.common.base.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;

public class FileBasedKeyGenerator implements KeyGenerator {

  private static final Logger LOG = LogManager.getLogger();

  private final Path passwordFile;
  private final Path directory;

  public FileBasedKeyGenerator(final Path passwordFile, final Path directory) {
    this.passwordFile = passwordFile;
    this.directory = directory;
  }

  @Override
  public String generate() {
    String address = null;
    try {
      final String password = readPasswordFromFile(passwordFile);
      String fileName = WalletUtils.generateLightNewWalletFile(password, directory.toFile());
      Path generated = directory.resolve(fileName);
      Path renamed = directory.resolve(fileName.substring(37).replace(".json", ".key"));
      boolean success = generated.toFile().renameTo(renamed.toFile());
      if (!success) throw new Exception("Can't rename newly generated key file");
      Credentials credentials = WalletUtils.loadCredentials(password, renamed.toFile());
      address = Keys.toChecksumAddress(credentials.getAddress());
      LOG.debug("Generated new key with address: " + address);
    } catch (final Exception ex) {
      LOG.error(ex);
    }
    return address;
  }

  @Override
  public String metaData(final String address) {
    final String createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    final StringBuilder sb = new StringBuilder();
    sb.append(String.format("[%s]\n", "metadata"));
    sb.append(String.format("%s = %s\n", "createdAt", createdAt));
    sb.append(String.format("%s = \"%s\"\n", "description", "File based configuration"));
    sb.append(String.format("\n"));
    sb.append(String.format("[%s]\n", "signing"));
    sb.append(String.format("%s = \"%s\"\n", "type", "file-based-signer"));
    sb.append(
        String.format(
            "%s = \"%s\"\n", "key-file", "./" + address.substring(2).toLowerCase() + ".key"));
    sb.append(String.format("%s = \"%s\"\n", "password-file", "./" + passwordFile.getFileName()));
    return sb.toString();
  }

  private static String readPasswordFromFile(final Path path) throws IOException {
    final byte[] fileContent = Files.readAllBytes(path);
    return new String(fileContent, Charsets.UTF_8);
  }
}
