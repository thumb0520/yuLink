# 更新日志

本项目的所有显著更改都将记录在此文件。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
并且本项目遵循 [语义化版本控制](https://semver.org/lang/zh-CN/)。

## [未发布]

### 计划中
- WebDAV 协议支持
- 自动备份照片功能
- 文件搜索功能
- 多语言支持（英文/中文）
- 深色模式优化
- 文件分享功能
- 批量重命名

## [1.0.0] - 2024-XX-XX

### 新增
- 🎉 首次发布
- ✨ **多协议支持**
  - SMB/CIFS 协议（支持 SMB 2.0/3.0）
  - FTP/FTPS 协议
  - SFTP 协议
- ✨ **连接管理**
  - 添加/编辑/删除 NAS 连接
  - 连接测试功能
  - 密码加密存储
  - 连接历史记录
- ✨ **文件浏览器**
  - 列表/网格双视图模式
  - 面包屑导航
  - 文件排序（名称/大小/日期）
  - 下拉刷新
  - 多选模式
- ✨ **文件操作**
  - 新建文件夹
  - 重命名文件/文件夹
  - 删除文件/文件夹
  - 移动文件
  - 查看文件属性
- ✨ **文件传输**
  - 后台传输服务
  - 传输进度通知
  - 传输队列管理
  - 最多 3 个并发传输
  - 传输历史记录
- ✨ **多媒体预览**
  - 图片预览（JPG/PNG/GIF/WebP/BMP）
  - 视频播放（MP4/MKV/AVI/MOV）
  - 音频播放（MP3/FLAC/WAV/AAC）
  - 基于 ExoPlayer 的流媒体播放
- ✨ **用户界面**
  - Material Design 3 设计规范
  - 浅色/深色主题
  - 中文界面
  - 响应式布局
- ✨ **安全特性**
  - Android Keystore 密码加密
  - FTPS/SFTP 加密传输
  - 网络安全配置

### 技术细节
- 基于 MVVM 架构
- 使用 Room 数据库
- 使用 Navigation Component
- 使用 Lifecycle 组件
- 支持 Android 8.0+ (API 26+)

---

## 版本说明

### 版本号格式

本项目使用语义化版本号：`主版本号.次版本号.修订号`

- **主版本号**：不兼容的 API 修改
- **次版本号**：向下兼容的功能性新增
- **修订号**：向下兼容的问题修正

### 更新类型

- **新增** - 新功能
- **变更** - 现有功能的变更
- **弃用** - 即将移除的功能
- **移除** - 已移除的功能
- **修复** - Bug 修复
- **安全** - 安全相关的更改

---

## 链接

- [GitHub Releases](https://github.com/yourusername/NasLink/releases)
- [下载页面](https://github.com/yourusername/NasLink/releases/latest)
