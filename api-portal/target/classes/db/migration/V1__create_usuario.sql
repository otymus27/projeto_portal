-- Criação da tabela de roles (caso não exista)
CREATE TABLE IF NOT EXISTS tb_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL
);

-- Criação da tabela de usuários
CREATE TABLE IF NOT EXISTS tb_usuarios (
   id BIGINT AUTO_INCREMENT PRIMARY KEY,
   username VARCHAR(255) NOT NULL UNIQUE,
   password VARCHAR(255) NOT NULL,
    senha_provisoria BOOLEAN NOT NULL DEFAULT FALSE
);

-- Criação da tabela de relacionamento entre usuários e roles
CREATE TABLE IF NOT EXISTS tb_usuarios_roles (
     user_id BIGINT NOT NULL,
     role_id BIGINT NOT NULL,
     PRIMARY KEY (user_id, role_id),
     FOREIGN KEY (user_id) REFERENCES tb_usuarios(id) ON DELETE CASCADE,
     FOREIGN KEY (role_id) REFERENCES tb_roles(id) ON DELETE CASCADE
);

-- Tabela de Pastas
CREATE TABLE IF NOT EXISTS tb_pasta (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome_pasta VARCHAR(255) NOT NULL,
    caminho_completo VARCHAR(255) NOT NULL,
    data_criacao DATETIME(6) NOT NULL,
    pasta_pai_id BIGINT,
    CONSTRAINT fk_subpastas_pasta_pai FOREIGN KEY (pasta_pai_id) REFERENCES tb_pasta (id)
);

-- Tabela de Arquivos
CREATE TABLE IF NOT EXISTS tb_arquivo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome_arquivo VARCHAR(255) NOT NULL,
    caminho_armazenamento VARCHAR(255) NOT NULL,
    tamanho_bytes BIGINT,
    data_upload DATETIME(6) NOT NULL,
    pasta_id BIGINT,
    criado_por_id BIGINT,
    CONSTRAINT fk_arquivos_pastas FOREIGN KEY (pasta_id) REFERENCES tb_pasta (id),
    CONSTRAINT fk_arquivos_usuarios FOREIGN KEY (criado_por_id) REFERENCES tb_usuarios (id)
);

-- ✅ NOVO: Tabela de relacionamento entre usuários e as pastas principais que eles podem acessar
-- Renomeada de tb_permissao_pasta para usuario_pasta_principal
CREATE TABLE tb_permissao_pasta (
     usuario_id BIGINT NOT NULL,
     pasta_id BIGINT NOT NULL,
     PRIMARY KEY (usuario_id, pasta_id),
     FOREIGN KEY (usuario_id) REFERENCES tb_usuarios(id),
     FOREIGN KEY (pasta_id) REFERENCES tb_pasta(id)
);


-- Inserção de roles
INSERT INTO tb_roles (nome) VALUES
                                ('ADMIN'),
                                ('BASIC'),
                                ('GERENTE');


-- Inserção de usuários
INSERT INTO tb_usuarios (username, password, senha_provisoria) VALUES
   ('admin', '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', FALSE),  -- senha: senha123
   ('gabriel', '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', TRUE),  -- senha: senha123
   ('beatriz', '$2a$10$Vt6ldlS92W5N6HF1OS5qfIWdb0P7Zfjdqxq6rzQ3S1CnllXaZRaBu', FALSE), -- senha: senha789
   ('14329301', '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', TRUE),   -- senha: senha123
   ('usuario5', '$2a$10$ADqjEwM1joxBvl0ivQiqK3odF2gGbzRslfvtnwTqfmRbx11P0RHgi', FALSE);  -- senha: senha202

-- Inserção de relacionamento usuário ↔ role
INSERT INTO tb_usuarios_roles (user_id, role_id) VALUES
     (1, 1),  -- admin com ADMIN
     (2, 2),  -- usuario2 com BASIC
     (3, 3),  -- usuario3 com GERENTE
     (4, 3),  -- usuario4 com ADMIN
     (5, 2);  -- usuario5 com BASIC

-- As pastas principais estão associadas a um setor.
-- Pastas filhas (subpastas) terão 'pasta_pai_id' mas 'setor_id' nulo.
INSERT INTO tb_pasta (nome_pasta, caminho_completo, data_criacao, pasta_pai_id) VALUES
    ('Relatorios-Financeiros', '/Financeiro/Relatorios-Financeiros', NOW(), NULL),
    ('Campanhas-2025', '/Marketing/Campanhas-2025', NOW(), NULL),
    ('Docs-RH', '/RH/Docs-RH', NOW(), NULL),
    ('Projetos-TI', '/TI/Projetos-TI', NOW(), NULL),
    ('Contratos', '/Juridico/Contratos', NOW(), NULL);

-- Define quais usuários têm acesso direto a quais pastas principais.
INSERT INTO tb_permissao_pasta (usuario_id, pasta_id) VALUES
   (1, 1), -- Admin tem acesso à pasta Financeiro
   (1, 2), -- Admin tem acesso à pasta Marketing
   (1, 3), -- Admin tem acesso à pasta RH
   (1, 4), -- Admin tem acesso à pasta TI
   (1, 5), -- Admin tem acesso à pasta Jurídico
   (2, 1), -- Gabriel (BASIC) tem acesso à pasta Financeiro
   (3, 2), -- Beatriz (GERENTE) tem acesso à pasta Marketing
   (4, 3), -- Usuario4 (GERENTE) tem acesso à pasta RH
   (5, 5); -- Usuario5 (BASIC) tem acesso à pasta Jurídico

-- Cada arquivo está associado a uma pasta e a um usuário que o criou.
-- Caminho de armazenamento é apenas um exemplo.
INSERT INTO tb_arquivo (nome_arquivo, caminho_armazenamento, tamanho_bytes, data_upload, pasta_id, criado_por_id) VALUES
                    ('Relatorio_de_Vendas.pdf', '/caminho/servidor/vendas.pdf', 10240, NOW(), 1, 2),
                    ('Plano_de_Midia.pdf', '/caminho/servidor/midia.pdf', 5120, NOW(), 2, 4),
                    ('Manual_do_Funcionario.pdf', '/caminho/servidor/manual.pdf', 8192, NOW(), 3, 3),
                    ('Especificacoes_Sistema.pdf', '/caminho/servidor/specs.pdf', 20480, NOW(), 4, 1),
                    ('Minuta_de_Acordo.pdf', '/caminho/servidor/acordo.pdf', 4096, NOW(), 5, 5);