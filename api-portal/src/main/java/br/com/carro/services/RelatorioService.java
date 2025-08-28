package br.com.carro.services;

import br.com.carro.entities.Marca;
import br.com.carro.repositories.MarcaRepository;
import com.itextpdf.text.Document;

import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class RelatorioService {

    // Adicione a injeção do seu MarcaRepository
    private final MarcaRepository marcaRepository;

    @Autowired
    public RelatorioService( MarcaRepository marcaRepository) {
        // ... (seu construtor existente)
        this.marcaRepository = marcaRepository;
    }

    /**
     * ✅ Gerador principal de relatórios de Marcas.
     * Recebe o formato e o filtro e delega a lógica de geração.
     */
    public byte[] gerarRelatorioMarcas(
            String formato, String nome, String sortField, String sortDir) throws Exception {
        // ✅ Adicione esta linha de log
        System.out.println("Gerando relatório com filtro de nome: '" + nome + "'");

        List<Marca> marcas;

        // ✅ A ordenação é aplicada a todos os registros, não a uma página
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir != null ? sortDir : "asc"), sortField != null ? sortField : "id");


        // ✅ Use a lógica de filtro de nome para obter os dados do repositório
        if (nome != null && !nome.isBlank()) {
            // ✅ Usa o repositório para buscar a lista inteira, sem paginação
            marcas = marcaRepository.findByNomeContainingIgnoreCase(nome, sort);
        } else {
            // ✅ Usa o findAll com ordenação
            marcas = marcaRepository.findAll(sort);
        }

        // ✅ Adicione esta linha para verificar o tamanho da lista
        System.out.println("Número de marcas encontradas para o relatório: " + marcas.size());

        switch (formato.toLowerCase()) {
            case "pdf":
                return gerarMarcaPdf(marcas);
            case "xls":
                return gerarMarcaXls(marcas);
            case "csv":
                return gerarMarcaCsv(marcas);
            default:
                throw new IllegalArgumentException("Formato de relatório inválido.");
        }
    }

    // ✅ Métodos privados para cada formato de relatório
    public byte[] gerarMarcaCsv(List<Marca> marcas) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            String[] header = {"Modelo", "Marca", "Ano"};
            writer.writeNext(header);

            for (Marca marca : marcas) {
                writer.writeNext(new String[]{
                        marca.getNome(),
                });
            }
            writer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            // Tratar a exceção
        }
        return null;
    }

    public byte[] gerarMarcaXls(List<Marca> marcas) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Relatório de Carros");

            // Cria o cabeçalho
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Modelo", "Marca", "Ano"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Preenche os dados
            int rowNum = 1;
            for (Marca marca : marcas) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(marca.getId());
                row.createCell(1).setCellValue(marca.getNome());
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            // Tratar a exceção
        }
        return null;
    }

    public byte[] gerarMarcaPdf(List<Marca> marcas) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            document.add(new Paragraph("Relatório de Marcas"));
            document.add(new Paragraph(" "));

            // ✅ Criação da tabela com 2 colunas
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            // Adiciona o cabeçalho da tabela
            table.addCell(new PdfPCell(new Paragraph("ID")));
            table.addCell(new PdfPCell(new Paragraph("Nome da Marca")));

            // Preenche a tabela com os dados das marcas
            for (Marca marca : marcas) {
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(marca.getId()))));
                table.addCell(new PdfPCell(new Paragraph(marca.getNome())));
            }

            // Adiciona a tabela ao documento
            document.add(table);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
