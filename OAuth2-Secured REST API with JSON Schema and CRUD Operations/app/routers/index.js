const medical = require('./medical.js');

const value = (app)=>{
    app.use('/v1/',medical);
}

module.exports=value;