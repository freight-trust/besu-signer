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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Provider;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import com.cavium.key.CaviumKey;
import com.cavium.key.parameter.CaviumECGenParameterSpec;

public class CaviumKeyStoreGenerator extends HSMKeyStoreGenerator {

  private String sas;

  public CaviumKeyStoreGenerator(final CaviumKeyStoreProvider provider, final String sas) {
    super(provider);
    this.sas = sas;
  }

  @Override
  protected String generateKey(Provider p, KeyStore ks, String algo, String curve)
      throws Exception {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", p);
    CaviumECGenParameterSpec kpgparams =
        new CaviumECGenParameterSpec(curve, "EC-public-key", "EC-private-key", false, true);
    keyPairGenerator.initialize(kpgparams);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    String address = generateAddress(keyPair, curve);
    long privateHandle = ((CaviumKey) keyPair.getPrivate()).getHandle();
    long publicHandle = ((CaviumKey) keyPair.getPublic()).getHandle();
    List<String> list =
        Arrays.asList(
            sas,
            System.getProperty("HSM_USER"),
            System.getProperty("HSM_PASSWORD"),
            Long.toString(privateHandle),
            Long.toString(publicHandle),
            address);
    String result = runCommand(list);
    LOG.info(result);
    return address;
  }

  @Override
  public String metaData(final String address) {
    final String createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    final StringBuilder sb = new StringBuilder();
    sb.append(String.format("[%s]\n", "metadata"));
    sb.append(String.format("%s = %s\n", "createdAt", createdAt));
    sb.append(String.format("%s = \"%s\"\n", "description", "Cavium configuration"));
    sb.append(String.format("\n"));
    sb.append(String.format("[%s]\n", "signing"));
    sb.append(String.format("%s = \"%s\"\n", "type", "cavium-signer"));
    sb.append(String.format("%s = \"%s\"\n", "address", address));
    return sb.toString();
  }

  public String runCommand(List<String> params) {
    ProcessBuilder pb = new ProcessBuilder(params);
    Process p;
    String result = "";
    try {
      p = pb.start();
      final BufferedReader reader =
          new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()));
      StringJoiner sj = new StringJoiner(System.getProperty("line.separator"));
      reader.lines().iterator().forEachRemaining(sj::add);
      result = sj.toString();
      p.waitFor();
      p.destroy();
    } catch (Exception ex) {
      LOG.trace(ex);
    }
    return result;
  }
}
