# EasyConnect - Android NAS 文件管理器

<p align="center">
  <img src="app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml" width="120" alt="EasyConnect Logo">
</p>

<p align="center">
  <a href="https://github.com/yourusername/EasyConnect/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License"></a>
  <a href="https://github.com/yourusername/EasyConnect"><img src="https://img.shields.io/badge/platform-Android-green.svg" alt="Platform"></a>
  <a href="https://github.com/yourusername/EasyConnect"><img src="https://img.shields.io/badge/API-26%2B-brightgreen.svg" alt="API"></a>
  <a href="https://github.com/yourusername/EasyConnect"><img src="https://img.shields.io/badge/language-Java-orange.svg" alt="Language"></a>
</p>

<p align="center">
  一款简洁易用的安卓 NAS 文件管理器，支持 SMB/CIFS、FTP、SFTP 协议，轻松实现手机与 NAS 服务器之间的文件传输。
</p>

---

## ✨ 功能特性

### 🔌 多协议支持
- **SMB/CIFS** - Windows 共享协议，兼容群晖、威联通等主流 NAS
- **FTP/FTPS** - 传统文件传输协议，支持 TLS 加密
- **SFTP** - 基于 SSH 的安全文件传输

### 📁 文件管理
- **文件浏览** - 列表/网格双视图模式
- **面包屑导航** - 快速跳转任意目录层级
- **文件操作** - 新建文件夹、重命名、删除、移动
- **多选模式** - 批量操作文件
- **排序功能** - 按名称、大小、日期排序

### 📤 文件传输
- **后台传输** - 前台服务保证传输不中断
- **进度通知** - 实时显示传输进度和速度
- **传输队列** - 支持多文件并发传输（最多 3 个）
- **断点续传** - 支持 FTP 协议断点续传

### 🎬 多媒体预览
- **图片查看** - 支持 JPG/PNG/GIF/WebP 等格式
- **视频播放** - 支持 MP4/MKV/AVI 等格式，基于 ExoPlayer
- **音频播放** - 支持 MP3/FLAC/WAV/AAC 等格式

### 🔒 安全特性
- **密码加密** - 使用 Android Keystore 加密存储密码
- **安全传输** - 支持 FTPS/SFTP 加密连接

---

## 📱 截图

<p align="center">
  <img src="screenshots/connection_list.png" width="200" alt="连接列表">
  <img src="screenshots/add_connection.png" width="200" alt="添加连接">
  <img src="screenshots/file_browser.png" width="200" alt="文件浏览">
  <img src="screenshots/transfer_queue.png" width="200" alt="传输队列">
</p>

---

## 🛠️ 技术栈

