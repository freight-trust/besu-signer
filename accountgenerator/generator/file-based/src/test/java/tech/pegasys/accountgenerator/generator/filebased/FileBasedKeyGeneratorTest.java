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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;

public class FileBasedKeyGeneratorTest {

  // private static final String INVALID_PASSWORD = "invalidPassword";
  private static final String MY_PASSWORD = "myPassword";

  private static FileBasedKeyGeneratorFactory factory;

  @BeforeAll
  public static void createFactory() {
    try {
      Path passwordPath = Files.createTempFile("pwd", ".cfg");
      File passwordFile = passwordPath.toFile();
      passwordFile.deleteOnExit();
      Files.write(passwordPath, MY_PASSWORD.getBytes(Charset.defaultCharset()));
      factory =
          new FileBasedKeyGeneratorFactory(passwordPath, passwordFile.getParentFile().toPath());
    } catch (final Exception e) {
      // intentionally empty
    }
  }

  @Test
  public void success() throws Exception {

    final String address = factory.getGenerator().generate();
    assertThat(address).isNotNull();
    assertThat(address).isNotEmpty();

    final Path directory = factory.getDirectory();
    assertThat(directory).isNotNull();
    final Path keyFile =
        directory.resolve(String.format("%s.key", address.substring(2).toLowerCase()));
    assertThat(keyFile).isNotNull();
    try {
      Credentials cred = WalletUtils.loadCredentials(MY_PASSWORD, keyFile.toFile());
      String recovered = Keys.toChecksumAddress(cred.getAddress());
      assertThat(recovered).isEqualTo(address);
    } catch (CipherException e) {
      throw new Exception("Invalid credentials");
    }
  }
}
