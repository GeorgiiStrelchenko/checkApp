package com.example.checkApp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.checkApp.entity.Check;

public interface CheckRepository extends JpaRepository<Check, Long> {

    List<Check> findByEnabledTrue();

    List<Check> findByUserId(Long userId);
}
