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
package tech.pegasys.accountgenerator.generator.cavium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.util.List;
import java.util.Properties;

import com.cavium.key.CaviumECPrivateKey;
import com.cavium.key.CaviumECPublicKey;
import com.cavium.key.CaviumKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CaviumKeyStoreProviderTest {

  private static String library;
  private static String pin;
  private static String sas;

  private static CaviumKeyStoreProvider ksp;
  private static CaviumKeyStoreGenerator kgr;

  @BeforeAll
  public static void beforeAll() {

    Properties p = new Properties();
    InputStream is = ClassLoader.getSystemResourceAsStream("cavium.properties");
    try {
      p.load(is);
      library = p.getProperty("library");
      pin = p.getProperty("pin");
      sas = p.getProperty("sas");
    } catch (IOException e) {
      fail("Properties file not found");
    }

    org.junit.jupiter.api.Assumptions.assumeTrue((new File(library).exists()));
    ksp = new CaviumKeyStoreProvider(library, pin);
    kgr = new CaviumKeyStoreGenerator(ksp, sas);
  }

  @AfterAll
  public static void afterAll() {
    if (kgr != null) {
      kgr.shutdown();
    }
    if (ksp != null) {
      ksp.shutdown();
    }
  }

  @Test
  void generateTest() {
    String address = kgr.generate();
    System.out.println("Generated: " + address);
    boolean exists = kgr.exists(address);
    assertThat(exists).isTrue();
    List<String> addresses = kgr.getAll();
    assertThat(addresses).contains(address);
    CaviumKey privateKey = null;
    try {
      privateKey = (CaviumKey) ksp.getKeyStore().getKey(address, "".toCharArray());
    } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException e) {
      fail("Failed to retrieve private key handle");
    }
    assertThat(privateKey).isNotNull();
    assertThat(privateKey).isInstanceOf(CaviumECPrivateKey.class);
    CaviumKey publicKey = null;
    try {
      publicKey =
          (CaviumKey) ksp.getKeyStore().getKey(address.replaceFirst("0x", "1x"), "".toCharArray());
    } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException e) {
      fail("Failed to retrieve private key handle");
    }
    assertThat(publicKey).isNotNull();
    assertThat(publicKey).isInstanceOf(CaviumECPublicKey.class);
  }

  @Test
  void getAllTest() {
    String address = kgr.generate();
    System.out.println("Generated: " + address);
    List<String> addresses = kgr.getAll();
    assertThat(addresses).isNotEmpty();
    for (String a : addresses) {
      System.out.println("Listed: " + a);
    }
  }
}
