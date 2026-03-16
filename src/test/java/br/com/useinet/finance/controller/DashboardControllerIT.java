package br.com.useinet.finance.controller;

import br.com.useinet.finance.dto.AuthResponse;
import br.com.useinet.finance.dto.DashboardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DashboardControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    private String token;

    @BeforeEach
    void setUp() {
        var register = Map.of("nome", "Dashboard User", "email", "dashboarduser@it.com", "senha", "senha1234");
        ResponseEntity<AuthResponse> auth = restTemplate.postForEntity("/auth/register", register, AuthResponse.class);
        if (auth.getBody() != null && auth.getBody().getToken() != null) {
            token = auth.getBody().getToken();
        } else {
            var login = Map.of("email", "dashboarduser@it.com", "senha", "senha1234");
            token = restTemplate.postForEntity("/auth/login", login, AuthResponse.class).getBody().getToken();
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void getDashboard_shouldReturn401WithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/dashboard", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getDashboard_shouldReturn200WithZeroesForNewUser() {
        ResponseEntity<DashboardResponse> response = restTemplate.exchange(
                "/dashboard", HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                DashboardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSaldo()).isEqualTo(0.0);
        assertThat(response.getBody().getTotalReceitas()).isEqualTo(0.0);
        assertThat(response.getBody().getTotalDespesas()).isEqualTo(0.0);
        assertThat(response.getBody().getUltimasTransacoes()).isEmpty();
        assertThat(response.getBody().getDespesasPorCategoria()).isEmpty();
        assertThat(response.getBody().getContas()).isEmpty();
    }

    @Test
    void getDashboard_shouldReflectCreatedTransactions() {
        restTemplate.exchange("/transactions", HttpMethod.POST,
                new HttpEntity<>(Map.of("descricao", "Salário", "valor", 5000.0, "tipo", "RECEITA"), authHeaders()),
                Object.class);
        restTemplate.exchange("/transactions", HttpMethod.POST,
                new HttpEntity<>(Map.of("descricao", "Aluguel", "valor", 1500.0, "tipo", "DESPESA"), authHeaders()),
                Object.class);

        ResponseEntity<DashboardResponse> response = restTemplate.exchange(
                "/dashboard", HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                DashboardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTotalReceitas()).isEqualTo(5000.0);
        assertThat(response.getBody().getTotalDespesas()).isEqualTo(1500.0);
        assertThat(response.getBody().getSaldo()).isEqualTo(3500.0);
        assertThat(response.getBody().getUltimasTransacoes()).hasSize(2);
    }

    @Test
    void getDashboard_shouldIncludeContas() {
        restTemplate.exchange("/contas", HttpMethod.POST,
                new HttpEntity<>(Map.of("nome", "Nubank", "saldo", 2000.0), authHeaders()),
                Object.class);

        ResponseEntity<DashboardResponse> response = restTemplate.exchange(
                "/dashboard", HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                DashboardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContas()).hasSize(1);
        assertThat(response.getBody().getContas().get(0).getNome()).isEqualTo("Nubank");
        assertThat(response.getBody().getContas().get(0).getSaldo()).isEqualTo(2000.0);
    }

    @Test
    void getDashboard_shouldNotShowOtherUsersData() {
        var register2 = Map.of("nome", "Other", "email", "other_dash@it.com", "senha", "senha1234");
        String otherToken = restTemplate.postForEntity("/auth/register", register2, AuthResponse.class)
                .getBody().getToken();
        HttpHeaders otherHeaders = new HttpHeaders();
        otherHeaders.setBearerAuth(otherToken);

        restTemplate.exchange("/transactions", HttpMethod.POST,
                new HttpEntity<>(Map.of("descricao", "Other income", "valor", 9999.0, "tipo", "RECEITA"), otherHeaders),
                Object.class);

        ResponseEntity<DashboardResponse> response = restTemplate.exchange(
                "/dashboard", HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                DashboardResponse.class);

        assertThat(response.getBody().getTotalReceitas()).isEqualTo(0.0);
        assertThat(response.getBody().getUltimasTransacoes()).isEmpty();
    }
}
