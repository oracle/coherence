#!/bin/sh
COH_DIST=$1
WLS_COH_FILE=/tmp/wls.coh.bits
CURRENT_COH_FILE=/tmp/current.coh.bits
WLS_OPEN_FILE=/tmp/wls.opened.files
WLS_ADD_FILE=/tmp/wls.add.files
WLS_DEL_FILE=/tmp/wls.delete.files
WLS_EDIT_FILE=/tmp/wls.edit.files
#recorde coherence bits in WLS to /tmp/wls.coh.bits
p4 -p p4server:6999 -P neVer2046 files  //depot/dev/src1212/adelabels/coh/dist/oracle.coherence/coherence/... | grep -v "delete change" |cut -d ' ' -f 1 > $WLS_COH_FILE
#recorde coherence bits in current build to /tmp/current.coh.bits
rm -rf $CURRENT_COH_FILE $WLS_OPEN_FILE $WLS_ADD_FILE $WLS_DEL_FILE $WLS_EDIT_FILE
find $COH_DIST/bin $COH_DIST/lib -type f | while read i; do
  echo $i | sed -e "s|$COH_DIST||" >> $CURRENT_COH_FILE
done
find $COH_DIST -maxdepth 1 -type f | while read i; do
  echo $i | sed -e "s|$COH_DIST||" >> $CURRENT_COH_FILE
done
#$WLS_ADD_FILE for files need to add to WLS, $WLS_DEL_FILE for need to delete in WLS, $WLS_EDIT_FILE for edit
while read i ; do
  grep $i $WLS_COH_FILE >> $WLS_EDIT_FILE
  if [ $? -eq 0 ]; then
    sed -i "s|.*$i.*||" $WLS_COH_FILE 
  else
    echo "//depot/dev/src1212/adelabels/coh/dist/oracle.coherence/coherence$i#1 - add default change" >> $WLS_ADD_FILE
  fi
done < $CURRENT_COH_FILE
# mark edit flag on $WLS_EDIT_FILE
sed -i  "s|$| - edit default change|" $WLS_EDIT_FILE
sed -i '/^$/d' $WLS_COH_FILE 
# files leave in $WLS_COH_FILE need to be deleted
if [ -s $WLS_COH_FILE ]; then
  cp $WLS_COH_FILE $WLS_DEL_FILE
  sed -i  "s|$| - delete default change|" $WLS_DEL_FILE
fi
if [ -f $WLS_EDIT_FILE ]; then
  cat  $WLS_EDIT_FILE > p4opened.files
  edit_filecount=`cat $WLS_EDIT_FILE | wc -l`
else
   edit_filecount=0
fi
if [ -f $WLS_ADD_FILE ]; then
  cat $WLS_ADD_FILE >> p4opened.files
  add_filecount=`cat $WLS_ADD_FILE | wc -l`
else
  add_filecount=0
fi
if [ -f $WLS_DEL_FILE ]; then
  cat $WLS_DEL_FILE >> p4opened.files
  del_filecount=`cat $WLS_DEL_FILE | wc -l`
else
  del_filecount=0
fi
echo "In this build, coherence bits editd: $edit_filecount, added: $add_filecount, deleted: $del_filecount. "
