package pe.edu.vallegrande.app.service;

import java.time.LocalDate;

import pe.edu.vallegrande.app.dto.response.DashboardResponse;

public interface DashboardService {

    DashboardResponse resumen(LocalDate fecha);
}
