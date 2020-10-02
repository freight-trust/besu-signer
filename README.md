# BesuSigner


### Build AccountGenerator
```
./gradlew build
```
Go to the distribution directory:
```
cd build/distributions/
```
Expand the distribution archive:
```
tar -xzf accountgenerator-<version>.tar.gz
```
Move to the expanded folder and display the AccountGenerator help to confirm installation.
```
cd accountgenerator-<version>/
bin/accountgenerator --help
```
### Key generation with Keystore:
Start the service
```
accountgenerator --http-listen-host=127.0.0.1 --http-listen-port=8543 --logging="DEBUG" --directory="../keysAndPasswords" file-based-account-generator --password-file="../keysAndPasswords/passwordFile"  
```
Generate a new account
```
curl -w "\n" -X POST --data '{"jsonrpc":"2.0","method":"eth_generateAccount","params":[],"id":1}' http://127.0.0.1:8543
```
### Key generation with HSM:
Start the service
```
accountgenerator --http-listen-host=127.0.0.1 --http-listen-port=8543 --logging="DEBUG" --directory="../keysAndPasswords" hsm-account-generator --library="/usr/local/lib/softhsm/libsofthsm2.so" --slot-label="WALLET-001" --slot-pin="us3rs3cur3"  
```
Generate a new account
```
curl -w "\n" -X POST --data '{"jsonrpc":"2.0","method":"eth_generateAccount","params":[],"id":1}' http://127.0.0.1:8543
```
### Key generation with Cavium:
Start the service
```
accountgenerator --http-listen-host=127.0.0.1 --http-listen-port=8543 --logging="DEBUG" --directory="../keysAndPasswords" cavium-account-generator --library="/opt/cloudhsm/lib/libcloudhsm_pkcs11.so" --slot-pin="alice:391019314" --sas="../scripts/sas.sh"
```
Generate a new account
```
curl -w "\n" -X POST --data '{"jsonrpc":"2.0","method":"eth_generateAccount","params":[],"id":1}' http://127.0.0.1:8543
```


