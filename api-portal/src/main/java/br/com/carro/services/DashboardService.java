package br.com.carro.services;

import br.com.carro.entities.Dashboard.DashboardMetricsDTO;
import br.com.carro.repositories.CarroRepository;
import br.com.carro.repositories.MarcaRepository;
import br.com.carro.repositories.ProprietarioRepository;
import br.com.carro.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final CarroRepository carroRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProprietarioRepository proprietarioRepository;
    private final MarcaRepository marcaRepository;

    @Autowired
    public DashboardService(CarroRepository carroRepository, UsuarioRepository usuarioRepository,
                            ProprietarioRepository proprietarioRepository, MarcaRepository marcaRepository) {
        this.carroRepository = carroRepository;
        this.usuarioRepository = usuarioRepository;
        this.proprietarioRepository = proprietarioRepository;
        this.marcaRepository = marcaRepository;
    }

    public DashboardMetricsDTO getMetrics() {
        long totalCarros = carroRepository.count();
        long totalUsuarios = usuarioRepository.count();
        long totalMarcas = marcaRepository.count();
        long totalProprietarios = proprietarioRepository.count();


        return new DashboardMetricsDTO(totalCarros, totalUsuarios, totalMarcas,totalProprietarios );
    }
}
