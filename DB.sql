drop database if exists marcoa;
create database marcoa;
use marcoa;

drop table if exists cliente;
CREATE TABLE cliente (
uuid varchar(40) not null PRIMARY KEY,
nome VARCHAR(50) NOT NULL,
telefone VARCHAR(20) NOT NULL,
email VARCHAR(40) not null unique,
modelo VARCHAR(40) default "residencial",
endereco VARCHAR(100),
cpf_cnpj VARCHAR(19) not null unique,
ativo tinyint default 0,
esp_ativo tinyint default 1,
senha varchar(40) not null,
data_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

drop table if exists usuario;
CREATE TABLE usuario (
uuid varchar(40) not null PRIMARY KEY,
nivel tinyint NOT NULL,
senha VARCHAR(40) NOT NULL,
usuario VARCHAR(40),
endereco VARCHAR(100),
cpf_cnpj VARCHAR(19),
uuid_cliente varchar(40),
data_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
foreign key (uuid_cliente) references cliente (uuid)
);

drop table if exists historico_usuario;
CREATE TABLE historico_usuario (
uuid_usuario varchar(40) not null,
id bigint unsigned auto_increment primary key,
acao VARCHAR(40) default NULL,
temp_prog decimal(5,3) default null,
data_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
foreign key (uuid_usuario) references usuario (uuid)
);

drop table if exists dispositivo;
CREATE TABLE dispositivo (
uuid varchar(40) not null primary key,
status_esp_ler tinyint default 0,
status_esp_gravar tinyint default 0,
auto_esp_ler tinyint default 0,
auto_esp_gravar tinyint default 0,
modo_viagem_esp_ler tinyint default 0,
modo_viagem_esp_gravar tinyint default 0,
potencia tinyint default 0,
ultima_sincronizacao timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
versao varchar(10),
temp_prog_esp_ler decimal(5,3) default 30.0,
temp_prog_esp_gravar decimal(5,3) default 30.0,
temp_atual decimal (5,3) default 0.0,
ligou_as timestamp,
erro_leitura int unsigned default 0,
intensidade_sinal tinyint default 0,
atualizar tinyint default 0,
nome VARCHAR(40) not null
);

drop table if exists historico_dispositivo;
CREATE TABLE historico_dispositivo (
uuid_dispositivo varchar(40),
id bigint unsigned auto_increment primary key,
temp_atual decimal(5,3)not null,
temp_prog decimal(5,3) not null,
data_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
foreign key (uuid_dispositivo) references dispositivo (uuid)
);

drop table if exists programacoes_suinos;
CREATE TABLE programacoes_suinos (
id bigint unsigned auto_increment primary key,
numero_dias tinyint not NULL,
temp_prog decimal(5,3) default null,
apartir_de TIMESTAMP,
uuid_dispositivo varchar(40) not null,
foreign key (uuid_dispositivo) references dispositivo (uuid)
);

drop table if exists programacoes_residencial;
CREATE TABLE programacoes_residencial (
liga timestamp,
desliga timestamp,
temp_prog decimal(5,3),
dia_semana tinyint,
uuid_dispositivo varchar(40) not null,
id bigint unsigned auto_increment primary key,
foreign key (uuid_dispositivo) references dispositivo (uuid)
);

DROP table IF EXISTS usuario_dispositivo;
create table usuario_dispositivo(
id bigint unsigned auto_increment primary key,
uuid_usuario varchar(40),
uuid_dispositivo varchar(40),
foreign key(uuid_usuario) references usuario(uuid_cliente),
foreign key (uuid_dispositivo) references dispositivo (uuid)
);

DROP TRIGGER IF EXISTS inserir_usuario_cliente;
CREATE TRIGGER inserir_usuario_cliente 
AFTER INSERT ON cliente
FOR EACH ROW
  INSERT IGNORE INTO usuario (usuario, senha, endereco,cpf_cnpj, uuid_cliente, nivel,uuid)
  VALUES (NEW.email, new.senha, new.endereco, new.cpf_cnpj, new.uuid, 5, uuid());
  
DROP TRIGGER IF EXISTS gerar_uuid_cliente;
CREATE TRIGGER gerar_uuid_cliente 
BEFORE INSERT ON cliente 
  FOR EACH ROW
  SET new.uuid = uuid();

 