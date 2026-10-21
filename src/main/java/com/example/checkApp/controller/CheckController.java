package com.example.checkApp.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.checkApp.dto.request.CheckRequest;
import com.example.checkApp.dto.response.CheckResponse;
import com.example.checkApp.service.CheckService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/checks")
@RequiredArgsConstructor
public class CheckController {

    private final CheckService checkService;

    @PostMapping
    public ResponseEntity<CheckResponse> create(@Valid @RequestBody CheckRequest request) {
        CheckResponse response = checkService.create(request);
        return ResponseEntity
                .created(URI.create("/api/checks/" + response.id()))
                .body(response);
    }

    @GetMapping
    public List<CheckResponse> findAll() {
        return checkService.findAll();
    }

    @GetMapping("/{id}")
    public CheckResponse findById(@PathVariable Long id) {
        return checkService.findById(id);
    }

    @PutMapping("/{id}")
    public CheckResponse update(@PathVariable Long id, @Valid @RequestBody CheckRequest request) {
        return checkService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        checkService.delete(id);
    }
}
