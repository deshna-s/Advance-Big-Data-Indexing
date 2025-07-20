const express = require('express');
const medicalcontroller = require('./../controllers/medical.js');

const router = express.Router();

router.route('/plan')
    .post(medicalcontroller.post)
    .get(medicalcontroller.get);

router.route('/plan/:id')
    .get(medicalcontroller.getById)
    .delete(medicalcontroller.deleteById)
    .put(medicalcontroller.putById)
    .patch(medicalcontroller.patchById);

module.exports= router;