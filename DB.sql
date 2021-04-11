use marcoa;
DROP table IF EXISTS usuario_dispositivo;
create table usuario_dispositivo(
id bigint unsigned auto_increment primary key,
uuid_usuario varchar(40),
uuid_dispositivo varchar(40),
foreign key(uuid_usuario) references usuario(uuid),
foreign key (uuid_dispositivo) references dispositivo (uuid)
);

DROP TRIGGER IF EXISTS inserir_usuario_cliente;
CREATE TRIGGER inserir_usuario_cliente 
AFTER INSERT ON cliente
FOR EACH ROW
  INSERT IGNORE INTO usuario (usuario, senha, endereco,cpf_cnpj, id_cliente, nivel,uuid)
  VALUES (NEW.email, new.senha, new.endereco, new.cpf_cnpj, new.id, 5, uuid());