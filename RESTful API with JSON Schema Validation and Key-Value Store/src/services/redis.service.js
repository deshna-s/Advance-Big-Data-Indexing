const redis = require("redis");
const config = require("../../config/local.json");

const client = redis.createClient(config.DB_PORT, config.DB_HOST);

client.on("connect", () => {
  console.log("Redis Client Connected!");
});

client.on("error", (err) => {
  console.log("Redis Client Error", err);
});

client.connect();

const ifKeyExists = async (key) => {
  const data = await client.exists(key);
  return !!data;
};

const getETag = async (key) => {
  return await client.hGet(key, "eTag");
};

const setETag = async (key, eTag) => {
  return await client.hSet(key, "eTag", eTag);
};

const addSetValue = async (key, value) => {
  return await client.sAdd(key, value);
};

const hSet = async (key, field, value) => {
  return await client.hSet(key, field, value);
};

const getKeys = async (pattern) => {
  return await client.keys(pattern);
};

const deleteKeys = async (keys) => {
  return await client.del(keys);
};

const getAllValuesByKey = async (key) => {
  return await client.hGetAll(key);
};

const sMembers = async (key) => {
  return await client.sMembers(key);
};

const getTopLevelPlanKeys = async () => {
  const keys = await client.keys("plan_*");
  output = [];
  for (let i = 0; i < keys.length; i++) {
    const key = keys[i];
    const parts = key.split("_");
    if (parts.length === 2) {
      output.push(key);
    }
  }
  return output;
};

module.exports = {
  ifKeyExists,
  getETag,
  setETag,
  addSetValue,
  hSet,
  getKeys,
  deleteKeys,
  getAllValuesByKey,
  sMembers,
  getTopLevelPlanKeys,
};
