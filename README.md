# YuLink - Android NAS 文件管理器

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="120" alt="YuLink Logo">
</p>

<p align="center">
  <a href="#"><img src="https://img.shields.io/badge/version-1.0.0-blue.svg" alt="Version"></a>
  <a href="#"><img src="https://img.shields.io/badge/platform-Android-green.svg" alt="Platform"></a>
  <a href="#"><img src="https://img.shields.io/badge/API-26%2B-brightgreen.svg" alt="API"></a>
  <a href="#"><img src="https://img.shields.io/badge/language-Java-orange.svg" alt="Language"></a>
</p>

<p align="center">
  一款简洁的安卓 NAS 文件管理器，支持 SMB/CIFS、FTP、SFTP 协议，实现手机与 NAS 之间的文件浏览、传输和媒体预览。
</p>

---

## 功能特性

### 多协议支持
- **SMB/CIFS** (端口 445) — 兼容群晖、威联通等主流 NAS
- **FTP** (端口 21) — 传统文件传输协议
- **SFTP** (端口 22) — 基于 SSH 的安全传输

### 文件管理
- 列表 / 网格双视图切换
- 面包屑导航，快速跳转目录层级
- 新建文件夹、重命名、删除、移动
- 多选模式批量操作
- 按名称、大小、日期排序

### 文件传输
- 前台服务后台传输，通知栏实时进度
- 支持最多 3 个文件并发传输
- 传输完成 / 失败通知自动替换进度通知
- 支持取消正在传输的任务
- 支持强制删除任何状态的任务（包括传输中）

### 媒体预览
- **图片** — 支持 JPG / PNG / GIF / WebP / BMP 等格式
- **视频** — 支持 MP4 / MKV / AVI 等格式，支持全屏播放
- **音频** — 支持 MP3 / FLAC / WAV / AAC 等格式

### 安全
- 密码使用 Android Keystore 加密存储
- 支持 SFTP 加密连接、SMB 加密传输

---

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 架构 | MVVM | ViewModel + LiveData |
| 语言 | Java | JDK 11 |
| 最低版本 | Android 8.0 | API 26 |
| 目标版本 | Android 14 | API 34 |
| UI | Material Design 3 | 1.11.0 |
| 导航 | Navigation Component | 2.7.7 |
| 数据库 | Room | 2.6.1 |
| SMB | smbj | 0.13.0 |
| FTP | Apache Commons Net | 3.11.1 |
| SFTP | sshj | 0.10.0 |
| 图片加载 | Glide | 4.16.0 |
| 媒体播放 | Media3 ExoPlayer | 1.2.1 |
| 加密 | AndroidX Security Crypto | 1.1.0-alpha06 |

---

## 项目结构

