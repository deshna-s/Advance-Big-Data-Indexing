var redis = require('redis')
var validate = require('jsonschema').validate;
var schema = require('./../schema_json/schema.json');
var linkedPlanServiceSchema = require('./../schema_json/linkedPlanServiceSchema.json')
var axios = require('axios')
var etag = require('etag')
const { response } = require('../app');


(async () => {
    redisClient = redis.createClient();
  
    redisClient.on("error", (error) => console.error(`Error : ${error}`));
  
    await redisClient.connect();
  })();

async function verifyToken(req,res){
  const token = req.headers.authorization;
  try {
    const rest = await axios({
			url: "https://oauth2.googleapis.com/tokeninfo?access_token="+token.split(" ")[1],
			method: "get",
		});
    return true;
  } catch (error) {
    return false;
  }

}   

const post = async(req,res)=>{

  if(await verifyToken(req,res)){
    var validator = validate(req.body, schema)
    var prms = await redisClient.set(req.body.objectType + ":" + req.body.objectId, JSON.stringify(req.body))
    if (validator.valid == true && prms == "OK") {
      res.status(201).send(JSON.stringify(req.body));
    }
    else {
      res.status(400).send("Data does not match schema")
    }
  }
  else{
    res.status(401).send("Unauthorized !!");
  }
}

const get = async(req,res)=>{

  if(await verifyToken(req,res)){
    result = []
    const keys = await redisClient.keys('*');
    for (const key of keys) {
      const data = await redisClient.get(key);
      result.push(JSON.parse(data));
    }
    res.send(result);
  }
  else{
    res.status(401).send("Unauthorized !!");
  }
}

const getById = async(req,res)=>{

  if(await verifyToken(req,res))
  {
    var key = "plan:" + req.params.id
    const result = await redisClient.get(key)
    if (result == null) {
      res.status(404).send()
    }
    else {
      res.status(200).send(JSON.parse(result))
    }
  }
  else{
    res.status(401).send("Unauthorized !!");
  }    
}

const deleteById = async(req,res)=>{

  if(await verifyToken(req,res)){
    var key = "plan:" + req.params.id
    var etagReq = req.headers.etag;
    const result = await redisClient.get(key)
    if (result == null) {
      res.status(404).send()
      return 
    }
    var etagRes = "W/"+etag(result)
    if (etagReq == undefined || etagReq == null || etagReq == '' || etagReq != etagRes)
    {
          res.status(412).send({error: "Etag is not valid"})
          return;
    }
    if (result == null) {
      res.status(404).send()
    }
    else {
      redisClient.del(key)
      res.status(204).send()
    }
  }
  else{
    res.status(401).send("Unauthorized !!");
  } 
}

const putById = async(req,res)=>{

  if(await verifyToken(req,res)){
    var key = "plan:" + req.params.id
    var etagReq = req.headers.etag;
    const result = await redisClient.get(key)
    if (result == null) {
      res.status(404).send()
      return 
    }
    var etagRes = "W/"+etag(result)
    if (etagReq == undefined || etagReq == null || etagReq == '' || etagReq != etagRes)
    {
        res.status(412).send({error: "Etag is not valid"})
        return;
    }
    if (result == null) {
      res.status(404).send()
    }
    else {
      var validator = validate(req.body, schema)
      if (validator.valid == true) 
      {
        
        const r = await redisClient.set(key,JSON.stringify(req.body))
      }
      else {
        res.status(400).send("Data does not match schema")
      }
      
      res.status(204).send()
    }
  }
  else{
    res.status(401).send("Unauthorized !!");
  } 
}

const patchById = async(req,res)=>{

  if(await verifyToken(req,res)){
  var validator = validate(req.body, linkedPlanServiceSchema)
  if (validator.valid != true) {
    res.status(400).send({error: "Data does not match schema specified"})
    return;
  }

  var key = "plan:" + req.params.id
  var etagReq = req.headers.etag;
  const result = await redisClient.get(key)
  if (result == null) {
    res.status(404).send()
    return 
  }
  var etagRes = "W/"+etag(result)
  if (etagReq == undefined || etagReq == null || etagReq == '' || etagReq != etagRes) {
    res.status(412).send({error: "Etag is not valid or object is not latest version"})
    return;
  }
  if (result == null) {
    res.status(404).send()
    return;
  }
  else {
    var reqvals = req.body['linkedPlanServices'];
    var resval = JSON.parse(result);
    reqvals.forEach(element => {
      resval['linkedPlanServices'].push(element);
    });
    req.body['linkedPlanServices'] = resval['linkedPlanServices']
    await redisClient.set(key, JSON.stringify(req.body))
    var sd = await redisClient.get(key)
    res.status(200).send(JSON.parse(sd))
    return;
  }
}
else{
  res.status(401).send("Unauthorized !!");
}

}

module.exports={post,get,getById,deleteById,putById,patchById};