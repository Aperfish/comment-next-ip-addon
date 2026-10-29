# 1.0.13-4 发布资料构建记录

日期：2026-10-29，Asia/Taipei。

从整理后的 repository 根目录执行 build.ps1，使用 JDK 25.0.5（目标 Java 21）、Node.js、PowerShell 和锁定校验值的构建依赖完成独立构建，所有脚本退出码为 0。

原版构建输入来自上游官方 1.0.13 Release：

https://github.com/acanyo/plugin-comment-next/releases/download/1.0.13/plugin-comment-next-1.0.13.jar

输入 SHA-256：53206d2eaeebe05e4be531ccbf8ec31235850fca81732cc990fc8140f0a32cc。

通过的检查：

- DistrictTest：31 项断言。
- AddonFormatTest：19 项断言。
- InjectionRegressionTest：构造器使用 PluginWrapper、管理器取得路径、前置缺失及失败后销毁检查。
- AdapterLinkTest：适配器在真实原版 1.0.13 服务类上的类型连接和构造检查。
- PackageAddon：新增/重定位类的基础字节码检查，原版服务和 mapper 不重新打包。
- 构建依赖按 tools/dependencies.lock.json 校验 SHA-256。

重要：注入与原服务连接检查仍使用编译签名夹具，不是真实 Halo/Spring 容器测试。尚未验证真实 Halo 安装启停、评论/回复请求、API/翻译服务调用和商店安装流程。

SLF4J 在独立连接检查中提示没有日志 provider，使用 NOP logger。这是测试夹具环境提示，不证明生产环境存在相同问题。

本资料包的 release-assets JAR 从此整理后的源码重新构建，补充了主页/Issue 元数据与许可文件，并非声称与此前聊天里的 JAR 位级一致。该制品的最终校验值见发布附件 SHA256SUMS.txt。