```
app/src/main/java/com/yulink/nas/
├── YuLinkApp.java                      # Application 入口
│
├── data/
│   ├── model/                          # 数据模型
│   │   ├── ConnectionInfo.java         # 连接信息
│   │   ├── NasFile.java                # NAS 文件模型
│   │   ├── ProtocolType.java           # 协议类型 (SMB/FTP/SFTP)
│   │   └── TransferTask.java           # 传输任务
│   ├── db/                             # Room 数据库
│   │   ├── AppDatabase.java
│   │   ├── converter/                  # 类型转换器
│   │   ├── dao/                        # ConnectionDao, TransferHistoryDao
│   │   └── entity/                     # ConnectionEntity, TransferHistoryEntity
│   └── repository/
│       ├── ConnectionRepository.java
│       ├── LocalFileRepository.java
│       └── TransferRepository.java
│
├── protocol/                           # 协议抽象层
│   ├── ProtocolManager.java            # 统一接口
│   ├── ProtocolFactory.java            # 工厂类
│   ├── ProtocolException.java
│   ├── smb/SmbProtocolManager.java     # SMB 实现（含随机访问流优化）
│   ├── ftp/FtpProtocolManager.java
│   └── sftp/SftpProtocolManager.java
│
├── transfer/                           # 传输层
│   ├── TransferService.java            # 前台服务
│   ├── TransferManager.java            # 线程池管理
│   ├── TransferWorker.java             # 传输工作线程
│   ├── TransferNotificationHelper.java # 通知管理
│   └── NasDataSource.java              # Media3 数据源（支持 SMB 随机访问）
│
├── ui/
│   ├── main/MainActivity.java          # 主界面 + 底部导航
│   ├── connection/                     # 连接管理
│   │   ├── ConnectionListFragment      # 连接列表
│   │   ├── AddConnectionFragment       # 添加/编辑连接
│   │   └── adapter/ConnectionAdapter
│   ├── browser/                        # 文件浏览器
│   │   ├── FileBrowserFragment         # 文件列表 + 面包屑
│   │   ├── FileBrowserViewModel
│   │   ├── adapter/FileListAdapter
│   │   └── dialog/                     # CreateFolderDialog, RenameDialog
│   ├── transfer/                       # 传输队列
│   │   ├── TransferQueueFragment
│   │   └── adapter/TransferAdapter
│   ├── preview/                        # 媒体预览
│   │   ├── ImagePreviewActivity        # 图片查看（先下载再用 Glide 加载）
│   │   ├── VideoPlayerActivity         # 视频播放（ExoPlayer + 全屏）
│   │   └── AudioPlayerActivity         # 音频播放
│   └── settings/SettingsFragment       # 设置页
│
└── util/
    ├── CryptoUtils.java                # AES 加密/解密
    ├── FileIconHelper.java             # 文件图标映射
    ├── MimeTypeHelper.java             # MIME 类型映射
    └── PathUtils.java                  # 路径工具
```

---

## 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高
- JDK 11+
- Android SDK API 26+

### 构建运行

```bash
git clone https://github.com/yourusername/YuLink.git
cd YuLink
```

用 Android Studio 打开项目，等待 Gradle 同步完成，连接设备后运行。

```bash
# Debug APK
./gradlew assembleDebug

# Release APK（需配置签名）
./gradlew assembleRelease
```

---

## 使用说明

### 添加连接
1. 点击底部 **连接** 标签，点击 **+** 按钮
2. 选择协议，填写主机、端口、用户名、密码
3. SMB 协议需填写共享文件夹名称
4. 点击 **测试连接** 验证后保存

### 浏览文件
- 点击连接进入文件浏览器
- 点击文件夹进入，点击面包屑跳转
- 长按文件进入多选模式
- 下拉刷新文件列表

### 传输文件
- 点击文件选择 **下载**，自动开始后台传输
- 底部 **传输** 标签查看进度
- 通知栏显示实时进度，完成后自动替换为完成通知
- 点击 **取消** 按钮可取消正在传输的任务
- 点击 **删除** 按钮可删除任务记录（传输中的任务会强制取消并删除）

### 预览媒体
- 图片 / 视频 / 音频文件直接点击即可预览
- 视频播放器支持全屏（右上角按钮）和横屏

---

## 更新日志

### v1.0.1 (2026-06-04)

- 修复取消任务后状态显示错误的问题
- 新增强制删除功能，支持删除任何状态的任务（包括传输中）
- 传输队列显示历史下载任务
- 传输完成后不再展示进度条
- 修复 Tab 切换失效的问题

### v1.0.0 (2026-06-04)

- SMB / FTP / SFTP 多协议支持
- 文件浏览器（列表/网格视图、面包屑导航、多选）
- 文件操作（新建/删除/重命名/移动）
- 后台文件传输 + 通知栏进度
- 图片 / 视频 / 音频预览
- 视频全屏播放
- SMB 大文件随机访问优化（解决 6GB+ 视频 seek 卡顿）
- 连接管理（添加/编辑/删除）
- Material Design 3 界面 + 深色模式
- Android Keystore 密码加密

---

<p align="center">
  如果觉得有帮助，请给个 ⭐ Star 支持一下
</p>
