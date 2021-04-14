<?php

use \Psr\Http\Message\ResponseInterface as Response;
use \Psr\Http\Message\ServerRequestInterface as Request;
require 'vendor/autoload.php';

$app = new \Slim\App;

$app->get('/', function (Request $request, Response $reponse) {
    echo 'home user working';
});

//get all users
$app->get('/api/users', function (Request $request, Response $reponse) {
    $sql = "SELECT * FROM  usuario";

    try {

        $db = new db();
        $pdo = $db->connect();

        $stmt = $pdo->query($sql);
        $users = $stmt->fetchAll(PDO::FETCH_OBJ);

        $pdo = null;
        echo json_encode($users);
    } catch (\PDOException $e) {
        echo '{"msg": {"resp": ' . $e->getMessage() . '}}';
    }
});


//get a single user
$app->get('/api/users/{id}', function (Request $request, Response $reponse, array $args) {
    $id = $request->getAttribute('id');

    $sql = "SELECT * FROM users where id = $id";

    try {
        $db = new db();
        $pdo = $db->connect();

        $stmt = $pdo->query($sql);
        $user = $stmt->fetchAll(PDO::FETCH_OBJ);

        $pdo = null;


        echo json_encode($user);
    } catch (\PDOException $e) {
        echo '{"msg": {"resp": ' . $e->getMessage() . '}}';
    }
});


//make a post request
    $app->post('/api/cliente/add', function (Request $request, Response $reponse, array $args) {
        $nome = $request->getParam('nome');
        $cpf_cnpj = $request->getParam('cpf_cnpj');
        $email = $request->getParam('email');
        $telefone = $request->getParam('telefone');
        $endereco = $request->getParam('endereco');
        $senha = $request->getParam('senha');

        try {
            //get db object
            $db = new db();
            //conncect
            $pdo = $db->connect();


            $sql = "INSERT INTO cliente (nome, telefone, email,endereco,cpf_cnpj,senha) VALUES (?,?,?,?,?,?)";


            $pdo->prepare($sql)->execute([$nome, $telefone, $email, $endereco, $cpf_cnpj,$senha]);

            echo '{"text": "User '. $email .' has been just added now", "code":200}';
            $pdo = null;
        } catch (\PDOException $e) {
            echo '{"text": "' . $e->getMessage() . '", "code":901}';
        }
    });
    
    $app->post('/api/dispositivo/dados', function (Request $request, Response $reponse, array $args) {
        $id = $request->getParam('id');

        try {
            $sql = "select d.uuid, d.status, d.auto, d.modo_viagem, d.ultima_sincronizacao, d.temp_prog, d.temp_atual, d.nome from usuario_dispositivo as ud inner join dispositivo as d on ud.uuid_dispositivo = d.uuid inner join usuario as u on u.uuid_cliente = ud.uuid_usuario where u.uuid = '".$id."';";
            
            $db = new db();
            $pdo = $db->connect();

            $stmt = $pdo->query($sql);
            $user = $stmt->fetchAll(PDO::FETCH_OBJ);

               $code = (object) ['code' => 200];
            if(count($user)>0){
               echo json_encode(array_merge((array) $user, (array) $code));
           }else{
               echo '{"code":902}';
           }
            $pdo = null;
        } catch (\PDOException $e) {
            echo '{"text": "' . $e->getMessage() . '", "code":903}';
        }
    });
    
    $app->post('/api/dispositivo/modoviagem', function (Request $request, Response $reponse, array $args) {
               $id_cliente = $request->getParam('id_cliente');
               $id = $request->getParam('id_usuario');
       $modo_viagem = $request->getParam('modo_viagem');

        try {
            $sql = "select d.uuid, d.status, d.auto, d.modo_viagem, d.ultima_sincronizacao, d.temp_prog, d.temp_atual, d.nome from usuario_dispositivo as ud inner join dispositivo as d on ud.uuid_dispositivo = d.uuid inner join usuario as u on u.uuid_cliente = ud.uuid_usuario where u.uuid = '".$id."';";
               
            $sql_update = "update dispositivo as d inner join usuario_dispositivo as ud on ud.uuid_dispositivo = d.uuid inner join usuario as u on u.uuid_cliente = ud.uuid_usuario set d.modo_viagem = ".$modo_viagem." where u.uuid_cliente = '".$id_cliente."';";
            
            $db = new db();
            $pdo = $db->connect();
            $pdo->beginTransaction();
            $pdo->query("SET SQL_SAFE_UPDATES = 0;");
            $pdo->query($sql_update);

            $stmt = $pdo->query($sql);
            $user = $stmt->fetchAll(PDO::FETCH_OBJ);
               $pdo->commit();

               $code = (object) ['code' => 200];
            if(count($user)>0){
               echo json_encode(array_merge((array) $user, (array) $code));
           }else{
               echo '{"code":902}';
           }

            
            $pdo = null;
        } catch (\PDOException $e) {
            $pdo->rollBack();
            echo '{"text": "' . $e->getMessage() . '", "code":903}';
        }
    });


$app->post('/api/login', function (Request $request, Response $reponse, array $args) {
    $email = $request->getParam('email');
    $senha = $request->getParam('senha');

    try {
        $sql = "select usuario.uuid_cliente, usuario.uuid, usuario.nivel, usuario.usuario, cliente.modelo, cliente.ativo from usuario  inner join cliente on cliente.uuid = usuario.uuid_cliente where usuario.senha = '".$senha."' and usuario.usuario = '".$email."'";
        
        $db = new db();
        $pdo = $db->connect();

        $stmt = $pdo->query($sql);
        $user = $stmt->fetchAll(PDO::FETCH_OBJ);

        $pdo = null;
           $code = (object) ['code' => 200];
        if(count($user)>0){
           echo json_encode(array_merge((array) $user[0], (array) $code));
       }else{
           echo '{"code":902}';
       }

        
        $pdo = null;
    } catch (\PDOException $e) {
        echo '{"text": "' . $e->getMessage() . '", "code":900}';
    }
});



//make a post request
$app->put('/api/users/update/{id}', function (Request $request, Response $reponse, array $args) {
    $id = $request->getAttribute('id');

    $first_name = $request->getParam('first_name');
    $last_name = $request->getParam('last_name');
    $phone = $request->getParam('phone');

    try {
        //get db object
        $db = new db();
        //conncect
        $pdo = $db->connect();


        $sql = "UPDATE  users SET first_name =?, last_name=?, phone=? WHERE id=?";


        $pdo->prepare($sql)->execute([$first_name, $last_name, $phone, $id]);

        echo '{"notice": {"text": "User '. $first_name .' has been just updated now"}}';
        $pdo = null;
    } catch (\PDOException $e) {
        echo '{"error": {"text": ' . $e->getMessage() . '}}';
    }
});


//make a post request
$app->delete('/api/users/delete/{id}', function (Request $request, Response $reponse, array $args) {
    $id = $request->getAttribute('id');

    try {
        //get db object
        $db = new db();
        //conncect
        $pdo = $db->connect();

        $sql = "DELETE FROM users WHERE id=?";

        $pdo->prepare($sql)->execute([$id]);
        $pdo = null;

        echo '{"notice": {"text": "User with '. $id .' has been just deleted now"}}';

    } catch (\PDOException $e) {
        echo '{"error": {"text": ' . $e->getMessage() . '}}';
    }
});
 


$app->run();
