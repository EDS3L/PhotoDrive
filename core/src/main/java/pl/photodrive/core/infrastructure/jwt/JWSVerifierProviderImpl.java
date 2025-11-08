package pl.photodrive.core.infrastructure.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JWSVerifierProviderImpl implements JWSVerifierProvider {

    private final JWSSigner signer;

    @Override
    public JWSVerifier forKid(String kid) throws JOSEException {

        if (signer instanceof com.nimbusds.jose.crypto.MACSigner macSigner) {
            byte[] secret = macSigner.getSecret();
            return new MACVerifier(secret);
        }
        throw new JOSEException("Unsupported signer type: " + signer.getClass().getName());
    }
}