package br.com.carro.services;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.ArquivoRepository;
import org.springframework.beans.factory.annotation.Value; // ✅ Importe a anotação @Value
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ArquivoService {

    private final ArquivoRepository arquivoRepository;
    private final PastaService pastaService;
    private String uploadDir = ""; // ✅ Configurar no application.properties

    @Autowired
    public ArquivoService(ArquivoRepository arquivoRepository, PastaService pastaService) {
        this.arquivoRepository = arquivoRepository;
        this.pastaService = pastaService;

        this.uploadDir = uploadDir;

        // ✅ Cria o diretório de uploads se ele não existir
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Salva o arquivo fisicamente no servidor e seus metadados no banco de dados.
     * @param file O arquivo a ser salvo.
     * @param pastaId O ID da pasta onde o arquivo será armazenado.
     * @param usuario O usuário que está enviando o arquivo.
     * @return O objeto Arquivo salvo no banco de dados.
     */
    public Arquivo salvarArquivo(MultipartFile file, Long pastaId, Usuario usuario) throws IOException {
        Pasta pasta = pastaService.buscarPorId(pastaId);

        // TODO: Implementar a lógica de permissão (ver se o usuário tem acesso à pasta)
        // if (!pasta.getUsuariosComPermissao().contains(usuario) && !usuario.hasRole("ADMIN")) {
        //     throw new AccessDeniedException("Você não tem permissão para adicionar arquivos nesta pasta.");
        // }

        // Gera um nome único para o arquivo físico
        String nomeArquivoUnico = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // ✅ Use Paths para garantir a portabilidade do caminho
        Path uploadPath = Paths.get(uploadDir, nomeArquivoUnico);

        // Salva o arquivo no sistema de arquivos
        Files.copy(file.getInputStream(), uploadPath);

        // Salva os metadados do arquivo no banco de dados
        Arquivo arquivo = new Arquivo();
        arquivo.setNomeArquivo(file.getOriginalFilename());
        arquivo.setCaminhoArmazenamento(uploadPath.toString());
        arquivo.setTamanhoBytes(file.getSize());
        arquivo.setDataUpload(LocalDateTime.now());
        arquivo.setPasta(pasta);
        arquivo.setCriadoPor(usuario);

        return arquivoRepository.save(arquivo);
    }

    /**
     * Busca um arquivo por ID.
     * @param id O ID do arquivo.
     * @return O objeto Arquivo.
     */
    public Arquivo buscarPorId(Long id) {
        return arquivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + id));
    }

    /**
     * Lista todos os arquivos de uma pasta específica.
     * @param pastaId O ID da pasta.
     * @return Uma lista de arquivos.
     */
    public List<Arquivo> listarArquivosPorPasta(Long pastaId) {
        pastaService.buscarPorId(pastaId); // Garante que a pasta existe
        return arquivoRepository.findByPastaId(pastaId);
    }

    /**
     * Exclui um arquivo do banco de dados e do sistema de arquivos.
     * @param id O ID do arquivo a ser excluído.
     */
    public void excluirArquivo(Long id) {
        Optional<Arquivo> arquivoOptional = arquivoRepository.findById(id);
        if (arquivoOptional.isPresent()) {
            Arquivo arquivo = arquivoOptional.get();
            // Primeiro, exclui o arquivo físico
            File arquivoFisico = new File(arquivo.getCaminhoArmazenamento());
            if (arquivoFisico.exists()) {
                arquivoFisico.delete();
            }
            // Depois, exclui o registro do banco de dados
            arquivoRepository.delete(arquivo);
        } else {
            throw new EntityNotFoundException("Arquivo não encontrado com o ID: " + id);
        }
    }
}