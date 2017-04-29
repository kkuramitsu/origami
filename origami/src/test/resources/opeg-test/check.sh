#!/bin/sh
CCMD="nez parse -Xnez.tool.LineTreeWriter -g"
gfile='iroha.nez'
#echo "$(nez parse -Xnez.tool.LineTreeWriter -g ${gfile} ${infile})"
p=''
tested=''
check2() {
 gfile=$1
 path=`basename $1 .nez`

 echo "$gfile ..."
 for i in {1..9} 
 do 
  infile="$path/$i.in"
  outfile="$path/$i.out"
  if [ -f ${infile} ]
  then
    p="$(nez parse -Xnez.tool.LineTreeWriter -g ${gfile} ${infile})"
    #echo "Parsed $p"
  fi
  if [ -f $outfile ]
  then
    tested=`cat $outfile`
    if [ "${tested}" = "${p}" ] 
    then
      echo "[PASSED] $infile"
    else
      echo "[FAILED] $infile"
      echo "  $p"
      echo "  $tested"
    fi
  else
   if [ -f $infile ]
   then
    echo "echo \"${p}\" > ${outfile}"
   fi
  fi
 done
}

for f in $@
do
 check2 $f
done

