package com.fleet.central.controller;

import com.fleet.central.dto.DemandPredictionDto;
import com.fleet.central.dto.RebalanceProposalRequest;
import com.fleet.central.dto.RebalanceTaskDto;
import com.fleet.central.service.RebalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Rebalance & Analytics", description = "Autonomous AI rebalancing proposals and demand forecasting")
public class RebalanceController {

    private final RebalanceService rebalanceService;

    public RebalanceController(RebalanceService rebalanceService) {
        this.rebalanceService = rebalanceService;
    }

    @GetMapping("/rebalance/pending")
    @Operation(summary = "Get list of pending rebalancing proposals")
    public ResponseEntity<List<RebalanceTaskDto>> getPendingProposals() {
        return ResponseEntity.ok(rebalanceService.getPendingProposals());
    }

    @PostMapping("/rebalance/{id}/approve")
    @Operation(summary = "Approve an AI rebalancing proposal and dispatch transfer command")
    public ResponseEntity<RebalanceTaskDto> approveProposal(@PathVariable String id) {
        return ResponseEntity.ok(rebalanceService.approveProposal(id));
    }

    @PostMapping("/rebalance/propose")
    @Operation(summary = "Submit a new rebalancing proposal (used by AI agent or MCP server)")
    public ResponseEntity<RebalanceTaskDto> proposeRebalance(@RequestBody RebalanceProposalRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String proposedBy = (auth != null && auth.getName() != null) ? auth.getName() : "AUTONOMOUS_AGENT";
        RebalanceTaskDto created = rebalanceService.proposeRebalance(request, proposedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/analytics/demand")
    @Operation(summary = "Predict demand for a station at a given hour")
    public ResponseEntity<DemandPredictionDto> getDemandPrediction(
            @RequestParam(defaultValue = "station_a") String stationId,
            @RequestParam(defaultValue = "8") int targetHour) {
        return ResponseEntity.ok(rebalanceService.predictDemand(stationId, targetHour));
    }
}
