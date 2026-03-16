package br.com.useinet.finance.controller;

import br.com.useinet.finance.dto.DashboardResponse;
import br.com.useinet.finance.model.Usuario;
import br.com.useinet.finance.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Resumo financeiro do usuário")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Resumo financeiro", description = "Retorna saldo, totais, últimas 10 transações e despesas por categoria")
    public ResponseEntity<DashboardResponse> getDashboard(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(dashboardService.getDashboard(usuario));
    }
}