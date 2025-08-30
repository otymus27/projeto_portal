package br.com.carro.services;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.DTO.PastaDTO;
import br.com.carro.entities.DTO.PastaRequestDTO;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.ArquivoRepository;
import br.com.carro.repositories.PastaRepository;
import br.com.carro.repositories.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PastaService {

    private final PastaRepository pastaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArquivoService arquivoService;
    private final ArquivoRepository arquivoRepository;

    @Value("${app.file.upload-dir}")
    private String fileStorageLocation;

    @Autowired
    public PastaService(PastaRepository pastaRepository, UsuarioRepository usuarioRepository, ArquivoService arquivoService,  ArquivoRepository arquivoRepository) {
        this.pastaRepository = pastaRepository;
        this.usuarioRepository = usuarioRepository;
        this.arquivoService = arquivoService;
        this.arquivoRepository = arquivoRepository;
    }



    public Page<PastaDTO> listarPastasPrincipais(boolean isAdmin, Usuario usuario, Pageable pageable) {
        Page<Pasta> pastas;
        if (isAdmin) {
            pastas = pastaRepository.findByPastaPaiIsNull(pageable);
        } else {
            Set<Long> pastasIds = usuario.getPastasPrincipaisAcessadas().stream()
                    .map(Pasta::getId)
                    .collect(Collectors.toSet());
            pastas = pastaRepository.findAllByIdIn(pastasIds, pageable);
        }

        List<PastaDTO> dtoList = pastas.getContent().stream()
                .map(PastaDTO::fromEntity)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, pastas.getTotalElements());
    }

    public Page<PastaDTO> listarSubpastas(Long pastaPaiId, Pageable pageable) {
        pastaRepository.findById(pastaPaiId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta pai não encontrada com o ID: " + pastaPaiId));

        Page<Pasta> subpastas = pastaRepository.findByPastaPaiId(pastaPaiId, pageable);

        List<PastaDTO> dtoList = subpastas.getContent().stream()
                .map(PastaDTO::fromEntity)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, subpastas.getTotalElements());
    }


    public Pasta buscarPorId(Long id) {
        return pastaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + id));
    }

    @Transactional
    public Pasta criarPasta(PastaRequestDTO pastaDTO) {
        Pasta novaPasta = new Pasta();
        novaPasta.setNomePasta(pastaDTO.getNomePasta());
        novaPasta.setCaminhoCompleto(pastaDTO.getCaminhoCompleto());
        novaPasta.setDataCriacao(LocalDateTime.now());

        if (pastaDTO.getPastaPaiId() != null) {
            Pasta pastaPai = pastaRepository.findById(pastaDTO.getPastaPaiId())
                    .orElseThrow(() -> new EntityNotFoundException("Pasta pai não encontrada."));
            novaPasta.setPastaPai(pastaPai);
        }

        Set<Usuario> usuariosPermitidos = new HashSet<>();
        if (pastaDTO.getUsuariosComPermissaoIds() != null) {
            usuariosPermitidos.addAll(usuarioRepository.findAllById(pastaDTO.getUsuariosComPermissaoIds()));
        }
        novaPasta.setUsuariosComPermissao(usuariosPermitidos);

        return pastaRepository.save(novaPasta);
    }

    @Transactional
    public Pasta atualizar(Long id, Pasta pastaAtualizada) {
        Pasta pastaExistente = pastaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + id));

        if (pastaAtualizada.getNomePasta() != null) {
            pastaExistente.setNomePasta(pastaAtualizada.getNomePasta());
        }
        if (pastaAtualizada.getCaminhoCompleto() != null) {
            pastaExistente.setCaminhoCompleto(pastaAtualizada.getCaminhoCompleto());
        }

        return pastaRepository.save(pastaExistente);
    }

    /**
     * ✅ NOVO MÉTODO: Deleta uma pasta e todo seu conteúdo (arquivos e subpastas) recursivamente.
     * @param pastaId O ID da pasta a ser deletada.
     */
    @Transactional
    public void excluir(Long pastaId) {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));

        // Deleta os arquivos físicos associados a esta pasta e suas subpastas.
        deletarArquivosDePasta(pasta);

        // Deleta a pasta e seus filhos do banco de dados.
        // O cascade type definido na entidade Pasta deve cuidar dos filhos.
        pastaRepository.delete(pasta);
    }

    // --- EXCLUSÃO ----

    /**
     * ✅ NOVO MÉTODO: Exclui uma pasta por ID, incluindo o conteúdo físico e do banco.
     * @param pastaId O ID da pasta a ser excluída.
     */
    @Transactional
    public void excluirPasta(Long pastaId) throws IOException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));

        // ✅ Adicione esta linha de log para depuração
        System.out.println("--- Pasta a ser excluída: ---");
        System.out.println("ID: " + pasta.getId());
        System.out.println("Nome: " + pasta.getNomePasta());
        System.out.println("Caminho completo: " + pasta.getCaminhoCompleto());
        System.out.println("--- Fim do diagnóstico da pasta ---");
        // ✅ TEMPORÁRIO: Chama o método de diagnóstico para verificar os arquivos
        diagnosticarDelecaoDePasta(pasta);
        // Primeiro, exclui todos os arquivos e subpastas fisicamente e do banco
        deletarConteudoPasta(pasta);

        // Em seguida, exclui a pasta principal do banco de dados
        pastaRepository.delete(pasta);

        // Passo 3: ✅ CORREÇÃO FINAL: Exclui o diretório físico da pasta principal.
        Path caminhoPasta = Paths.get(pasta.getCaminhoCompleto());
        if (Files.exists(caminhoPasta)) {
            Files.deleteIfExists(caminhoPasta);
        }
    }



    /**
     * Método auxiliar recursivo para deletar arquivos físicos e do banco.
     */
    private void deletarArquivosDePasta(Pasta pasta) {
        // Deleta os arquivos da pasta atual
        for (Arquivo arquivo : pasta.getArquivos()) {
            try {
                Path filePath = Paths.get(arquivo.getCaminhoArmazenamento());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Apenas loga o erro, mas não interrompe o processo para tentar deletar o restante.
                System.err.println("Erro ao deletar arquivo físico: " + arquivo.getNomeArquivo() + " - " + e.getMessage());
            }
        }

        // Chama a si mesmo para as subpastas
        for (Pasta subpasta : pasta.getSubpastas()) {
            deletarArquivosDePasta(subpasta);
        }
    }



    /**
     * Cria subpastas e arquivos a partir de um upload de diretório.
     * @param files A lista de arquivos com seus caminhos relativos.
     * @param pastaPaiId O ID da pasta principal de destino.
            * @param usuario O usuário logado.
            * @throws IOException Se ocorrer um erro ao salvar o arquivo.
     */
    @Transactional
    public void criarSubpastasEArquivos(MultipartFile[] files, Long pastaPaiId, Usuario usuario) throws IOException {
        Pasta pastaPai = pastaRepository.findById(pastaPaiId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta pai não encontrada com o ID: " + pastaPaiId));

        for (MultipartFile file : files) {
            String caminhoRelativo = file.getOriginalFilename();

            // ✅ CORREÇÃO: Verifica se o caminho contém uma barra para saber se é uma subpasta
            int ultimoIndiceDaBarra = caminhoRelativo.lastIndexOf('/');

            if (ultimoIndiceDaBarra != -1) {
                // Se tiver uma barra, o arquivo está em uma subpasta.
                String caminhoSomentePasta = caminhoRelativo.substring(0, ultimoIndiceDaBarra);

                Pasta pastaAtual = pastaPai;
                String[] diretorios = caminhoSomentePasta.split("/");

                // Itera sobre o caminho e cria as pastas, se elas não existirem
                for (String nomeDiretorio : diretorios) {
                    pastaAtual = getOrCreatePasta(nomeDiretorio, pastaAtual);
                }

                // Salva o arquivo na subpasta final
                arquivoService.uploadArquivo(file, pastaAtual.getId(), usuario);

            } else {
                // Se não tiver barra, o arquivo está na pasta principal.
                arquivoService.uploadArquivo(file, pastaPai.getId(), usuario);
            }
        }
    }

    /**
     * Busca ou cria uma subpasta dentro de uma pasta pai.
     * @param nomePasta O nome da subpasta.
     * @param pastaPai A pasta pai.
     * @return A subpasta existente ou a recém-criada.
     */
    private Pasta getOrCreatePasta(String nomePasta, Pasta pastaPai) {
        // Tenta encontrar a pasta existente.
        Optional<Pasta> subpastaExistente = pastaRepository.findByNomePastaAndPastaPai(nomePasta, pastaPai);

        if (subpastaExistente.isPresent()) {
            return subpastaExistente.get();
        } else {
            Pasta novaPasta = new Pasta();
            novaPasta.setNomePasta(nomePasta);
            // ✅ CORREÇÃO: Constrói o caminho completo usando a API de Paths
            Path caminhoCompletoDaPastaPai = Paths.get(pastaPai.getCaminhoCompleto());
            Path novoCaminhoCompleto = caminhoCompletoDaPastaPai.resolve(nomePasta);
            novaPasta.setCaminhoCompleto(novoCaminhoCompleto.toAbsolutePath().normalize().toString());
            novaPasta.setPastaPai(pastaPai);
            novaPasta.setDataCriacao(LocalDateTime.now());


            try {
                // Tenta salvar a nova pasta.
                return pastaRepository.save(novaPasta);
            } catch (DataIntegrityViolationException e) {
                // Se a exceção de violação de integridade ocorrer (devido a uma pasta duplicada),
                // busca a pasta que já foi criada por outra thread ou por uma operação anterior.
                return pastaRepository.findByNomePastaAndPastaPai(nomePasta, pastaPai)
                        .orElseThrow(() -> new IllegalStateException("Falha ao criar ou encontrar a pasta após violação de integridade."));
            }
        }
    }

    /**
     * Busca pastas por nome com paginação.
     * @param nome O termo de busca.
     * @param pageable As informações de paginação (número da página, tamanho da página, etc.).
     * @return Uma página de PastaDTOs.
     */
    public Page<PastaDTO> buscarPastasPorNome(String nome, Pageable pageable) {
        Page<Pasta> pastas = pastaRepository.findByNomePastaContainingIgnoreCase(nome, pageable);

        // Converte a Page de Pastas para uma Page de PastaDTOs
        return pastas.map(PastaDTO::fromEntity);
    }


    /**
     * ✅ NOVO MÉTODO: Cria um arquivo ZIP de uma pasta e seus conteúdos.
     * @param pastaId O ID da pasta a ser compactada.
     * @return O caminho do arquivo ZIP temporário.
     * @throws IOException se ocorrer um erro de I/O.
     */
    public Path downloadPasta(Long pastaId) throws IOException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));

        // Cria um arquivo ZIP temporário para o download
        Path tempZipFile = Files.createTempFile(pasta.getNomePasta() + "_", ".zip");

        // ✅ CORREÇÃO: Registra o arquivo temporário para ser deletado na saída da JVM
        tempZipFile.toFile().deleteOnExit();

        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(tempZipFile))) {
            adicionarPastaAoZip(pasta, zipOut, "");
        }

        return tempZipFile;
    }

    /**
     * Método recursivo para adicionar a pasta e seus conteúdos ao arquivo ZIP.
     * @param pasta A pasta atual a ser adicionada.
     * @param zipOut O stream de saída do arquivo ZIP.
     * @param caminhoRelativo O caminho relativo da pasta dentro do ZIP.
     * @throws IOException
     */
    private void adicionarPastaAoZip(Pasta pasta, ZipOutputStream zipOut, String caminhoRelativo) throws IOException {
        String nomeEntrada = caminhoRelativo.isEmpty() ? pasta.getNomePasta() + "/" : caminhoRelativo + pasta.getNomePasta() + "/";

        // Adiciona a entrada da própria pasta (útil para pastas vazias)
        ZipEntry zipEntry = new ZipEntry(nomeEntrada);
        zipOut.putNextEntry(zipEntry);
        zipOut.closeEntry();

        // Adiciona todos os arquivos da pasta ao ZIP
        for (Arquivo arquivo : pasta.getArquivos()) {
            Path arquivoPath = Paths.get(arquivo.getCaminhoArmazenamento());

            // ✅ CORREÇÃO: Verifica se o arquivo existe antes de tentar adicioná-lo
            if (Files.exists(arquivoPath)) {
                String nomeEntradaArquivo = nomeEntrada + arquivo.getNomeArquivo();
                zipOut.putNextEntry(new ZipEntry(nomeEntradaArquivo));
                Files.copy(arquivoPath, zipOut);
                zipOut.closeEntry();
            }
        }

        // Adiciona todas as subpastas de forma recursiva
        for (Pasta subpasta : pasta.getSubpastas()) {
            adicionarPastaAoZip(subpasta, zipOut, nomeEntrada);
        }
    }

    // Método auxiliar para deletar somente o conteúdo da pasta, não a pasta em si.
    private void deletarConteudoPasta(Pasta pasta) {

        // 1. Deleta arquivos de forma recursiva
        deletarArquivosEEntidades(pasta);

        // 2. Deleta as subpastas no banco de dados
        // A entidade Pasta tem um cascade, então a deleção da pasta pai deve apagar os filhos,
        // mas vamos garantir a deleção manual para maior segurança.
        for (Pasta subpasta : pasta.getSubpastas()) {
            deletarConteudoPasta(subpasta);
        }
    }

    /**
     * ✅ NOVO MÉTODO AUXILIAR: Deleta arquivos físicos e suas entidades.
     */
    private void deletarArquivosEEntidades(Pasta pasta) {
        // Itera sobre uma cópia para evitar ConcurrentModificationException
        for (Arquivo arquivo : List.copyOf(pasta.getArquivos())) {
            try {
                Path filePath = Paths.get(arquivo.getCaminhoArmazenamento());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                System.err.println("Erro ao deletar arquivo físico: " + arquivo.getNomeArquivo() + " - " + e.getMessage());
            }
        }
        // Deleta todas as entidades de arquivos do banco de dados de uma vez
        arquivoRepository.deleteAll(pasta.getArquivos());
    }

    /**
     * ✅ NOVO MÉTODO: Substitui o conteúdo de uma pasta por um novo upload.
     * Isso deleta o conteúdo existente e recria a estrutura a partir do novo upload.
     * @param pastaId O ID da pasta a ser substituída.
     * @param files Os novos arquivos para o upload.
     * @param usuario O usuário logado.
     * @throws IOException Se ocorrer um erro durante o upload.
     */
    @Transactional
    public void substituirPasta(Long pastaId, MultipartFile[] files, Usuario usuario) throws IOException {
        // Deleta o conteúdo da pasta existente.
        // A pasta pai continua a mesma, mas os arquivos e subpastas são deletados.
        Pasta pastaParaSubstituir = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada."));

        // Chama o método para deletar o conteúdo.
        deletarConteudoPasta(pastaParaSubstituir);

        // Faz o upload da nova estrutura e arquivos no local da pasta existente.
        criarSubpastasEArquivos(files, pastaId, usuario);
    }

    /**
     * ✅ MÉTODO DE DIAGNÓSTICO TEMPORÁRIO:
     * Verifica as propriedades do arquivo antes de tentar deletá-lo.
     * NÃO USE EM PRODUÇÃO.
     */
    private void diagnosticarDelecaoDePasta(Pasta pasta) {
        System.out.println("--- Diagnóstico de Deleção para a Pasta: " + pasta.getNomePasta() + " ---");

        for (Arquivo arquivo : List.copyOf(pasta.getArquivos())) {
            try {
                Path filePath = Paths.get(arquivo.getCaminhoArmazenamento());

                System.out.println("  > Verificando arquivo: " + arquivo.getNomeArquivo());
                System.out.println("    - Caminho do banco de dados: " + arquivo.getCaminhoArmazenamento());
                System.out.println("    - Caminho normalizado: " + filePath.toAbsolutePath().normalize());
                System.out.println("    - Arquivo existe? " + Files.exists(filePath));
                System.out.println("    - Posso escrever/deletar? " + Files.isWritable(filePath));

                // Tenta a deleção e imprime o resultado
                boolean deletado = Files.deleteIfExists(filePath);
                System.out.println("    - Tentativa de deleção: " + (deletado ? "Sucesso" : "Falha"));

            } catch (Exception e) {
                System.out.println("    - ERRO AO DELETAR: " + e.getMessage());
            }
        }

        for (Pasta subpasta : pasta.getSubpastas()) {
            diagnosticarDelecaoDePasta(subpasta);
        }

        System.out.println("--- Fim do Diagnóstico ---");
    }


    //Métodos para testes locais apagar depois
    /**
     * ✅ MÉTODO DE TESTE TEMPORÁRIO E DEFINITIVO
     * Cria e tenta deletar um arquivo para verificar o caminho de deleção.
     */

    public void testarCaminhoDelecao() {
        System.out.println("--- Teste de Caminho de Deleção ---");

        Path pastaTemporaria = Paths.get(fileStorageLocation);
        Path arquivoTeste = pastaTemporaria.resolve(UUID.randomUUID().toString() + "_teste.txt");

        try {
            // Tenta criar o arquivo
            Files.createFile(arquivoTeste);
            System.out.println("1. Arquivo de teste criado com sucesso em: " + arquivoTeste.toAbsolutePath().normalize());

            // Tenta deletar o mesmo arquivo imediatamente
            boolean deletado = Files.deleteIfExists(arquivoTeste);
            System.out.println("2. O arquivo de teste foi deletado? " + (deletado ? "Sim" : "Não"));

            if (Files.exists(arquivoTeste)) {
                System.out.println("3. O arquivo AINDA EXISTE, algo está impedindo a deleção.");
            } else {
                System.out.println("3. O arquivo NÃO EXISTE, a deleção funcionou. O problema está no caminho do banco de dados.");
            }
        } catch (IOException e) {
            System.out.println("ERRO: Ocorreu uma exceção de I/O: " + e.getMessage());
        }

        System.out.println("--- Fim do Teste ---");
    }


}