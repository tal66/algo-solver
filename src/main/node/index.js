const dgram = require("node:dgram");

const port = 8010;
const server = dgram.createSocket("udp4");

server.bind(port);

server.on("listening", () => {
  console.log(`listening on ${port}`);
});

let stats = {};

setInterval(() => {
  console.clear();
  console.table(stats);
}, 350);

server.on("message", (msg, info) => {
  try {
    let item_stats = JSON.parse(msg);
    let algo = item_stats.algo;
    delete item_stats["algo"];
    stats[algo] = item_stats;
  } catch (e) {}
});
