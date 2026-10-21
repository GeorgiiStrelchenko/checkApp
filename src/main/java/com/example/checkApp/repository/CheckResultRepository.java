package com.example.checkApp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.checkApp.entity.CheckResult;

public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

    List<CheckResult> findTop20ByCheckIdOrderByCheckedAtDesc(Long checkId);

    Page<CheckResult> findByCheckId(Long checkId, Pageable pageable);
}
