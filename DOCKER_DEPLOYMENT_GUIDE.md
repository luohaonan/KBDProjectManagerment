# KBD 项目 Docker 部署指南

## 部署方案

- 前端：React/Vite 构建后由 Nginx 托管，宿主机端口 `18080`。
- 后端：Spring Boot 3 / Java 21，宿主机端口 `18081`，容器内端口 `8080`。
- 数据库：MySQL 8.4，宿主机端口 `33077`，使用 Docker volume 持久化。
- 浏览器访问 `http://服务器IP:18080`，前端 Nginx 将 `/api/*` 反向代理到后端容器。

这些端口避开了已占用的 `80`、`8080`。如仍冲突，可修改 `docker-compose.yml` 中 ports 左侧的宿主机端口。

## 敏感信息处理

1. 后端配置已移除默认明文数据库密码。
2. 数据库密码、MySQL root 密码、JWT 密钥通过服务器 `.env.server` 注入。
3. `.env.server`、数据库 dump、部署压缩包已加入忽略规则，不会进入镜像或 Git。
4. `plans/`、构建产物、依赖目录、日志等已通过 `.dockerignore` 排除。
5. 数据库 dump 可能包含业务数据，建议使用 `-EncryptPackage` 生成加密包。

说明：应用运行时必须能读取密码，因此不能把运行必需的密码做不可逆加密。安全做法是不要写入代码和镜像，而是在服务器运行时通过环境变量注入并限制文件权限。

## 本机生成部署包

仅构建镜像、导出本机数据库并生成部署包：

```powershell
.\build-and-push.ps1 -ExportDatabase
```

推送镜像到 Docker Hub 或其它 Registry：

```powershell
docker login
.\build-and-push.ps1 -Registry docker.io -ImagePrefix 你的DockerHub用户名/kbd-pms -ImageTag v1.0.0 -Push -ExportDatabase -EncryptPackage
```

常用参数：

| 参数 | 说明 |
| --- | --- |
| `-ExportDatabase` | 导出当前数据库到 `deploy/db/init/001-current-database.sql` |
| `-DbHost` / `-DbPort` | 本机数据库地址和端口，默认 `127.0.0.1:3306` |
| `-DbName` / `-DbUser` | 数据库名和用户，默认 `kbd_pm_system` / `root` |
| `-Registry` / `-ImagePrefix` / `-ImageTag` | 镜像仓库、镜像名前缀和标签 |
| `-Push` | 构建完成后推送镜像 |
| `-EncryptPackage` | 如安装 7-Zip，则生成 AES256 加密 zip |
| `-SkipImageArchive` | 不把镜像保存进部署包，仅适合服务器能从 Registry 拉取镜像的场景 |

默认情况下，脚本会把前后端镜像导出到部署包内：

```text
images/kbd-pms-images.tar
```

这样即使不使用 Docker Hub，也可以上传部署包后在服务器直接加载镜像。

## 上传并解压

```bash
mkdir -p /opt/kbd-pms
cd /opt/kbd-pms
unzip kbd-pms-deploy-package.zip
```

如果是加密包：

```bash
7z x kbd-pms-deploy-package-encrypted.zip
```

## 创建服务器环境变量

```bash
cd /opt/kbd-pms
cp .env.server.example .env.server
nano .env.server
chmod 600 .env.server
```

示例：

```env
COMPOSE_PROJECT_NAME=kbd-pms
DOCKER_IMAGE_PREFIX=你的镜像前缀/kbd-pms
IMAGE_TAG=v1.0.0
DB_NAME=kbd_pm_system
DB_USER=kbd_user
DB_PASSWORD=请填写强密码
MYSQL_ROOT_PASSWORD=请填写强root密码
JWT_SECRET=请填写至少32位随机字符串
JWT_EXPIRATION=86400000
```

## 启动服务

如果部署包内包含 `images/kbd-pms-images.tar`，先加载镜像：

```bash
docker load -i images/kbd-pms-images.tar
```

镜像已推送到 Registry 时：

```bash
docker compose --env-file .env.server pull
docker compose --env-file .env.server up -d
```

如果在服务器源码目录直接构建：

```bash
docker compose --env-file .env.server up -d --build
```

首次启动时 MySQL 会自动执行 `deploy/db/init/001-current-database.sql`。注意该目录只在数据库 volume 为空时执行一次。

## 更新邮件地址功能

当前版本的邮件通知会同时提供两个登录地址：

- 内网地址：`http://192.168.39.233:18080/`
- 外网地址：`http://343t787f48.wicp.vip/`

邮件配置页面也会显示并维护这两个地址。已有部署不会因为重新启动自动执行新的 SQL，因此升级时需要手动执行迁移脚本。

### 本地构建部署包

建议使用新的镜像标签，避免服务器继续使用旧的 `latest` 镜像：

```powershell
.\build-and-push.ps1 -ImagePrefix 你的镜像前缀/kbd-pms -ImageTag v20260731-mail-url
```

如果服务器从 Registry 拉取镜像：

```powershell
docker login
.\build-and-push.ps1 -Registry docker.io -ImagePrefix 你的DockerHub用户名/kbd-pms -ImageTag v20260731-mail-url -Push -SkipImageArchive
```

