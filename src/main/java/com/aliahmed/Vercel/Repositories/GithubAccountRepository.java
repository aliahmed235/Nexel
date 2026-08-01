package com.aliahmed.Vercel.Repositories;

import com.aliahmed.Vercel.entity.GithubAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GithubAccountRepository extends JpaRepository<GithubAccount, Long> {

    Optional<GithubAccount> findByUserId(Long userId);
}
