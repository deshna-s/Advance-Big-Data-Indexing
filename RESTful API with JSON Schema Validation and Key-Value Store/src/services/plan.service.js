const {
  addSetValue,
  hSet,
  deleteKeys,
  getAllValuesByKey,
  sMembers,
  getKeys,
  setETag,
  getTopLevelPlanKeys,
} = require("./redis.service");
const hash = require("object-hash");

const schema = require("../models/plan.model");

const getAllPlans = async () => {
  const output = [];
  const topkeys = await getTopLevelPlanKeys();

  for (const topkey of topkeys) {
    const plan = await getSavedPlan(topkey);
    output.push(plan);
  }
  return output;
};

const getSavedPlan = async (key) => {
  const output = await getOrDeletePlanData(key, {}, false);
  return output;
};

const createSavePlan = async (key, plan) => {
  await convertJSONToMap(plan);
  return getOrDeletePlanData(key, {}, false);
};

const convertJSONToMap = async (json) => {
  const valueMap = {};
  const map = {};

  for (let [key, value] of Object.entries(json)) {
    const redisKey = `${json["objectType"]}_${json["objectId"]}`;
    if (Array.isArray(value)) {
      value = await convertToList(value);
      for (let [_, valueArray] of Object.entries(value)) {
        for (let [keyInnerArray, _] of Object.entries(valueArray)) {
          await addSetValue(`${redisKey}_${key}`, keyInnerArray);
        }
      }
    } else if (typeof value === "object") {
      value = await convertJSONToMap(value);
      const calculatedValue = Object.keys(value)[0];
      await addSetValue(`${redisKey}_${key}`, calculatedValue);
    } else {
      await hSet(redisKey, key, value.toString());
      valueMap[key] = value;
      map[redisKey] = valueMap;
    }
  }
  return map;
};

const convertToList = async (array) => {
  let list = [];
  for (let i = 0; i < array.length; i++) {
    let value = array[i];
    if (Array.isArray(value)) {
      value = await convertToList(value);
    } else if (typeof value === "object") {
      value = await convertJSONToMap(value);
    }
    list.push(value);
  }
  return list;
};

const getOrDeletePlanData = async (redisKey, outputMap, isDelete) => {
  const keys = await getKeys(`${redisKey}*`);
  for (let l = 0; l < keys.length; l++) {
    const key = keys[l];

    if (key === redisKey) {
      if (isDelete) {
        deleteKeys([key]);
      } else {
        const val = await getAllValuesByKey(key);
        for (let [keyName, _] of Object.entries(val)) {
          if (keyName.toLowerCase() !== "etag") {
            outputMap[keyName] = !isNaN(val[keyName])
              ? Number(val[keyName])
              : val[keyName];
          }
        }
      }
    } else {
      const newStr = key.substring(`${redisKey}_`.length);
      const members = [...(await sMembers(key))];
      console.log(members);
      if (members.length > 1) {
        const listObj = [];
        for (let i = 0; i < members.length; i++) {
          const member = members[i];
          if (isDelete) {
            await getOrDeletePlanData(member, null, true);
          } else {
            listObj.push(await getOrDeletePlanData(member, {}, false));
          }
        }
        if (isDelete) {
          await deleteKeys([key]);
        } else {
          outputMap[newStr] = listObj;
        }
      } else {
        if (isDelete) {
          await getOrDeletePlanData(members[0], null, true);
          await deleteKeys([members[0], key]);
        } else {
          const val = await getAllValuesByKey(members[0]);
          const newMap = {};

          for (let [keyName, _] of Object.entries(val)) {
            newMap[keyName] = !isNaN(val[keyName])
              ? Number(val[keyName])
              : val[keyName];
          }

          if (
            schema.properties &&
            schema.properties[newStr] &&
            schema.properties[newStr].type === "array"
          ) {
            outputMap[newStr] = [
              await getOrDeletePlanData(members[0], newMap, false),
            ];
          } else {
            outputMap[newStr] = newMap;
          }
        }
      }
    }
  }

  return outputMap;
};

const deleteSavedPlan = async (key) => {
  await getOrDeletePlanData(key, {}, true);
};

const generateETag = (key, jsonObject) => {
  const eTag = hash(jsonObject);
  setETag(key, eTag);
  return eTag;
};

module.exports = {
  getSavedPlan,
  convertJSONToMap,
  createSavePlan,
  getOrDeletePlanData,
  deleteSavedPlan,
  generateETag,
  getAllPlans,
};
