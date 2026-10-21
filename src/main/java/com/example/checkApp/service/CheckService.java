package com.example.checkApp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.checkApp.dto.request.CheckRequest;
import com.example.checkApp.dto.response.CheckResponse;
import com.example.checkApp.entity.Check;
import com.example.checkApp.exception.NotFoundException;
import com.example.checkApp.repository.CheckRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CheckService {

    private static final String RESOURCE = "Check";

    private final CheckRepository checkRepository;

    public CheckResponse create(CheckRequest request) {
        Check check = new Check();
        apply(request, check);
        return toResponse(checkRepository.save(check));
    }

    @Transactional(readOnly = true)
    public List<CheckResponse> findAll() {
        return checkRepository.findAll().stream()
                .map(CheckService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CheckResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public CheckResponse update(Long id, CheckRequest request) {
        Check check = getOrThrow(id);
        apply(request, check);
        return toResponse(checkRepository.save(check));
    }

    public void delete(Long id) {
        Check check = getOrThrow(id);
        checkRepository.delete(check);
    }

    private Check getOrThrow(Long id) {
        return checkRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(RESOURCE, id));
    }

    private static void apply(CheckRequest request, Check check) {
        check.setName(request.name());
        check.setUrl(request.url());
        check.setCheckIntervalSeconds(request.checkIntervalSeconds());
        check.setEnabled(request.enabled());
    }

    private static CheckResponse toResponse(Check check) {
        return new CheckResponse(
                check.getId(),
                check.getName(),
                check.getUrl(),
                check.getCheckIntervalSeconds(),
                check.isEnabled(),
                check.getLastStatus(),
                check.getLastCheckedAt(),
                check.getCreatedAt()
        );
    }
}
