#!/bin/bash

mkdir -p target/slosh/META-INF/resources/slosh
pushd target/slosh/META-INF/resources/slosh

# tag file to ensure it exists
echo "this is the slosh jar" > exists.txt

# Raw slosh character
mkdir "a\\a"
mkdir "b\\"
mkdir "\\c"
echo "this is root there" > "root\\there.txt"
echo "this is content in a\a/foo.txt" > "a\\a/foo.txt"
echo "this is more content in b\/bar.txt" > "b\\/bar.txt"
echo "this is even more content in \c/zed.txt" > "\\c/zed.txt"

# Encoded slosh character
mkdir "a%5Ca"
mkdir "b%5C"
mkdir "%5Cc"
echo "this is root%5Cthere" > "root%5Cthere.txt"
echo "this is content in a%5Ca/foo.txt" > "a%5Ca/foo.txt"
echo "this is more content in b%5C/bar.txt" > "b%5C/bar.txt"
echo "this is even more content in %5Cc/zed.txt" > "%5Cc/zed.txt"

popd
pushd target/slosh
jar -cvf ../slosh.jar .
popd

cp target/slosh.jar slosh.jar