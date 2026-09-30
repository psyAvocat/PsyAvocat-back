package com.psyavocat.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalUtilisateurs;
    private long totalProfessionnels;
    private long professionnelsEnAttente;
    private long rendezVousMoisCourant;
}
