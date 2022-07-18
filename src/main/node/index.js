const net = require("net");

const port = 8010;
const server = net.createServer();

server.listen(port, function () {
  console.log(`listening on ${port}`);
});

let stats = {};

server.on("connection", function (socket) {
  console.log(`new connection ${socket.remotePort}`);

  setInterval(() => {
    console.clear();
    console.table(stats);
  }, 250);

  socket.on("data", function (chunk) {
    const data = chunk.toString();
    data.split("\n").forEach((item) => {
      if (item.length > 1) {
        try {
          let item_stats = JSON.parse(item);
          let algo = item_stats.algo;
          delete item_stats["algo"];
          stats[algo] = item_stats;
        } catch (e) {}
      }
    });
  });

  socket.on("end", function () {
    console.log("ending connection");
  });

  socket.on("error", function (err) {
    console.log(`error: ${err}`);
  });
});
