// {"id":12,"description":"Cuisine d'été","value":0},
// {"id":2,"description":"Leds cheminée","value":0},
//{"id":3,"description":"Spots table séjour","value":1}

var app = new Vue({
  el: '#app',
  template: '#main-page',
  data: {
    devices: [
    /* {id:12, description:"Cuisine d'été", value:0},{id:13, description:"Eclairage cheminée", value:1}*/
    ]
  },
  methods: {
    clicked: (d,e) => {
      console.log("clicked " + e + " " + d.value)
      d.value = (d.value + 1) % 2
      ws.send(JSON.stringify({id: "value", payload: {id: d.id, value: d.value}}))
    }
  }
})

var prefix = location.protocol.replace("http", "ws") + '//'+location.hostname+(location.port ? ':'+location.port: '');
console.log(prefix)
var ws = new WebSocket(prefix + "/ws/ui");
setInterval(() => {
  ws.send(JSON.stringify({id: "ping"}));
}, 20000);

console.log("Connect")
ws.onmessage = a => {
  console.log("data: " + a.data)
  var m = JSON.parse(a.data)
  if (m.id == "init") {
    app.devices = m.payload
  } else if (m.id == "add" || m.id == "update") {
    var d = app.devices.find(a => a.id == m.payload.id);
    if (d == undefined) {
      d = m.payload
      app.devices.push(d)
    } else {
      if (m.payload.description != undefined) {
        d.description = m.payload.description
      }

      if (m.payload.value != undefined) {
        d.value = m.payload.value
      }
    }

  } else {
    console.log("Unhandled message : " + a.data)
  }
}
