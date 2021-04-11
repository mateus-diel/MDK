<?php
class db
{

    public function connect()
    {
        $host = "187.109.226.100:8585";
        $user = "mdk";
        $pass = "@grimelemon1";
        $dbname = "marcoa";

        //connect database using php pdo wrapper 
        $pdo = new PDO("mysql:host=$host;dbname=$dbname", $user, $pass);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        return $pdo;
    }
}
