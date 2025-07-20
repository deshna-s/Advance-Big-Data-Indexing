const express = require('express');

const planRoute = require('./plan.route');

const router = express.Router();

const routes = [{
    path: '/plan',
    route: planRoute,
}];


routes.forEach((route) => {
    router.use(route.path, route.route);
});

module.exports = router;