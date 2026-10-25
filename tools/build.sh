#!/bin/bash
# 本地/CI 通用构建：javac -> d8 -> aapt -> v2/v3 签名
# 依赖（可用环境变量覆盖）：
#   AJ     android.jar 路径
#   TOOLS  存放 r8.jar / apksig.jar / protobuf.jar 的目录（signer 类也编译在此）
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
AJ=${AJ:-/usr/lib/android-sdk/platforms/android-23/android.jar}
TOOLS=${TOOLS:-/root/tools}
OUT=$ROOT/build
rm -rf "$OUT"; mkdir -p "$OUT/stubs" "$OUT/classes" "$OUT/dex" "$OUT/apk"

[ -f "$AJ" ] || { echo "[x] 找不到 android.jar: $AJ"; exit 1; }
[ -f "$TOOLS/r8.jar" ] || { echo "[x] 找不到 $TOOLS/r8.jar"; exit 1; }

echo "[1/5] 编译 stub"
javac -nowarn -encoding UTF-8 -d "$OUT/stubs" $(find "$ROOT/stubs" -name '*.java')
jar cf "$OUT/stubs.jar" -C "$OUT/stubs" .

echo "[2/5] 编译模块源码"
javac -nowarn -encoding UTF-8 -Xlint:none -source 8 -target 8 \
  -classpath "$AJ:$OUT/stubs.jar" -d "$OUT/classes" \
  $(find "$ROOT/app/src/main/java" -name '*.java')

echo "[3/5] dex (d8)"
java -cp "$TOOLS/r8.jar" com.android.tools.r8.D8 --min-api 26 \
  --lib "$AJ" --lib "$OUT/stubs.jar" \
  --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')

echo "[4/5] 打包"
python3 - "$ROOT/app/src/main/AndroidManifest.xml" "$OUT/AndroidManifest.xml" com.bilifix.themefix <<'PY'
import sys
src, dst, pkg = sys.argv[1], sys.argv[2], sys.argv[3]
s = open(src, encoding='utf-8').read()
s = s.replace('<manifest xmlns:android="http://schemas.android.com/apk/res/android"',
              '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="%s"' % pkg, 1)
open(dst, 'w', encoding='utf-8').write(s)
PY
aapt package -f -M "$OUT/AndroidManifest.xml" -I "$AJ" \
  -A "$ROOT/app/src/main/assets" -F "$OUT/apk/base.apk"
python3 - "$OUT/apk/base.apk" "$OUT/dex/classes.dex" "$OUT/BiliIntlThemeFix.apk" <<'PY'
import sys, zipfile, shutil
shutil.copyfile(sys.argv[1], sys.argv[3])
with zipfile.ZipFile(sys.argv[3], 'a', zipfile.ZIP_DEFLATED) as z:
    z.write(sys.argv[2], 'classes.dex')
PY

echo "[5/5] 签名 (v2 + v3)"
KS=$ROOT/keystore.jks
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -alias k -keyalg RSA -keysize 2048 -validity 10000 \
    -storetype PKCS12 -storepass android -keypass android \
    -dname "CN=BiliIntlThemeFix, O=OpenSource" >/dev/null 2>&1
fi
mkdir -p "$TOOLS/signer"
[ -f "$TOOLS/signer/ApkSign.class" ] || \
  javac -nowarn -cp "$TOOLS/apksig.jar:$TOOLS/protobuf.jar" -d "$TOOLS/signer" "$ROOT/tools/signer/ApkSign.java"
java -cp "$TOOLS/apksig.jar:$TOOLS/protobuf.jar:$TOOLS/signer" ApkSign \
  "$OUT/BiliIntlThemeFix.apk" "$OUT/BiliIntlThemeFix-signed.apk" "$KS" k android 26
echo "built: $OUT/BiliIntlThemeFix-signed.apk"
