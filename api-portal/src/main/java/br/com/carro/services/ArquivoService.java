package br.com.carro.services;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.ArquivoRepository;
import br.com.carro.repositories.PastaRepository;
import org.springframework.beans.factory.annotation.Value; // ✅ Importe a anotação @Value
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
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
    private final PastaRepository pastaRepository;
    private final UsuarioService usuarioService; // Usar o UsuarioService para buscar o usuário
    private final String uploadDir = "caminho/para/seus/arquivos"; // ✅ ATENÇÃO: Configure este caminho!

    @Autowired
    public ArquivoService(ArquivoRepository arquivoRepository, PastaRepository pastaRepository, UsuarioService usuarioService) {
        this.arquivoRepository = arquivoRepository;
        this.pastaRepository = pastaRepository;
        this.usuarioService = usuarioService;

        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    // ✅ O método agora recebe o usuário como argumento
    public Arquivo salvarArquivo(MultipartFile file, Long pastaId, Usuario usuario) throws IOException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));

        // Usa o usuário recebido
        if (!checarAcessoPasta(pasta, usuario)) {
            throw new AccessDeniedException("Você não tem permissão para adicionar arquivos nesta pasta.");
        }

        String nomeArquivoUnico = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get(uploadDir, nomeArquivoUnico);
        Files.copy(file.getInputStream(), uploadPath);

        Arquivo arquivo = new Arquivo();
        arquivo.setNomeArquivo(file.getOriginalFilename());
        arquivo.setCaminhoArmazenamento(uploadPath.toString());
        arquivo.setTamanhoBytes(file.getSize());
        arquivo.setDataUpload(LocalDateTime.now());
        arquivo.setPasta(pasta);
        arquivo.setCriadoPor(usuario);

        return arquivoRepository.save(arquivo);
    }

    // ✅ O método agora recebe o usuário como argumento
    public Arquivo buscarPorId(Long id, Usuario usuario) throws AccessDeniedException {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + id));

        if (!checarAcessoPasta(arquivo.getPasta(), usuario)) {
            throw new AccessDeniedException("Você não tem permissão para acessar este arquivo.");
        }

        return arquivo;
    }

    // ✅ O método agora recebe o usuário como argumento
    public List<Arquivo> listarArquivosPorPasta(Long pastaId, Usuario usuario) throws AccessDeniedException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));

        if (!checarAcessoPasta(pasta, usuario)) {
            throw new AccessDeniedException("Você não tem permissão para listar arquivos desta pasta.");
        }

        return arquivoRepository.findByPastaId(pastaId);
    }

    // ✅ O método agora recebe o usuário como argumento
    public void excluirArquivo(Long id, Usuario usuario) throws AccessDeniedException {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + id));

        if (!checarAcessoPasta(arquivo.getPasta(), usuario)) {
            throw new AccessDeniedException("Você não tem permissão para excluir este arquivo.");
        }

        File arquivoFisico = new File(arquivo.getCaminhoArmazenamento());
        if (arquivoFisico.exists()) {
            arquivoFisico.delete();
        }

        arquivoRepository.delete(arquivo);
    }

    // Método auxiliar para checar permissão na pasta
    private boolean checarAcessoPasta(Pasta pasta, Usuario usuario) {
        if (usuario.getRoles().stream().anyMatch(role -> role.getNome().equals("ADMIN"))) {
            return true;
        }
        if (pasta == null) {
            return false;
        }
        if (pasta.getUsuariosComPermissao().contains(usuario)) {
            return true;
        }
        if (pasta.getPastaPai() == null) {
            return usuario.getPastasPrincipaisAcessadas().contains(pasta);
        }
        return checarAcessoPasta(pasta.getPastaPai(), usuario);
    }
}