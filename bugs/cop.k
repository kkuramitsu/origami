
unit [km] = [m] * 1000
enum 距離 = 安全距離|!安全距離

context Main |>
  let (->) (x: [m]) :距離 =
    if ( x < 30:0) then 安全距離 else !安全距離 
  
context Rain |>
  let (->) (x: [m]) :距離 = 
　　if (x < 60.0[m]) then 安全距離 else !安全距離


