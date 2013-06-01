<?php
/**
 * Author: DeathfireD
 * Date: 2013-June-01
 * Desc: This checks if a client's port is actually open.
 */

$ip = $_SERVER['REMOTE_ADDR'];
$port = $_POST['port'];

if(fsockopen($ip,$port)){
	print "OPEN";
}else {
	print "CLOSED";
}
?>