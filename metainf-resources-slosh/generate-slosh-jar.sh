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
mkdir "a%2fa"
mkdir "b%2f"
mkdir "%2fc"
echo "this is root%2Fthere" > "root%2Fthere.txt"
echo "this is content in a%2fa/foo.txt" > "a%2fa/foo.txt"
echo "this is more content in b%2f/bar.txt" > "b%2f/bar.txt"
echo "this is even more content in %2fc/zed.txt" > "%2fc/zed.txt"

popd
pushd target/slosh
jar -cvf ../slosh.jar .
popd

cp target/slosh.jar slosh.jar