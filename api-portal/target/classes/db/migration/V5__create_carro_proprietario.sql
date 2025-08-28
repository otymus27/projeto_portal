
CREATE TABLE IF NOT EXISTS tb_carro_proprietario (
                    carro_id BIGINT NOT NULL,
                    proprietario_id BIGINT NOT NULL,
                    PRIMARY KEY (carro_id, proprietario_id),
                    FOREIGN KEY (carro_id) REFERENCES tb_carro(id) ON DELETE CASCADE,
                    FOREIGN KEY (proprietario_id) REFERENCES tb_proprietario(id) ON DELETE CASCADE
    );



INSERT INTO tb_carro_proprietario (carro_id, proprietario_id) VALUES
                                                     (1, 1),
                                                     (1, 2);
