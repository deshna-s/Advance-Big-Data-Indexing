const express = require('express');
const cors = require('cors');
const routes = require('./routers/index.js');

const app = express();

app.use(express.json());
app.use(express.urlencoded());
app.use(cors());

routes(app);

module.exports= app;
