<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<script language="javascript"> 
	<?php
		include("js/datos.php"); 
	?> 
</script>


<script type="text/javascript">
var c=0;

var t;
var timer_is_on=0;

function timedCount()
{
document.getElementById('postTitle').innerHTML=titulos[c];
document.getElementById('postBody').innerHTML=datos[c];
document.getElementById('imagenDelMomento').src=src[c];

c=c+1;
if(c==4)
{
c=0;
}

t=setTimeout("timedCount()",10000);
}

function doTimer()
{
if (!timer_is_on)
  {
  timer_is_on=1;
  timedCount();
  }
}
</script> 

<title>Pagina Insilico</title>
<link rel="stylesheet" type="text/css" href="css/test.css" />
</head>

<body onload="doTimer()">

<div id="content1">
<p>
<p class="Titulo" id="insilico" name="insilico"> Investigaci&oacute;n con funci&oacute;n social</p>
  <p class="Titulo" id="insilico2" name="insilico2">INS&Iacute;LICO</p>
</p>

			<div id="content2">
			  <p class="Titulo" id="postTitle" > Inteligencia Artificial</p>
			<p class="sss" id="postBody" >
	  la IA trata de �desarrollar sistemas que piensen y act�en racionalmente� V. Juli�n, V. Botti. ��podemos pensar que la IA, en su conjunto, trata realmente de construir precisamente dichas entidades aut�nomas e inteligentes (los agentes inteligentes).� V. Juli�n, V. Botti.
			</p>
			<br />
			<br />
			<br />
  <img src="imagenes/momento1.jpg" id="imagenDelMomento" /></div>

<div id="content3">
<img src="css/Picture1.jpg" alt="imagen1" width="135" height="131" />
<p id="informacion">
Trabajamos en este momento en el desarrollo de una aplicaci�n, usando distintas t�cnicas de IA
</p>
</div>
<div id="content32">
<img src="css/Picture2.jpg" alt="imagen2" width="131" height="137" />
<p id="informacion">Buscamos soluci�n al problema de los �Polic�as Corruptos�.
</p>
</div>

<div id="forma">
<form name="login" method="post" action="index.php">
<input type="text" name="usuario" id="usuario" value="Usuario" onclick="this.value=''" />
<br />
<input type="password" name="clave" id="clave" />
<br />
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="submit" id="enviar" name="submit" value="Ingresar"/>
</form>

<?php
	if ($_POST['submit'] ) {
		$user = $_POST['usuario'];
		$passwd = $_POST['clave'];
		include("conn.php");
		$result = mysql_query("Select * from insilico.Integrante where UserId='$user'");
		$row = mysql_fetch_assoc($result);
		if ($row && $row['Password'] == $passwd) {
			echo "Correcto";
		} else {
			echo "Incorrecto";				
		}
		echo "<br />$user - $passwd";
		mysql_close($cnn);
	}
?>

</div>

<div id="botones">
<table>
<tr onmouseover="document.body.style.cursor='crosshair'" onmouseout="document.body.style.cursor='default'">
<td class="celdas" onmouseover="style.backgroundColor='#95B3D7'" onmouseout="style.backgroundColor='transparent'" onclick="document.location.href='http://www.google.com'">El Grupo</td>
<td class="celdas" onmouseover="style.backgroundColor='#95B3D7'" onmouseout="style.backgroundColor='transparent'" onclick="document.location.href='http://www.google.com'">Nuestro Trabajo</td>
<td class="celdas" onmouseover="style.backgroundColor='#95B3D7'" onmouseout="style.backgroundColor='transparent'" onclick="document.location.href='http://www.google.com'">Maratones</td>
<td class="celdas" onmouseover="style.backgroundColor='#95B3D7'" onmouseout="style.backgroundColor='transparent'" onclick="document.location.href='http://www.google.com'">Integrantes</td>
</tr>

</table>

</div>

<div id="enlaces" >
<a href="http://www.utp.edu.co" > Universidad Tecnologica de Pereira</a> &bull;
<a href="http://isc.utp.edu.co" > Ingenieria de Sistemas y computacion</a>&bull;
<a href="http://www.google.com.co" > Contactenos</a>


</div>

</div>
</body>

</html>
