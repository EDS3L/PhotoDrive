package pl.photodrive.core.infrastructure.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;

interface JWSVerifierProvider { JWSVerifier forKid(String kid) throws JOSEException; }
