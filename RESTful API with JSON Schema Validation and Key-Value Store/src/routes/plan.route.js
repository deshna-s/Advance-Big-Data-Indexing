const express = require("express");
const { planController } = require("../controllers");

const router = express.Router();
// const { verifyAccessToken } = require("../middleware/auth");

router.route("/").post(planController.createPlan);
router.route("/").get(planController.getPlans);

router
  .route("/:objectId")
  .get(planController.getPlan)
  .delete(planController.deletePlan)
  .patch(planController.patchPlan);

module.exports = router;
