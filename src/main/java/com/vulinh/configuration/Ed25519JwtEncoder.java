package com.vulinh.configuration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.stereotype.Component;

@Component
public class Ed25519JwtEncoder implements JwtEncoder {

  private final KeyPair edKeyPair;
  private final OctetKeyPair edJwkPair;
  private final ApplicationProperties applicationProperties;

  public Ed25519JwtEncoder(ApplicationProperties applicationProperties)
      throws NoSuchAlgorithmException {
    this.applicationProperties = applicationProperties;
    var signingKey = applicationProperties.security().signingKey();
    this.edKeyPair = KeyPairGenerator.getInstance(signingKey).generateKeyPair();
    this.edJwkPair = buildOctetKeyPair(this.edKeyPair, signingKey);
  }

  OctetKeyPair getEdJwkPair() {
    return edJwkPair;
  }

  @Override
  public Jwt encode(JwtEncoderParameters parameters) throws JwtEncodingException {
    var claims = parameters.getClaims();
    var algorithm = applicationProperties.security().signingKey();
    var keyId = edJwkPair.getKeyID();

    try {
      var jwsHeader = new JWSHeader.Builder(resolveJWSAlgorithm(algorithm)).keyID(keyId).build();

      var claimsBuilder = new JWTClaimsSet.Builder();
      claims
          .getClaims()
          .forEach(
              (key, value) ->
                  claimsBuilder.claim(
                      key, value instanceof Instant instant ? Date.from(instant) : value));

      var headerB64 = jwsHeader.toBase64URL();
      var payloadB64 = new Payload(claimsBuilder.build().toJSONObject()).toBase64URL();
      var signingInput = headerB64 + "." + payloadB64;

      var signer = Signature.getInstance(algorithm);
      signer.initSign(edKeyPair.getPrivate());
      signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));

      return new Jwt(
          "%s.%s".formatted(signingInput, Base64URL.encode(signer.sign())),
          claims.getIssuedAt(),
          claims.getExpiresAt(),
          Map.of("alg", algorithm, "kid", keyId),
          claims.getClaims());
    } catch (Exception e) {
      throw new JwtEncodingException(
          "Failed to sign JWT with " + algorithm + ": " + e.getMessage(), e);
    }
  }

  static JWSAlgorithm resolveJWSAlgorithm(String name) {
    return switch (name) {
      case "Ed25519" -> JWSAlgorithm.Ed25519;
      case "Ed448" -> JWSAlgorithm.Ed448;
      default -> throw new IllegalArgumentException("Unsupported algorithm: " + name);
    };
  }

  private static OctetKeyPair buildOctetKeyPair(KeyPair keyPair, String signingKey)
      throws NoSuchAlgorithmException {
    var jwsAlgorithm = resolveJWSAlgorithm(signingKey);

    if (!(keyPair.getPublic() instanceof EdECPublicKey publicKey)
        || !(keyPair.getPrivate() instanceof EdECPrivateKey privateKey)) {
      throw new NoSuchAlgorithmException(signingKey + " key pair has unexpected type");
    }

    var publicBytes = publicKey.getEncoded();

    return new OctetKeyPair.Builder(
            jwsAlgorithm.equals(JWSAlgorithm.Ed25519) ? Curve.Ed25519 : Curve.Ed448,
            Base64URL.encode(
                Arrays.copyOfRange(publicBytes, publicBytes.length - 32, publicBytes.length)))
        .d(
            Base64URL.encode(
                privateKey
                    .getBytes()
                    .orElseThrow(
                        () ->
                            new NoSuchAlgorithmException(
                                "Cannot extract " + signingKey + " private key bytes"))))
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.randomUUID().toString())
        .algorithm(jwsAlgorithm)
        .build();
  }
}
