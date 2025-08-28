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

-- Inserção de roles
INSERT INTO tb_roles (nome) VALUES
                                ('ADMIN'),
                                ('BASIC'),
                                ('GERENTE');

-- Inserção de usuários
INSERT INTO tb_usuarios (username, password, senha_provisoria) VALUES
                                                                   ('admin', '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', FALSE),  -- senha: senha123
                                                                   ('gabriel', '$2a$10$wgeAMfb8E1olrHj5Ko5P7T7FyvhYrgHQt18sJll8eLg1BYJc0AXve', TRUE),  -- senha: senha456
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