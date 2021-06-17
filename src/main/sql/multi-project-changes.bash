#!/bin/bash -ex
# 引数で設定したフォルダ内の changes,revisionsテーブルを を1つにまとめます。

SCRIPT_DIR=`dirname $0`

LANG=$1

echo "create: ${LANG}.db"
sqlite3 $LANG.db < $SCRIPT_DIR/multi-project-changes-init.sql
for DB in `ls $LANG/*.db` ; do
    echo "merging: ${LANG}.db <- ${DB}"
    (
        echo "ATTACH DATABASE \"${DB}\" AS guest;"
        cat $SCRIPT_DIR/multi-project-changes-merge.sql
    ) | sqlite3 $LANG.db
done
