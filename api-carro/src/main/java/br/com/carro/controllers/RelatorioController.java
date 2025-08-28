package br.com.carro.controllers;

import br.com.carro.entities.Marca;
import br.com.carro.services.RelatorioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//
//Este é o controlador dedicado para os relatórios. Ele recebe a requisição com o
//formato e os filtros, e delega a tarefa de geração do arquivo para o RelatorioService.

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    @Autowired
    private RelatorioService relatorioService;

    // ✅ Endpoint para gerar o relatório de marcas em PDF, XLS, ou CSV
    @GetMapping("/marcas")
    @PreAuthorize("hasAnyRole('ADMIN','BASIC','GERENTE')")
    public ResponseEntity<byte[]> gerarRelatorioMarcas(
            @RequestParam String formato,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDir)
    {

        try {
            byte[] relatorioBytes = relatorioService.gerarRelatorioMarcas(formato, nome,sortField,sortDir);

            HttpHeaders headers = new HttpHeaders();
            String filename = "relatorio-marcas." + formato;

            // Define o tipo de conteúdo com base no formato
            if ("pdf".equalsIgnoreCase(formato)) {
                headers.setContentType(MediaType.APPLICATION_PDF);
            } else if ("xls".equalsIgnoreCase(formato)) {
                headers.setContentType(MediaType.valueOf("application/vnd.ms-excel"));
            } else if ("csv".equalsIgnoreCase(formato)) {
                headers.setContentType(MediaType.valueOf("text/csv"));
            }

            // Força o download do arquivo no navegador
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(relatorioBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
