const status = require("http-status");
const { ifKeyExists, getETag } = require("../services/redis.service");
const config = require("../../config/local.json");
const { isValidJSONSchema } = require("../services/jsonSchema.service");

const PLAN_SCHEMA = require("../models/plan.model");
const {
  createSavePlan,
  getSavedPlan,
  deleteSavedPlan,
  generateETag,
  getAllPlans,
} = require("../services/plan.service");

const getPlans = async (req, res) => {
  const plans = await getAllPlans();
  return res.status(status.OK).send(plans);
};

const getPlan = async (req, res) => {
  try {
    const { objectId } = req.params;

    const KEY = `${config.PLAN_TYPE}_${objectId}`;

    const isKeyValid = await ifKeyExists(KEY);

    if (!isKeyValid) {
      return res.status(status.NOT_FOUND).send({
        message: `Invalid ObjectId! - ${objectId}`,
        value: objectId,
        type: "Invalid",
      });
    }

    const eTag = await getETag(KEY);

    const urlETag = req.headers["if-none-match"];
    const ifMatchETag = req.headers["if-match"];

    if (!!ifMatchETag && ifMatchETag !== eTag) {
      return res.status(status.PRECONDITION_FAILED).send({
        message: `Precondition failed. ETag mismatch for ObjectId! - ${objectId}`,
        expected: ifMatchETag,
        found: eTag,
      });
    }

    if (!!urlETag && urlETag == eTag) {
      res.setHeader("ETag", eTag);
      return res.status(status.NOT_MODIFIED).send();
    }

    const plan = await getSavedPlan(KEY);
    res.setHeader("ETag", eTag);
    return res.status(status.OK).send(plan);
  } catch (error) {
    return res.status(status.UNAUTHORIZED).send({
      message: "Bad Request or Unauthorised",
    });
  }
};

const createPlan = async (req, res) => {
  try {
    const planJSON = req.body;
    if (!!!planJSON) {
      return res.status(status.BAD_REQUEST).send({
        message: "Invalid body!",
        type: "Invalid",
      });
    }

    const isValidSchema = await isValidJSONSchema(planJSON, PLAN_SCHEMA);

    if (isValidSchema?.error) {
      return res.status(status.BAD_REQUEST).send({
        message: "Invalid Schema!",
        type: "Invalid",
        ...isValidSchema?.data,
      });
    }

    const KEY = `${config.PLAN_TYPE}_${planJSON.objectId}`;

    const isKeyValid = await ifKeyExists(KEY);
    if (isKeyValid) {
      return res.status(status.CONFLICT).send({
        message: `Plan already exist! - ${planJSON.objectId}`,
        type: "Already Exists",
      });
    }
    await createSavePlan(KEY, planJSON);
    const eTag = generateETag(KEY, planJSON);

    res.setHeader("ETag", eTag);

    return res.status(status.OK).send({
      message: "Plan created successfully",
      objectId: planJSON.objectId,
    });
  } catch (error) {
    return res.status(status.UNAUTHORIZED).send({
      message: "Bad Request or Unauthorised",
    });
  }
};

const deletePlan = async (req, res) => {
  try {
    const { objectId } = req.params;

    const KEY = `${config.PLAN_TYPE}_${objectId}`;

    const isKeyValid = await ifKeyExists(KEY);

    if (!isKeyValid) {
      return res.status(status.NOT_FOUND).send({
        message: `Invalid ObjectId! - ${objectId}`,
        value: objectId,
        type: "Invalid",
      });
    }

    await deleteSavedPlan(KEY);

    return res.status(status.OK).send({
      message: "Plan deleted successfully",
      objectId,
    });
  } catch (error) {
    return res.status(status.UNAUTHORIZED).send({
      message: "Bad Request or Unauthorised",
    });
  }
};

const patchPlan = async (req, res) => {
  try {
    const { objectId } = req.params;
    const planJSON = req.body;

    const KEY = `${config.PLAN_TYPE}_${objectId}`;

    const isKeyValid = await ifKeyExists(KEY);

    if (!isKeyValid) {
      return res.status(status.NOT_FOUND).send({
        message: `Invalid ObjectId! - ${objectId}`,
        value: objectId,
        type: "Invalid",
      });
    }

    if (!!!planJSON) {
      return res.status(status.BAD_REQUEST).send({
        message: "Invalid body!",
        type: "Invalid",
      });
    }

    const isValidSchema = await isValidJSONSchema(planJSON, PLAN_SCHEMA);

    if (isValidSchema?.error) {
      return res.status(status.BAD_REQUEST).send({
        message: "Invalid Schema!",
        type: "Invalid",
        ...isValidSchema?.data,
      });
    }

    const urlETag = req.headers["if-match"] || [];
    if (urlETag && !urlETag.length) {
      return res.status(status.NOT_FOUND).send({
        message: "Etag not provided!",
      });
    }

    const eTag = await getETag(KEY);

    if (urlETag !== eTag) {
      res.setHeader("ETag", eTag);
      return res.status(status.PRECONDITION_FAILED).send();
    }

    await createSavePlan(KEY, planJSON);
    const eTagNew = generateETag(KEY, planJSON);

    const plan = await getSavedPlan(KEY);
    res.setHeader("ETag", eTagNew);
    return res.status(status.OK).send(plan);
  } catch (error) {
    return res.status(status.INTERNAL_SERVER_ERROR).send({
      message: error.message,
    });
  }
};

module.exports = {
  getPlan,
  createPlan,
  deletePlan,
  getPlans,
  patchPlan,
};
