package io.github.khaytul_illia.inventory_manager_api.security.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
@Slf4j
public class JwtRsaPemKeyConfig {

    @Bean
    public RSAPrivateKey jwtPrivateKey(
        @Value("${spring.application.security.jwt.private_key_source}") Resource jwtPrivateKeySource
    ) throws Exception {
        log.debug("Loading JWT private key from '{}'", jwtPrivateKeySource);

        String privateKeyValue = loadKeyValue(jwtPrivateKeySource);
        byte[] decodedKey = Base64.getDecoder().decode(privateKeyValue);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);

        log.info("Successfully loaded JWT private key");

        return privateKey;
    }

    @Bean
    public RSAPublicKey jwtPublicKey(
        @Value("${spring.application.security.jwt.public_key_source}") Resource jwtPublicKeySource
    ) throws Exception {
        log.debug("Loading JWT public key from '{}'", jwtPublicKeySource);

        String publicKeyValue = loadKeyValue(jwtPublicKeySource);
        byte[] decodedKey = Base64.getDecoder().decode(publicKeyValue);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);

        log.info("Successfully loaded JWT public key");

        return publicKey;
    }

    private String loadKeyValue(Resource keySource) throws Exception {
        try (InputStream is = keySource.getInputStream()) {
            byte[] keyBytes = is.readAllBytes();

            return new String(keyBytes)
                .replaceAll("-----BEGIN [^-]+ KEY-----", "")
                .replaceAll("-----END [^-]+ KEY-----", "")
                .replaceAll("\\s", "");
        }
    }

}
