#!/bin/bash
# ============================================================
#  一键推送脚本（在 Termux 或电脑的 Git Bash 里运行）
#  用法：
#    1. 把下面 USERNAME 改成你的 GitHub 用户名
#    2. bash push.sh
# ============================================================
set -e

USERNAME="LinixsaMing"          # ← 改成你的 GitHub 用户名
REPO="BiliIntlThemeFix"
TAG="v0.3.3"

if [ "$USERNAME" = "CHANGE_ME" ]; then
    echo "[x] 请先把脚本里的 USERNAME 改成你的 GitHub 用户名"
    exit 1
fi

cd "$(dirname "$0")"

# git 没初始化就初始化
if [ ! -d .git ]; then
    echo "[1] 初始化仓库"
    git init -b main
    git config user.name  "$USERNAME"
    git config user.email "$USERNAME@users.noreply.github.com"
else
    echo "[1] 仓库已存在"
    git config --global --add safe.directory "$(pwd)" 2>/dev/null || true
fi

echo "[2] 暂存并提交"
git add -A
if git diff --cached --quiet; then
    echo "    没有新改动"
else
    git commit -m "feat: BiliIntlThemeFix ${TAG}"
fi

echo "[3] 配置远程"
if git remote get-url origin >/dev/null 2>&1; then
    git remote set-url origin "https://github.com/${USERNAME}/${REPO}.git"
else
    git remote add origin "https://github.com/${USERNAME}/${REPO}.git"
fi

echo "[4] 推送到 https://github.com/${USERNAME}/${REPO}"
git branch -M main
git push -u origin main

echo
echo "✅ 完成。下一步："
echo "   git tag ${TAG} && git push origin ${TAG}    # 打 tag 触发 Release"
echo "   记得把 Const.java 里的 GITHUB_URL 改成真实地址"