| 类别 | 技术 | 说明 |
|------|------|------|
| **架构** | MVVM | Model-View-ViewModel |
| **语言** | Java | Android 官方语言 |
| **最低版本** | Android 8.0 (API 26) | 覆盖 95%+ 设备 |
| **SMB 协议** | [smbj](https://github.com/hierynomus/smbj) | 纯 Java 实现的 SMB 2/3 |
| **FTP 协议** | [Apache Commons Net](https://commons.apache.org/proper/commons-net/) | 成熟稳定的 FTP 客户端 |
| **SFTP 协议** | [sshj](https://github.com/hierynomus/sshj) | 现代化的 SSH/SFTP 库 |
| **数据库** | [Room](https://developer.android.com/training/data-storage/room) | Android 官方 ORM |
| **UI 组件** | [Material Design 3](https://m3.material.io/) | Google 最新设计规范 |
| **媒体播放** | [Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer) | 官方媒体播放器 |
| **图片加载** | [Glide](https://bumptech.github.io/glide/) | 高效图片加载库 |
| **导航** | [Navigation Component](https://developer.android.com/guide/navigation) | 单 Activity 架构 |

---

## 📦 项目结构

```
app/src/main/java/com/easyconnect/nas/
├── EasyConnectApp.java           # Application 入口
├── data/                          # 数据层
│   ├── model/                     # 数据模型
│   │   ├── NasFile.java          # NAS 文件模型
│   │   ├── ConnectionInfo.java   # 连接信息
│   │   ├── TransferTask.java     # 传输任务
│   │   └── ProtocolType.java     # 协议类型枚举
│   ├── db/                        # Room 数据库
│   │   ├── entity/               # 数据库实体
│   │   ├── dao/                  # 数据访问对象
│   │   └── converter/            # 类型转换器
│   └── repository/                # 数据仓库
│       ├── ConnectionRepository.java
│       ├── TransferRepository.java
│       └── LocalFileRepository.java
├── protocol/                      # 协议层
│   ├── ProtocolManager.java      # 协议接口
│   ├── ProtocolFactory.java      # 协议工厂
│   ├── ftp/                      # FTP 实现
│   ├── sftp/                     # SFTP 实现
│   └── smb/                      # SMB 实现
├── transfer/                      # 传输层
│   ├── TransferService.java      # 前台服务
│   ├── TransferManager.java      # 传输管理器
│   ├── TransferWorker.java       # 传输工作线程
│   └── NasDataSource.java        # Media3 数据源
├── ui/                            # 界面层
│   ├── main/                     # 主界面
│   ├── connection/               # 连接管理
│   ├── browser/                  # 文件浏览器
│   ├── transfer/                 # 传输队列
│   ├── preview/                  # 媒体预览
│   └── settings/                 # 设置
└── util/                          # 工具类
    ├── CryptoUtils.java          # 加密工具
    ├── FileIconHelper.java       # 文件图标
    ├── MimeTypeHelper.java       # MIME 类型
    └── PathUtils.java            # 路径工具
```

---

## 🚀 快速开始

### 环境要求

- **Android Studio** - Hedgehog (2023.1.1) 或更高版本
- **JDK** - 11 或更高版本
- **Android SDK** - API 26 或更高版本
- **Gradle** - 8.5 或更高版本

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/yourusername/EasyConnect.git
   cd EasyConnect
   ```

2. **使用 Android Studio 打开项目**
   - 启动 Android Studio
   - 选择 `File > Open`
   - 选择项目根目录

3. **同步 Gradle**
   - 等待 Gradle 同步完成
   - 如有问题，点击 `File > Sync Project with Gradle Files`

4. **运行应用**
   - 连接 Android 设备或启动模拟器
   - 点击 `Run > Run 'app'` 或按 `Shift+F10`

### 构建 APK

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本
./gradlew assembleRelease
```

生成的 APK 位于 `app/build/outputs/apk/` 目录。

---

## 📖 使用指南

### 添加 NAS 连接

1. 打开应用，点击右下角 **+** 按钮
2. 选择协议类型（SMB/FTP/SFTP）
3. 填写服务器信息：
   - **连接名称** - 自定义名称，如"家庭 NAS"
   - **主机地址** - NAS 的 IP 地址或域名
   - **端口** - 默认端口会自动填充
   - **用户名/密码** - NAS 登录凭证
   - **共享名称** - SMB 协议需要填写共享文件夹名
4. 点击 **测试连接** 验证配置
5. 点击 **保存** 完成添加

### 浏览文件

1. 在连接列表中点击要连接的服务器
2. 进入文件浏览器，可以：
   - **点击文件夹** - 进入目录
   - **点击面包屑** - 快速跳转
   - **长按文件** - 进入多选模式
   - **下拉刷新** - 刷新文件列表

### 传输文件

1. 在文件浏览器中选择文件
2. 点击 **下载** 按钮开始传输
3. 在 **传输** 标签页查看进度
4. 传输完成后可在通知栏点击打开文件

### 预览媒体

- **图片** - 直接点击图片文件即可预览
- **视频** - 点击视频文件进入播放器
- **音频** - 点击音频文件进入播放器

---

## ⚙️ 配置说明

### 网络安全配置

应用默认允许明文 HTTP 连接（FTP 协议需要）。如需修改，编辑 `app/src/main/res/xml/network_security_config.xml`：

```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

### ProGuard 规则

Release 版本会自动混淆。关键库的 keep 规则已配置在 `app/proguard-rules.pro`：

```proguard
# smbj
-keep class com.hierynomus.** { *; }

# sshj
-keep class net.schmizz.** { *; }

# Commons Net
-keep class org.apache.commons.net.** { *; }

# BouncyCastle
-keep class org.bouncycastle.** { *; }
```

---

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. **Fork 项目**
2. **创建特性分支** (`git checkout -b feature/AmazingFeature`)
3. **提交更改** (`git commit -m 'Add some AmazingFeature'`)
4. **推送到分支** (`git push origin feature/AmazingFeature`)
5. **创建 Pull Request**

### 代码规范

- 遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 使用有意义的变量和方法名
- 添加必要的注释
- 保持方法简洁，单一职责

### 报告问题

请使用 [GitHub Issues](https://github.com/yourusername/EasyConnect/issues) 报告问题，包含：

- 设备型号和 Android 版本
- 应用版本
- 复现步骤
- 错误日志（如有）

---

## 📝 更新日志

### v1.0.0 (2024-XX-XX)

- 🎉 首次发布
- ✨ 支持 SMB/CIFS、FTP、SFTP 协议
- ✨ 文件浏览器（列表/网格视图）
- ✨ 文件操作（创建/删除/重命名/移动）
- ✨ 后台文件传输
- ✨ 多媒体预览（图片/视频/音频）
- ✨ 连接管理（添加/编辑/删除）
- ✨ Material Design 3 界面

---

## 📄 许可证

本项目采用 [MIT 许可证](LICENSE) - 详见 [LICENSE](LICENSE) 文件

```
MIT License

Copyright (c) 2024 Your Name

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🙏 致谢

感谢以下开源项目：

- [smbj](https://github.com/hierynomus/smbj) - SMB 2/3 协议实现
- [sshj](https://github.com/hierynomus/sshj) - SSH/SFTP 库
- [Apache Commons Net](https://commons.apache.org/proper/commons-net/) - FTP 客户端
- [ExoPlayer](https://github.com/google/ExoPlayer) - 媒体播放器
- [Glide](https://github.com/bumptech/glide) - 图片加载库
- [Material Design 3](https://m3.material.io/) - 设计规范

---

## 📧 联系方式

- **GitHub** - [yourusername](https://github.com/yourusername)
- **Email** - your.email@example.com
- **Issues** - [GitHub Issues](https://github.com/yourusername/EasyConnect/issues)

---

<p align="center">
  如果觉得这个项目有帮助，请给个 ⭐️ Star 支持一下！
</p>
