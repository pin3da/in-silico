<?php

	$cnn = mysql_connect('localhost', 'in-silico', 'in-silico');
	if (!$cnn) {
   		die('Could not connect: ' . mysql_error());
	} 
	mysql_select_db("insilico",$cnn);
?>
