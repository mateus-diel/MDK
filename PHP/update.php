<?php
  $filename="update.bin";
  header("Content-type: application/octet-stream");
  header("Content-disposition: attachment;filename=$filename");
  header('Content-Length: '.filesize($filename));
  readfile($filename);
?>