package com.ET.categorization.merchant;

import com.ET.categorization.dto.CategorizationResult;
import com.ET.categorization.dto.CategorizationSource;
import com.ET.categorization.normalization.MerchantNameNormalizer;
import com.ET.category.entity.Category;
import com.ET.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantLookupService {

    private final MerchantNameNormalizer normalizer;
    private final MerchantRepository merchantRepository;
    private final MerchantAliasRepository aliasRepository;
    private final CategoryRepository categoryRepository;

    public Optional<CategorizationResult> lookup(String merchantRaw) {
        if (merchantRaw == null || merchantRaw.isBlank()) return Optional.empty();

        String normalized = normalizer.normalize(merchantRaw);
        if (normalized.isBlank()) return Optional.empty();

        Optional<Merchant> merchant = merchantRepository.findByNormalizedName(normalized);
        if (merchant.isEmpty()) {
            merchant = aliasRepository.findByAlias(normalized).map(MerchantAlias::getMerchant);
        }

        return merchant.map(this::toResult);
    }

    @Transactional
    public Long findOrCreateMerchant(String merchantRaw, Long categoryId, int confidence) {
        if (merchantRaw == null || merchantRaw.isBlank() || categoryId == null) return null;
        String normalized = normalizer.normalize(merchantRaw);
        if (normalized.isBlank()) return null;

        Optional<Merchant> existing = merchantRepository.findByNormalizedName(normalized)
                .or(() -> aliasRepository.findByAlias(normalized).map(MerchantAlias::getMerchant));
        if (existing.isPresent()) return existing.get().getId();

        Merchant merchant = Merchant.builder()
                .normalizedName(normalized)
                .displayName(merchantRaw.trim())
                .categoryId(categoryId)
                .confidence(confidence)
                .userDefined(false)
                .build();
        merchant = merchantRepository.save(merchant);
        log.info("Learned new merchant '{}' -> categoryId={}", normalized, categoryId);
        return merchant.getId();
    }

    @Transactional
    public void recordUserOverride(String merchantRaw, Long categoryId) {
        if (merchantRaw == null || merchantRaw.isBlank() || categoryId == null) return;
        String normalized = normalizer.normalize(merchantRaw);
        if (normalized.isBlank()) return;

        Merchant merchant = merchantRepository.findByNormalizedName(normalized)
                .orElseGet(() -> Merchant.builder()
                        .normalizedName(normalized)
                        .displayName(merchantRaw.trim())
                        .build());

        merchant.setCategoryId(categoryId);
        merchant.setConfidence(CategorizationResult.USER_OVERRIDE_CONFIDENCE);
        merchant.setUserDefined(true);
        merchantRepository.save(merchant);
        log.info("Recorded user override: merchant '{}' -> categoryId={}", normalized, categoryId);
    }

    @Transactional
    public void addAlias(Merchant merchant, String aliasRaw) {
        if (merchant == null || aliasRaw == null || aliasRaw.isBlank()) return;
        String normalizedAlias = normalizer.normalize(aliasRaw);
        if (normalizedAlias.isBlank() || normalizedAlias.equals(merchant.getNormalizedName())) return;
        if (aliasRepository.findByAlias(normalizedAlias).isPresent()) return;

        aliasRepository.save(MerchantAlias.builder()
                .merchant(merchant)
                .alias(normalizedAlias)
                .build());
    }

    private CategorizationResult toResult(Merchant merchant) {
        String categoryName = categoryRepository.findById(merchant.getCategoryId())
                .map(Category::getName)
                .orElse("Other");
        CategorizationSource source = merchant.isUserDefined()
                ? CategorizationSource.USER_OVERRIDE
                : CategorizationSource.MERCHANT_DB;
        String reason = merchant.isUserDefined()
                ? "User previously categorized this merchant as " + categoryName
                : "Known merchant '" + merchant.getDisplayName() + "'";

        return new CategorizationResult(
                merchant.getId(), merchant.getCategoryId(), categoryName,
                merchant.getConfidence(), source, reason, merchant.getDisplayName()
        );
    }
}
