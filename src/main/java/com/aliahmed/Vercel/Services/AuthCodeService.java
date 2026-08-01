package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.Repositories.AuthCodeRepository;
import com.aliahmed.Vercel.config.AppProperties;
import com.aliahmed.Vercel.entity.AuthCode;
import com.aliahmed.Vercel.entity.User;
import com.aliahmed.Vercel.exception.InvalidAuthCodeException;
import com.aliahmed.Vercel.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Issues and redeems the one-time codes that stand in for a JWT during the
 * redirect back to the frontend.
 */
@Service
public class AuthCodeService {

    private static final int CODE_BYTES = 32;

    private final AuthCodeRepository repository;
    private final AppProperties properties;

    public AuthCodeService(AuthCodeRepository repository, AppProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Returns the plaintext code. Only its hash is stored. */
    @Transactional
    public String issue(User user) {
        String code = SecurityUtils.generateSecureToken(CODE_BYTES);

        repository.save(AuthCode.builder()
                .codeHash(SecurityUtils.sha256Hex(code))
                .user(user)
                .expiresAt(Instant.now().plus(properties.getAuthCode().getTtl()))
                .build());

        return code;
    }

    /**
     * Burns the code and returns its owner. Throws if the code is unknown,
     * expired, or already used — the caller cannot tell which, deliberately.
     */
    @Transactional
    public User redeem(String code) {
        String codeHash = SecurityUtils.sha256Hex(code);

        if (repository.markUsed(codeHash, Instant.now()) != 1) {
            throw new InvalidAuthCodeException("This login code is invalid, expired, or already used.");
        }

        return repository.findByCodeHash(codeHash)
                .map(AuthCode::getUser)
                .orElseThrow(() -> new InvalidAuthCodeException("This login code is no longer valid."));
    }

    /** Housekeeping for rows whose codes can no longer be redeemed. */
    @Transactional
    public int purgeExpired() {
        return repository.deleteExpiredBefore(Instant.now().minusSeconds(3600));
    }
}
