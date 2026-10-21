package com.example.checkApp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.example.checkApp.entity.Check;
import com.example.checkApp.entity.CheckResult;
import com.example.checkApp.model.CheckStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CheckRepository checkRepository;

    @Autowired
    private CheckResultRepository checkResultRepository;

    private Check newCheck(String name, String url) {
        Check check = new Check();
        check.setName(name);
        check.setUrl(url);
        return check;
    }

    @Test
    void savesAndLoadsCheck() {
        Check saved = checkRepository.save(newCheck("google", "https://google.com"));
        entityManager.flush();
        entityManager.clear();

        Optional<Check> loaded = checkRepository.findById(saved.getId());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getName()).isEqualTo("google");
        assertThat(loaded.get().isEnabled()).isTrue();
        assertThat(loaded.get().getCheckIntervalSeconds()).isEqualTo(60);
        assertThat(loaded.get().getCreatedAt()).isNotNull();
    }

    @Test
    void findByEnabledTrueSkipsDisabled() {
        checkRepository.save(newCheck("a", "https://a.example"));
        Check disabled = newCheck("b", "https://b.example");
        disabled.setEnabled(false);
        checkRepository.save(disabled);
        entityManager.flush();

        List<Check> enabled = checkRepository.findByEnabledTrue();
        assertThat(enabled).hasSize(1);
        assertThat(enabled.get(0).getName()).isEqualTo("a");
    }

    @Test
    void checkResultReferencesCheck() {
        Check check = checkRepository.save(newCheck("google", "https://google.com"));

        CheckResult result = new CheckResult();
        result.setCheck(check);
        result.setStatus(CheckStatus.UP);
        result.setHttpCode(200);
        result.setResponseTimeMs(42L);
        checkResultRepository.save(result);
        entityManager.flush();
        entityManager.clear();

        List<CheckResult> results = checkResultRepository.findTop20ByCheckIdOrderByCheckedAtDesc(check.getId());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(CheckStatus.UP);
        assertThat(results.get(0).getHttpCode()).isEqualTo(200);
        assertThat(results.get(0).getCheckedAt()).isNotNull();
    }

    @Test
    void downResultStoresErrorWithoutHttpCode() {
        Check check = checkRepository.save(newCheck("broken", "https://broken.example"));

        CheckResult result = new CheckResult();
        result.setCheck(check);
        result.setStatus(CheckStatus.DOWN);
        result.setErrorMessage("Connect timed out");
        result.setCheckedAt(Instant.now());
        checkResultRepository.save(result);
        entityManager.flush();
        entityManager.clear();

        CheckResult loaded = checkResultRepository.findTop20ByCheckIdOrderByCheckedAtDesc(check.getId()).get(0);
        assertThat(loaded.getStatus()).isEqualTo(CheckStatus.DOWN);
        assertThat(loaded.getHttpCode()).isNull();
        assertThat(loaded.getErrorMessage()).isEqualTo("Connect timed out");
    }

    @Test
    void deleteCheckCascadesResults() {
        Check check = checkRepository.saveAndFlush(newCheck("google", "https://google.com"));
        CheckResult result = new CheckResult();
        result.setCheck(check);
        result.setStatus(CheckStatus.UP);
        checkResultRepository.save(result);
        entityManager.flush();
        entityManager.clear();

        checkRepository.deleteById(check.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(checkResultRepository.findAll()).isEmpty();
    }
}
