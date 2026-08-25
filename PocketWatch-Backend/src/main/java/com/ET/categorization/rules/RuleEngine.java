package com.ET.categorization.rules;

import com.ET.categorization.dto.CategorizationResult;
import com.ET.categorization.dto.CategorizationSource;
import com.ET.categorization.dto.ExtractedMerchantInfo;
import com.ET.category.entity.Category;
import com.ET.category.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleEngine {

    private final CategoryRuleRepository ruleRepository;
    private final CategoryRepository categoryRepository;

    private volatile List<CategoryRule> cachedRules = List.of();
    private volatile Map<Long, String> categoryNamesById = Map.of();
    private volatile Long transfersCategoryId = null;

    private static final Pattern PERSONAL_HONORIFIC =
            Pattern.compile("^(MR|MRS|MISS|MS|DR)\\.?\\s*[A-Z]");
    private static final Pattern BANK_ACCOUNT_PLACEHOLDER =
            Pattern.compile("^BANK\\s*ACC(OUNT)?$");

    @PostConstruct
    public void refresh() {
        cachedRules = ruleRepository.findAllByOrderByPriorityDesc();
        categoryNamesById = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        transfersCategoryId = categoryRepository.findAll().stream()
                .filter(c -> "Transfers".equalsIgnoreCase(c.getName()))
                .map(Category::getId)
                .findFirst()
                .orElse(null);
        log.info("Rule engine loaded {} rules", cachedRules.size());
    }

    public Optional<CategorizationResult> match(ExtractedMerchantInfo extracted) {
        Optional<CategorizationResult> personalTransfer = matchPersonalTransfer(extracted);
        if (personalTransfer.isPresent()) return personalTransfer;

        String haystack = buildHaystack(extracted);
        if (haystack.isBlank()) return Optional.empty();

        for (CategoryRule rule : cachedRules) {
            if (haystack.contains(rule.getKeyword().toUpperCase())) {
                String categoryName = categoryNamesById.getOrDefault(rule.getCategoryId(), "Other");
                return Optional.of(new CategorizationResult(
                        null,
                        rule.getCategoryId(),
                        categoryName,
                        CategorizationResult.RULE_ENGINE_CONFIDENCE,
                        CategorizationSource.RULE_ENGINE,
                        "Matched rule keyword '" + rule.getKeyword() + "'",
                        extracted.merchantRaw()
                ));
            }
        }
        return Optional.empty();
    }

    private Optional<CategorizationResult> matchPersonalTransfer(ExtractedMerchantInfo extracted) {
        if (transfersCategoryId == null || extracted.merchantRaw() == null) return Optional.empty();

        String merchant = extracted.merchantRaw().trim().toUpperCase();
        boolean isHonorific = PERSONAL_HONORIFIC.matcher(merchant).find();
        boolean isBankAccPlaceholder = BANK_ACCOUNT_PLACEHOLDER.matcher(merchant).matches();
        if (!isHonorific && !isBankAccPlaceholder) return Optional.empty();

        String reason = isBankAccPlaceholder
                ? "Transfer to a bank account (no named payee)"
                : "Personal UPI transfer to an individual";

        return Optional.of(new CategorizationResult(
                null, transfersCategoryId, "Transfers",
                CategorizationResult.RULE_ENGINE_CONFIDENCE,
                CategorizationSource.RULE_ENGINE, reason, extracted.merchantRaw()
        ));
    }

    private String buildHaystack(ExtractedMerchantInfo extracted) {
        StringBuilder sb = new StringBuilder();
        if (extracted.merchantRaw() != null) sb.append(extracted.merchantRaw().toUpperCase()).append(' ');
        if (extracted.cleanedDescription() != null) sb.append(extracted.cleanedDescription().toUpperCase());
        return sb.toString();
    }
}
