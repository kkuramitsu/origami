sum(a,i,b) =
  if(i < |a|){
    sum(a,i+1,b+a[i])
  }else{
    b
  }
//sum([0,1],0,0)

