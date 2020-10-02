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

import tech.pegasys.accountgenerator.core.KeyGenerator;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

public class HSMKeyStoreGenerator implements KeyGenerator {

  protected static final Logger LOG = LogManager.getLogger();
  protected static final String CURVE = "secp256k1";
  // protected static final String CURVE = "secp256r1";
  // protected static final String CURVE = "secp384r1";
  protected static final String ALGORITHM = "SHA256withECDSA";

  private final HSMKeyStoreProvider provider;

  public HSMKeyStoreGenerator() {
    this.provider = null;
  }

  public HSMKeyStoreGenerator(final HSMKeyStoreProvider provider) {
    this.provider = provider;
  }

  @Override
  public String generate() {
    Provider p = provider.getProvider();
    KeyStore ks = provider.getKeyStore();
    String address = null;
    try {
      address = generateKey(p, ks, ALGORITHM, CURVE);
      LOG.debug("Generated new key with address: " + address);
    } catch (Exception ex) {
      LOG.trace(ex);
    }
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
    sb.append(String.format("%s = \"%s\"\n", "slotIndex", provider.getSlotIndex()));
    return sb.toString();
  }

  public List<String> getAll() {
    List<String> result = new ArrayList<>();
    KeyStore ks = provider.getKeyStore();
    try {
      for (Enumeration<String> aliases = ks.aliases(); aliases.hasMoreElements(); ) {
        String address = aliases.nextElement();
        LOG.debug("Listed key with address: " + address);
        result.add(address);
      }
    } catch (KeyStoreException ex) {
      LOG.trace(ex);
    }
    return result;
  }

  public void removeAll() {
    KeyStore ks = provider.getKeyStore();
    try {
      for (Enumeration<String> aliases = ks.aliases(); aliases.hasMoreElements(); ) {
        String address = aliases.nextElement();
        LOG.debug("Deleted key with address: " + address);
        try {
          ks.deleteEntry(address);
        } catch (KeyStoreException e) {
          LOG.trace(e);
        }
      }
    } catch (KeyStoreException ex) {
      LOG.trace(ex);
    }
  }

  public boolean exists(String address) {
    KeyStore ks = provider.getKeyStore();
    try {
      return ks.containsAlias(address);
    } catch (KeyStoreException ex) {
      LOG.trace(ex);
      return false;
    }
  }

  protected String generateKey(Provider p, KeyStore ks, String algo, String curve)
      throws Exception {
    // Generate an EC key pair using the provider to force generation on the HSM instead of
    // software.
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", p);
    ECGenParameterSpec kpgparams = new ECGenParameterSpec(curve);
    keyPairGenerator.initialize(kpgparams);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    // Create a self-signed certificate to store with the public key.
    // This is a java keystore requirement. The certificate is signed using the HSM.
    X509Certificate cert =
        generateCertificate(keyPair, 365, algo, "CN=AccountGenerator, L=CT, C=ZA", p);
    String address = generateAddress(keyPair, curve);
    ks.setKeyEntry(address, keyPair.getPrivate(), null, new X509Certificate[] {cert});
    return address;
  }

  protected String generateAddress(KeyPair keyPair, String curve) {
    byte[] publicKey = extractPublicKey((ECPublicKey) keyPair.getPublic(), curve);
    return Keys.toChecksumAddress(Keys.getAddress(Sign.publicFromPoint(publicKey)));
  }

  protected X509Certificate generateCertificate(
      KeyPair pair, int days, String algorithm, String dn, Provider p) throws Exception {
    X500Name issuerName = new X500Name(dn);
    BigInteger serial = BigInteger.valueOf(new SecureRandom().nextInt()).abs();
    Calendar calendar = Calendar.getInstance();
    Date startDate = new Date();
    calendar.setTime(startDate);
    calendar.add(Calendar.DAY_OF_YEAR, days);

    Date endDate = calendar.getTime();
    X509v3CertificateBuilder builder =
        new JcaX509v3CertificateBuilder(
            issuerName, serial, startDate, endDate, issuerName, pair.getPublic());
    builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));

    KeyUsage usage =
        new KeyUsage(
            KeyUsage.keyCertSign
                | KeyUsage.digitalSignature
                | KeyUsage.keyEncipherment
                | KeyUsage.dataEncipherment
                | KeyUsage.cRLSign);
    builder.addExtension(Extension.keyUsage, false, usage);

    ASN1EncodableVector purposes = new ASN1EncodableVector();
    purposes.add(KeyPurposeId.id_kp_serverAuth);
    purposes.add(KeyPurposeId.id_kp_clientAuth);
    purposes.add(KeyPurposeId.anyExtendedKeyUsage);
    builder.addExtension(Extension.extendedKeyUsage, false, new DERSequence(purposes));
    ContentSigner contentSigner =
        new JcaContentSignerBuilder(algorithm).setProvider(p).build(pair.getPrivate());

    JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
    X509Certificate cert = converter.getCertificate(builder.build(contentSigner));
    cert.checkValidity(new Date());
    cert.verify(pair.getPublic());
    return cert;
  }

  private byte[] extractPublicKey(final ECPublicKey publicKey, String curve) {
    final ECPoint w = publicKey.getW();
    final BigInteger x = w.getAffineX();
    final BigInteger y = w.getAffineY();
    X9ECParameters params = SECNamedCurves.getByName(curve);
    ECDomainParameters ec =
        new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
    return ec.getCurve().createPoint(x, y).getEncoded(false);
  }
}
