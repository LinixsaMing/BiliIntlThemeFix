# 如何把它发到 GitHub

仓库已经初始化好并完成首次提交，只差"建远程库 + 推送"。

## 一、在 GitHub 上建空仓库

用 **GitHub 移动端 App** 也可以：右下角 `+` → **New repository** → 名字填 `BiliIntlThemeFix`
→ Public → **三项初始化选项全部不要勾** → 创建。

> 注意：手机 App 只能建仓库，**没法批量上传代码**，推代码还是得用下面的方式。

网页端步骤：

1. 打开 https://github.com/new
2. Repository name 填 **BiliIntlThemeFix**（或你喜欢的名字）
3. 可见性选 **Public**
4. **不要**勾选 "Add a README file" / ".gitignore" / "license"（我们已经有）
5. 点 Create repository

## 二、初始化并推送（三种方式选一）

> 手机里的这份**没有带 .git**（避免不同设备间权限冲突），所以先自己 git init 一次。

### 方式 0：一键脚本（最省事）

在 Termux 里：

```bash
cd /sdcard/deepseek/BiliIntlThemeFix
# 先把 push.sh 里的 USERNAME 改成你的 GitHub 用户名
sh push.sh
```

### 方式 A：电脑上

把手机里的 `/sdcard/deepseek/BiliIntlThemeFix` 整个文件夹拷到电脑，然后：

```bash
cd BiliIntlThemeFix
git init -b main
git add .
git commit -m "feat: 初始化 BiliIntlThemeFix"
git remote add origin https://github.com/<你的用户名>/BiliIntlThemeFix.git
git push -u origin main
```

推送时会要求输入用户名和密码 —— 密码位置**必须填 Personal Access Token**，不是账号密码。
Token 在这里生成：https://github.com/settings/tokens （勾 `repo` 权限）。

### 方式 B：手机上用 Termux

```bash
pkg install git
cd /sdcard/deepseek/BiliIntlThemeFix
git init -b main
git add .
git commit -m "feat: 初始化 BiliIntlThemeFix"
git remote add origin https://github.com/<你的用户名>/BiliIntlThemeFix.git
git push -u origin main
```

如果报 `dubious ownership`，先执行：

```bash
git config --global --add safe.directory /sdcard/deepseek/BiliIntlThemeFix
```

### 方式 C：先在手机上打包，再传到电脑

```bash
cd /sdcard/deepseek
zip -r BiliIntlThemeFix.zip BiliIntlThemeFix
```

## 三、推送完之后

1. Actions 页面会自动跑一次构建（`build.yml`），产出 APK
2. 打 tag 触发 Release：
   ```bash
   git tag v0.3.3
   git push origin v0.3.3
   ```
3. 记得把 `app/src/main/java/com/bilifix/themefix/Const.java` 里的
   `GITHUB_URL` 改成真实地址，再提交一次

## 四、建议补的东西

- 仓库 About 里写一句简介 + 勾上 Releases
- 仓库 Settings → Features 打开 Issues
- 如果你想用别的协议，改 `LICENSE`