脚本会生成 `kbd-pms-deploy-package.zip`，并将数据库迁移脚本放入包内：

```text
db/20260731_mail_access_urls.sql
```

### 服务器升级

先备份数据库和上传附件，再解压新部署包：

```bash
cd /opt/kbd-pms
sudo /opt/kbd-pms/deploy/backup-kbd-pms.sh
unzip -o kbd-pms-deploy-package.zip
```

如果使用镜像归档包，加载新镜像：

```bash
docker load -i images/kbd-pms-images.tar
```

编辑现有 `.env.server`，确认镜像前缀和标签与本次构建一致：

```env
DOCKER_IMAGE_PREFIX=你的镜像前缀/kbd-pms
IMAGE_TAG=v20260731-mail-url
```

如果使用 Registry，拉取新镜像；如果使用归档包则跳过 `pull`：

```bash
docker compose --env-file .env.server pull
```

启动/更新前后端容器：

```bash
docker compose --env-file .env.server up -d
```

等待 MySQL 健康后，执行已有数据库迁移：

```bash
docker compose --env-file .env.server exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' < db/20260731_mail_access_urls.sql
```

验证两条地址已经写入数据库：

```bash
docker compose --env-file .env.server exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "SELECT config_key, config_value FROM system_config WHERE config_key IN ('"'"'app.internal.url'"'"','"'"'app.external.url'"'"');"'
```

最后检查容器和后端日志：

```bash
docker compose --env-file .env.server ps
docker compose --env-file .env.server logs --tail=100 backend
```

登录系统的“邮件配置”页面，确认内网地址和外网地址均已显示。随后触发一条提醒邮件，邮件底部应同时出现两条链接。

### 仅修改服务器地址配置，不更新代码

如果服务器已经运行包含双地址模板的镜像，只需执行上面的迁移脚本即可。不要执行 `docker compose down -v`，该命令会删除数据库和上传附件 volume。

## 验证

```bash
docker compose --env-file .env.server ps
docker compose --env-file .env.server logs -f backend
curl http://服务器IP:18080/health
```

浏览器访问：

```text
http://服务器IP:18080
```

## 常用运维命令

```bash
docker compose --env-file .env.server down
docker compose --env-file .env.server restart
docker compose --env-file .env.server pull
docker compose --env-file .env.server up -d
```

备份数据库：

```bash
docker compose --env-file .env.server exec mysql sh -c 'mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --single-transaction --routines --triggers --events "$MYSQL_DATABASE"' > backup.sql
```

如需强制重新导入初始 SQL，需要删除数据库 volume（会清空容器数据库，务必先备份）：

```bash
docker compose --env-file .env.server down -v
docker compose --env-file .env.server up -d
```

## 每周备份数据库和附件，仅保留 2 个副本

### 附件存储方式说明

系统上传附件不是直接以 BLOB 存入 MySQL。当前代码中：

- MySQL 的 `document.storage_path` 字段保存附件路径；
- 真实文件由后端写入文件系统；
- Docker 部署中附件目录挂载为 volume：`kbd-pms-uploads:/app/uploads`。

因此完整备份必须同时包含：

1. MySQL 数据库；
2. Docker volume `kbd-pms_kbd-pms-uploads` 中的附件文件。

### 安装备份脚本

把项目中的 `deploy/backup-kbd-pms.sh` 上传到服务器 `/opt/kbd-pms/deploy/backup-kbd-pms.sh` 后，在服务器执行：

```bash
cd /opt/kbd-pms
sudo chmod +x deploy/backup-kbd-pms.sh
```

手动执行一次测试：

```bash
sudo /opt/kbd-pms/deploy/backup-kbd-pms.sh
ls -lh /opt/kbd-pms-backups
```

备份文件形如：

```text
/opt/kbd-pms-backups/kbd-pms-20260731-120000.tar.gz
```

脚本会自动只保留最新 2 个备份包。

### 设置每周自动备份

每周日凌晨 3 点执行备份：

```bash
sudo crontab -e
```

添加一行：

```cron
0 3 * * 0 /opt/kbd-pms/deploy/backup-kbd-pms.sh >> /var/log/kbd-pms-backup.log 2>&1
```

查看定时任务：

```bash
sudo crontab -l
```

查看备份日志：

```bash
sudo tail -100 /var/log/kbd-pms-backup.log
```

### 恢复备份概览

假设备份文件为 `/opt/kbd-pms-backups/kbd-pms-20260731-120000.tar.gz`：

```bash
mkdir -p /tmp/kbd-restore
tar -xzf /opt/kbd-pms-backups/kbd-pms-20260731-120000.tar.gz -C /tmp/kbd-restore
```

恢复数据库：

```bash
cd /opt/kbd-pms
sudo docker compose --env-file .env.server exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD"' < /tmp/kbd-restore/kbd-pms-20260731-120000/database.sql
```

恢复附件 volume：

```bash
sudo docker run --rm \
  --entrypoint sh \
  --user 0:0 \
  -v kbd-pms_kbd-pms-uploads:/data \
  -v /tmp/kbd-restore/kbd-pms-20260731-120000:/backup \
  kbd-pms/backend:latest \
  -c 'cd /data && tar -xzf /backup/uploads.tar.gz'
```