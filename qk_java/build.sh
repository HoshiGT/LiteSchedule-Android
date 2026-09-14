#!/usr/bin/env bash
#
# LiteSchedule 纯命令行构建（不需要 Gradle）
#
# 用法：
#   ./build.sh                      # 默认版本号 5 / 1.0.4，输出 ../qingkebiao.apk
#   VERSION_CODE=6 VERSION_NAME=1.0.5 OUT=../qingkebiao_v1.0.5.apk ./build.sh
#
# 依赖：Android SDK（build-tools 35.0.0 + platform android-36）
#   可用环境变量 ANDROID_SDK 指定 SDK 路径，默认 ~/Android/Sdk
#
# ⚠️ gen/androidx.{core,customview,recyclerview,viewpager2}/R.java 是**预生成并入库**的，
#    不要在构建前删掉整个 gen/ 目录，否则编出来的 APK 会缺少 AndroidX 的 R 类，
#    装到手机上运行时会崩（NoClassDefFoundError）。本脚本只重建 gen/com（app 自己的 R.java）。
#
set -e
cd "$(dirname "$0")"

SDK="${ANDROID_SDK:-$HOME/Android/Sdk}"
BT="$SDK/build-tools/35.0.0"
AJAR="$SDK/platforms/android-36/android.jar"
VERSION_CODE="${VERSION_CODE:-5}"
VERSION_NAME="${VERSION_NAME:-1.0.4}"
KEYSTORE="${KEYSTORE:-../wakeup-local.keystore}"
KS_PASS="${KS_PASS:-localwakeup}"
OUT="${OUT:-../qingkebiao.apk}"

for f in "$BT/aapt2" "$BT/d8" "$BT/apksigner" "$AJAR"; do
    [ -e "$f" ] || { echo "缺少构建依赖：$f" >&2; exit 1; }
done
[ -f gen/androidx.recyclerview/R.java ] || {
    echo "缺少 gen/androidx.recyclerview/R.java（AndroidX 预生成 R 类），无法构建完整包" >&2; exit 1
}

rm -rf build classes gen/com
mkdir -p build classes

echo "[1/6] 编译资源"
"$BT/aapt2" compile --dir res -o build/res.zip

echo "[2/6] 链接资源并生成 R.java（版本 $VERSION_NAME / $VERSION_CODE）"
"$BT/aapt2" link \
    -I "$AJAR" \
    -R build/res.zip \
    -R ax_res_lib/core-1.9.0.zip \
    -R ax_res_lib/recyclerview-1.2.1.zip \
    -R ax_res_lib/viewpager2-1.0.0.zip \
    --manifest AndroidManifest.xml --auto-add-overlay \
    --java gen -o build/base.apk \
    --min-sdk-version 26 --target-sdk-version 36 \
    --version-code "$VERSION_CODE" --version-name "$VERSION_NAME"

echo "[3/6] 编译 Java"
CP="$AJAR:$(find libs -name '*.jar' | tr '\n' ':')"
javac -source 17 -target 17 -classpath "$CP" \
    -d classes $(find gen -name 'R.java') src/com/hoshi/qingkebiao/*.java

echo "[4/6] dex"
"$BT/d8" --lib "$AJAR" --release --output build \
    $(find classes -name '*.class') $(find libs -name '*.jar')

echo "[5/6] 打包 dex 进 APK"
(cd build && zip -q base.apk classes.dex)

echo "[6/6] 签名 -> $OUT"
"$BT/apksigner" sign --ks "$KEYSTORE" \
    --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
    --out "$OUT" build/base.apk

"$BT/aapt" dump badging "$OUT" | grep -E "^package"
echo "构建完成：$OUT"
