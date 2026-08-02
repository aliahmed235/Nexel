package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.Repositories.GithubAccountRepository;
import com.aliahmed.Vercel.dto.GithubTokenResponse;
import com.aliahmed.Vercel.entity.GithubAccount;
import com.aliahmed.Vercel.entity.User;
import com.aliahmed.Vercel.exception.GithubOAuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GithubAccountService {

    private final GithubAccountRepository repository;
    private final CryptoService cryptoService;

    @Transactional
    public void storeToken(User user, GithubTokenResponse token) {
        GithubAccount account = repository.findByUserId(user.getId())
                .orElseGet(() -> GithubAccount.builder().user(user).build());

        account.setAccessTokenEnc(cryptoService.encrypt(token.accessToken()));
        account.setTokenType(token.tokenType());
        account.setScope(token.scope());
        repository.save(account);
    }

    /** Decrypts the stored token so another service can call GitHub as this user. */
    @Transactional(readOnly = true)
    public String accessTokenFor(Long userId) {
        return repository.findByUserId(userId)
                .map(account -> cryptoService.decrypt(account.getAccessTokenEnc()))
                .orElseThrow(() -> new GithubOAuthException("No GitHub account linked to user " + userId));
    }
}
