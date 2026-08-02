package com.aliahmed.Vercel.Controllers;

import com.aliahmed.Vercel.Services.AuthService;
import com.aliahmed.Vercel.config.AppProperties;
import com.aliahmed.Vercel.dto.AuthTokenResponse;
import com.aliahmed.Vercel.dto.CurrentUserResponse;
import com.aliahmed.Vercel.dto.ExchangeCodeRequest;
import com.aliahmed.Vercel.entity.User;
import com.aliahmed.Vercel.mapper.UserMapper;
import com.aliahmed.Vercel.util.CookieUtils;
import com.aliahmed.Vercel.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Optional;

/**
 * HTTP edge of the login flow. Everything here is redirects, cookies and
 * status codes — the decisions live in {@link AuthService}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserMapper userMapper;
    private final AppProperties properties;

    /** Entry point. Sends the browser to GitHub's consent screen. */
    @GetMapping("/github/authorize")
    public ResponseEntity<Void> authorize() {
        String state = authService.generateState();
        AppProperties.OAuthState config = properties.getOauthState();

        ResponseCookie cookie = CookieUtils.build(
                config.getCookieName(), state, config.getCookiePath(),
                config.getTtl(), config.isCookieSecure());

        return ResponseEntity.status(302)
                .location(URI.create(authService.buildAuthorizeUrl(state)))
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    /**
     * GitHub sends the browser back here. The state cookie set above rides
     * along and must match the state in the query string.
     */
    @GetMapping("/github/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error,
                                         HttpServletRequest request) {

        AppProperties.OAuthState config = properties.getOauthState();
        ResponseCookie clearState = CookieUtils.expire(
                config.getCookieName(), config.getCookiePath(), config.isCookieSecure());

        if (error != null) {
            log.warn("GitHub returned an OAuth error: {}", error);
            return redirect(authService.buildFrontendErrorRedirect("github_denied"), clearState);
        }
        if (code == null || state == null) {
            return redirect(authService.buildFrontendErrorRedirect("missing_parameters"), clearState);
        }

        Optional<String> expectedState = CookieUtils.read(request, config.getCookieName());
        if (expectedState.isEmpty() || !SecurityUtils.constantTimeEquals(expectedState.get(), state)) {
            log.warn("OAuth state mismatch — rejecting callback");
            return redirect(authService.buildFrontendErrorRedirect("invalid_state"), clearState);
        }

        try {
            String oneTimeCode = authService.completeGithubLogin(code);
            return redirect(authService.buildFrontendRedirect(oneTimeCode), clearState);
        } catch (RuntimeException e) {
            log.error("GitHub login failed", e);
            return redirect(authService.buildFrontendErrorRedirect("login_failed"), clearState);
        }
    }

    /**
     * Trades the one-time code from the redirect for the session token.
     *
     * <p>Called by the frontend, once, immediately on landing. The code is
     * burned on first use and expires within a minute either way.
     */
    @PostMapping("/exchange")
    public ResponseEntity<AuthTokenResponse> exchange(@RequestBody ExchangeCodeRequest request) {
        return ResponseEntity.ok(authService.exchangeCode(request.code()));
    }

    /** Who am I? The first endpoint the frontend calls with its new token. */
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userMapper.toCurrentUserResponse(user));
    }

    private ResponseEntity<Void> redirect(String location, ResponseCookie cookie) {
        return ResponseEntity.status(302)
                .location(URI.create(location))
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}
