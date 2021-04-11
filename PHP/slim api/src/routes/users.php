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


$app->post('/api/login', function (Request $request, Response $reponse, array $args) {
    $email = $request->getParam('email');
    $senha = $request->getParam('senha');

    try {
        $sql = "select uuid, nivel, usuario from usuario  where senha = '".$senha."' and usuario = '".$email."'";
        $db = new db();
        $pdo = $db->connect();

        $stmt = $pdo->query($sql);
        $user = $stmt->fetchAll(PDO::FETCH_OBJ);

        $pdo = null;

        echo json_encode($user[0]);
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
