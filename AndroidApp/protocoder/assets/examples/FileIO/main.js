/* 
*	Simple File Input and Output operations 
*   
*/ 

ui.toast("saving and loading data");

// create an Arrary with the data we want to save 
var data = new Array();
data.push("hello 1");
data.push("hello 2");
data.push("hello 3");
data.push("hello 4");
data.push("hello 5");

//saving data in file.txt 
fileio.saveStrings("file.txt", data);

//read data and store it in readData
var readData = fileio.loadStrings("file.txt");

//show in the console the data
for(var i = 0; i < readData.length; i++) { 
  console.log(readData[i]);  
} 