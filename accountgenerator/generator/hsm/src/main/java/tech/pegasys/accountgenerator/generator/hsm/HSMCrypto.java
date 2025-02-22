/*
 * Copyright 2020 ConsenSys AG.
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

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iaik.pkcs.pkcs11.Mechanism;
import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.SessionInfo;
import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.State;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.objects.ECPrivateKey;
import iaik.pkcs.pkcs11.objects.ECPublicKey;
import iaik.pkcs.pkcs11.objects.Key;
import iaik.pkcs.pkcs11.objects.KeyPair;
import iaik.pkcs.pkcs11.objects.PKCS11Object;
import iaik.pkcs.pkcs11.objects.PrivateKey;
import iaik.pkcs.pkcs11.objects.PublicKey;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

public class HSMCrypto {

  private static final Logger LOG = LogManager.getLogger();
  private static final String CURVE = "secp256k1";

  private Module module;

  private final String library;
  private final Map<Long, Slot> slots;
  private final Map<String, Long> labels;
  private final Map<Long, Session> sessions;
  private final X9ECParameters params;
  private final ECDomainParameters curve;

  public HSMCrypto(final String library) {
    this.library = library;
    this.slots = new HashMap<>();
    this.labels = new HashMap<>();
    this.sessions = new HashMap<>();
    this.params = SECNamedCurves.getByName(CURVE);
    this.curve =
        new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
  }

  // initialize gets information on available slots and initialize the crypto module
  public void initialize() {
    try {
      module = Module.getInstance(library);
      module.initialize(null);
      Slot[] slotList = module.getSlotList(true);
      for (Slot s : slotList) {
        long id = s.getSlotID();
        LOG.debug(id);
        slots.put(id, s);
        labels.put(s.getToken().getTokenInfo().getLabel().trim(), id);
      }
    } catch (IOException | TokenException ex) {
      LOG.error(ex);
      throw new HSMCryptoException("Failed to initialize crypto module.", ex);
    }
  }

  // shutdown will log out all users and finalize the crypto module
  public void shutdown() {
    try {
      for (long slotIndex : slots.keySet()) {
        if (sessions.get(slotIndex) != null) logout(slotIndex);
      }
      if (module != null) module.finalize(null);
    } catch (TokenException ex) {
      LOG.error(ex);
      throw new HSMCryptoException("Failed to shutdown crypto module.", ex);
    }
  }

  // login will log a user into a slot
  public void login(long slotIndex, String slotPin) {
    if (slotPin.isEmpty()) {
      throw new HSMCryptoException("Invalid pin.");
    }
    try {
      if (slots.containsKey(slotIndex)) {
        Session session = slots.get(slotIndex).getToken().openSession(true, true, null, null);
        session.login(Session.UserType.USER, slotPin.toCharArray());
        sessions.put(slotIndex, session);
      } else {
        throw new RuntimeException("Invalid slot index.");
      }
    } catch (Exception ex) {
      LOG.error(ex);
      throw new HSMCryptoException("Failed to login to slot.", ex);
    }
  }

  // logout will log a user out of a slot
  public void logout(long slotIndex) {
    try {
      Session session = sessions.get(slotIndex);
      if (session != null) {
        session.logout();
        session.closeSession();
        sessions.put(slotIndex, null);
      } else {
        throw new RuntimeException("Invalid slot index or session not open.");
      }
    } catch (Exception ex) {
      LOG.error(ex);
      throw new HSMCryptoException("Failed to logout of slot.", ex);
    }
  }

  // isLoggedIn returns true if a user is currently logged into the slot
  public boolean isLoggedIn(long slotIndex) {
    boolean result = false;
    Session session = openSession(slotIndex);
    try {
      SessionInfo si = session.getSessionInfo();
      result = si.getState().equals(State.RW_USER_FUNCTIONS);
    } catch (TokenException ex) {
      LOG.error(ex);
      throw new HSMCryptoException("Failed to determine user status.", ex);
    } finally {
      closeSession(session);
    }
    return result;
  }

  // generateECKeyPair generates a new key pair inside the wallet
  public String generateECKeyPair(long slotIndex) {
    String address;
    byte[] id = timeToBytes();
    byte[] ecParams =
        new byte[] {
          (byte) 0x06, (byte) 0x05, (byte) 0x2B, (byte) 0x81, (byte) 0x04, (byte) 0x00, (byte) 0x0A
        };

    ECPrivateKey privateKeyTemplate = new ECPrivateKey();
    privateKeyTemplate.getToken().setBooleanValue(Boolean.TRUE);
    privateKeyTemplate.getSign().setBooleanValue(Boolean.TRUE);
    privateKeyTemplate.getPrivate().setBooleanValue(Boolean.TRUE);
    privateKeyTemplate.getLabel().setCharArrayValue("EC-private-key".toCharArray());
    privateKeyTemplate.getId().setByteArrayValue(id);
    ECPublicKey publicKeyTemplate = new ECPublicKey();
    publicKeyTemplate.getToken().setBooleanValue(Boolean.TRUE);
    publicKeyTemplate.getVerify().setBooleanValue(Boolean.TRUE);
    publicKeyTemplate.getEcdsaParams().setByteArrayValue(ecParams);
    publicKeyTemplate.getPrivate().setBooleanValue(Boolean.FALSE);
    publicKeyTemplate.getLabel().setCharArrayValue("EC-public-key".toCharArray());
    publicKeyTemplate.getId().setByteArrayValue(id);
    KeyPair keyPair;
    Session session = openSession(slotIndex);
    try {
      keyPair =
          session.generateKeyPair(
              Mechanism.get(PKCS11Constants.CKM_EC_KEY_PAIR_GEN),
              publicKeyTemplate,
              privateKeyTemplate);
      address = getAddress((ECPublicKey) keyPair.getPublicKey());
      setLabel(session, keyPair.getPrivateKey(), address);
      setLabel(session, keyPair.getPublicKey(), address);
    } catch (TokenException ex) {
      LOG.error(ex);
      throw new HSMCryptoException("Failed to generate key pair.", ex);
    } finally {
      closeSession(session);
    }
    return address;
  }

  // deleteECKeyPair deletes a key pair from the wallet
  public void deleteECKeyPair(long slotIndex, String address) {
    Session session = openSession(slotIndex);
    try {
      Key key = new Key();
      key.getLabel().setCharArrayValue(address.toCharArray());
      List<PKCS11Object> keys = findObjects(session, key);
      if (keys.isEmpty()) throw new RuntimeException("Failed to find key pair.");
      for (PKCS11Object k : keys) {
        session.destroyObject(k);
      }
    } catch (Exception ex) {
      LOG.error(ex);
      throw new HSMCryptoException("Failed to delete key pair.", ex);
    } finally {
      closeSession(session);
    }
  }

  // getAddresses returns all the addresses on the wallet
  public List<String> getAddresses(long slotIndex) {
    Session session = openSession(slotIndex);
    List<String> result = new ArrayList<>();
    try {
      PrivateKey key = new PrivateKey();
      session.findObjectsInit(key);
      PKCS11Object[] objects = session.findObjects(100);
      session.findObjectsFinal();
      for (PKCS11Object object : objects) {
        String address = object.getAttribute(PKCS11Constants.CKA_LABEL).toString();
        if (isAddress(address)) {
          LOG.debug(address);
          result.add(address);
        }
      }
    } catch (TokenException ex) {
      LOG.error(ex);
      throw new HSMCryptoException("Failed to get list of addresses.", ex);
    } finally {
      closeSession(session);
    }
    return result;
  }

  public long getSlotIndex(String label) {
    if (labels.containsKey(label)) return labels.get(label);
    return -1L;
  }

  // containsAddress returns true if the wallet contains the given address
  public boolean containsAddress(long slotIndex, String address) {
    Session session = openSession(slotIndex);
    boolean result = false;
    try {
      PublicKey key = new PublicKey();
      key.getLabel().setCharArrayValue(address.toCharArray());
      result = findObject(session, key) != null;
    } catch (Exception ex) {
      LOG.error(ex);
      throw new HSMCryptoException("Failed to determine if slot contains address.", ex);
    } finally {
      closeSession(session);
    }
    return result;
  }

  // openSession opens a new session
  private Session openSession(long slotIndex) {
    try {
      if (slots.containsKey(slotIndex))
        return slots.get(slotIndex).getToken().openSession(true, true, null, null);
      else throw new RuntimeException("Invalid slot index: " + slotIndex);
    } catch (Exception ex) {
      LOG.error(ex);
      throw new HSMCryptoException("Failed to open session.", ex);
    }
  }

  // closeSession closes a session
  private void closeSession(Session session) {
    try {
      session.closeSession();
    } catch (TokenException ex) {
      LOG.error(ex);
      throw new HSMCryptoException("Failed to close session.", ex);
    }
  }

  // timeToBytes returns the current unix time as a byte array
  private byte[] timeToBytes() {
    long l = Instant.now().getEpochSecond();
    byte[] result = new byte[8];
    for (int i = 7; i >= 0; i--) {
      result[i] = (byte) (l & 0xFF);
      l >>= 8;
    }
    return result;
  }

  // getECPoint returns the CKA_EC_POINT of the given public key.
  private byte[] getECPoint(ECPublicKey publicKey) {
    return publicKey.getEcPoint().getByteArrayValue();
  }

  // getDecodedECPoint decodes the CKA_EC_POINT and removes the DER encoding
  private byte[] getDecodedECPoint(ECPublicKey publicKey) {
    byte[] encodedPoint = DEROctetString.getInstance(getECPoint(publicKey)).getOctets();
    return curve.getCurve().decodePoint(encodedPoint).getEncoded(false);
  }

  // getPublicKey returns the raw decoded public key.
  private byte[] getPublicKey(ECPublicKey publicKey) {
    return getDecodedECPoint(publicKey);
  }

  // getAddress derives the address from a raw decoded public key
  private String getAddress(ECPublicKey publicKey) {
    byte[] publicKeyBytes = getPublicKey(publicKey);
    return Keys.toChecksumAddress(Keys.getAddress(Sign.publicFromPoint(publicKeyBytes)));
  }

  // isAddress checks whether or not a Ethereum address is valid
  private boolean isAddress(String address) {
    return address.matches("^(0x){1}[0-9a-fA-F]{40}$");
  }

  // setLabel updates the label in the token associated to the given object handle
  private void setLabel(Session session, Key objectHandle, String label) throws TokenException {
    Key obj = new Key();
    obj.getLabel().setCharArrayValue(label.toCharArray());
    session.setAttributeValues(objectHandle, obj);
  }

  // findObject returns the first match of the given template
  private PKCS11Object findObject(Session session, PKCS11Object template) throws TokenException {
    session.findObjectsInit(template);
    PKCS11Object[] objects = session.findObjects(1);
    session.findObjectsFinal();
    if (objects.length > 0) {
      return objects[0];
    }
    return null;
  }

  // findObjects returns all the matches to the given template
  private List<PKCS11Object> findObjects(Session session, PKCS11Object template)
      throws TokenException {
    List<PKCS11Object> result = new ArrayList<>();
    session.findObjectsInit(template);
    PKCS11Object[] objects = session.findObjects(1000);
    session.findObjectsFinal();
    result.addAll(Arrays.asList(objects));
    return result;
  }
}
