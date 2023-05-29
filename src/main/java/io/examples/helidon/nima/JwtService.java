package io.examples.helidon.nima;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class JwtService {
    public static boolean verify(String token, String jwkSourceUrl) {
        try {
            DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            JWKSource<SecurityContext> keySource = JWKSourceBuilder.create(URI.create(jwkSourceUrl).toURL()).build();
            JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
            jwtProcessor.setJWSKeySelector(keySelector);
            JWTClaimsSet claimsSet = jwtProcessor.process(token, null);
            log.info(claimsSet.toJSONObject().toString());
            return true;
        } catch (ParseException | BadJOSEException | JOSEException | MalformedURLException e) {
            log.error(e.getMessage());
            return false;
        }
    }
}
