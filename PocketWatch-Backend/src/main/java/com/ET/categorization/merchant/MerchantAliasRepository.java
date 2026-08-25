package com.ET.categorization.merchant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantAliasRepository extends JpaRepository<MerchantAlias, Long> {
    Optional<MerchantAlias> findByAlias(String alias);
}
