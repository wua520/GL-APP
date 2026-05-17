# Fitness Server 后端服务

## 环境变量配置

### Windows服务器设置环境变量

在PowerShell中执行（每次重启后需要重新设置，或添加到系统环境变量）：

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="15730046932"
$env:JWT_SECRET="fitness-app-secret-key-2024-very-long-secret-key-for-jwt-token"
```

### 永久设置（推荐）

1. 右键"此电脑" → "属性" → "高级系统设置" → "环境变量"
2. 在"系统变量"中点击"新建"，添加：
   - 变量名：`DB_USERNAME`，值：`root`
   - 变量名：`DB_PASSWORD`，值：`15730046932`
   - 变量名：`JWT_SECRET`，值：`fitness-app-secret-key-2024-very-long-secret-key-for-jwt-token`

### Linux服务器设置

编辑 `~/.bashrc` 或 `~/.bash_profile`：

```bash
export DB_USERNAME=root
export DB_PASSWORD=15730046932
export JWT_SECRET=fitness-app-secret-key-2024-very-long-secret-key-for-jwt-token
```

然后执行：`source ~/.bashrc`

## 启动服务

```bash
java -jar fitness-server-1.0.0.jar
```

## 本地开发

复制 `.env.example` 为 `.env`，填入真实的配置值。
