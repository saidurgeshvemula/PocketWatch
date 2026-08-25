package com.ET.categorization.rules;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRuleRepository extends JpaRepository<CategoryRule, Long> {
    List<CategoryRule> findAllByOrderByPriorityDesc();
}
