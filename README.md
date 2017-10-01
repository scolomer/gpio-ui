
Connexion avec wscat au WS des devices
wscat -c ws://localhost:9000/ws/devices

Déclarer un device:
{"id":12,"description":"Cuisine d'été","value":0}

Changer la valeur du device :
curl 'http://localhost:9000/rest/device/12?value=1'


Cnnexion avec wscat au WS UI
wscat -c ws://localhost:9000/ws/ui
