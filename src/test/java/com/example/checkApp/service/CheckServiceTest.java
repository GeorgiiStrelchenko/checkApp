package com.example.checkApp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.checkApp.dto.request.CheckRequest;
import com.example.checkApp.dto.response.CheckResponse;
import com.example.checkApp.entity.Check;
import com.example.checkApp.entity.User;
import com.example.checkApp.exception.NotFoundException;
import com.example.checkApp.repository.CheckRepository;

@ExtendWith(MockitoExtension.class)
class CheckServiceTest {

    @Mock
    private CheckRepository checkRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private CheckService checkService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        lenient().when(currentUserService.getUser()).thenReturn(alice);
    }

    private CheckRequest request() {
        return new CheckRequest("google", "https://google.com", 60, true);
    }

    private Check entity(Long id) {
        Check check = new Check();
        check.setId(id);
        check.setName("google");
        check.setUrl("https://google.com");
        check.setCheckIntervalSeconds(60);
        check.setEnabled(true);
        check.setUser(alice);
        return check;
    }

    @Test
    void createSavesAndMaps() {
        when(checkRepository.save(any(Check.class))).thenAnswer(inv -> {
            inv.getArgument(0, Check.class).setId(1L);
            return inv.getArgument(0, Check.class);
        });

        CheckResponse response = checkService.create(request());

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("google");
        assertThat(response.url()).isEqualTo("https://google.com");
        assertThat(response.enabled()).isTrue();
        verify(checkRepository).save(any(Check.class));
    }

    @Test
    void createAssignsCurrentUserAsOwner() {
        when(checkRepository.save(any(Check.class))).thenAnswer(inv -> {
            inv.getArgument(0, Check.class).setId(1L);
            return inv.getArgument(0, Check.class);
        });

        checkService.create(request());

        verify(checkRepository).save(org.mockito.ArgumentMatchers.argThat(
                check -> check.getUser() == alice));
    }

    @Test
    void findAllReturnsOnlyOwnChecks() {
        when(checkRepository.findByUserId(1L)).thenReturn(List.of(entity(1L), entity(2L)));

        List<CheckResponse> all = checkService.findAll();

        assertThat(all).hasSize(2);
        assertThat(all.get(0).id()).isEqualTo(1L);
        verify(checkRepository).findByUserId(1L);
    }

    @Test
    void findByIdReturnsMapped() {
        when(checkRepository.findById(1L)).thenReturn(Optional.of(entity(1L)));

        assertThat(checkService.findById(1L).name()).isEqualTo("google");
    }

    @Test
    void findByIdThrows404WhenMissing() {
        when(checkRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkService.findById(404L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Check with id 404 not found");
    }

    @Test
    void findByIdThrows404WhenOwnedByAnotherUser() {
        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        Check foreign = entity(1L);
        foreign.setUser(bob);
        when(checkRepository.findById(1L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> checkService.findById(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateAppliesNewValues() {
        Check existing = entity(1L);
        when(checkRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(checkRepository.save(existing)).thenReturn(existing);

        CheckResponse response = checkService.update(1L,
                new CheckRequest("bing", "https://bing.com", 120, false));

        assertThat(response.name()).isEqualTo("bing");
        assertThat(response.url()).isEqualTo("https://bing.com");
        assertThat(response.checkIntervalSeconds()).isEqualTo(120);
        assertThat(response.enabled()).isFalse();
    }

    @Test
    void deleteRemovesExisting() {
        Check existing = entity(1L);
        when(checkRepository.findById(1L)).thenReturn(Optional.of(existing));

        checkService.delete(1L);

        verify(checkRepository).delete(existing);
    }

    @Test
    void deleteThrows404WhenMissing() {
        when(checkRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkService.delete(404L))
                .isInstanceOf(NotFoundException.class);

        verify(checkRepository, never()).delete(any());
    }
}
