const app = require('./app/app.js');

const port =3000;
app.listen(port,()=>{
    console.log(`Server is running on port ${port}`);
});