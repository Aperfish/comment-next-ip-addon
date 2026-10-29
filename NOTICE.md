# 来源、许可与构建说明

维护者：Aperfish。上游评论组件 Next 作者：Handsome；项目 https://github.com/acanyo/plugin-comment-next 。本扩展非上游官方发布。

原项目及复用的定位客户端、URI 安全策略和 JSON 工具遵循 GPL-3.0。对应参考源码在 vendor/upstream-source/，固定至上游 1.0.13 标签对应提交 82d5357c0f81d82833e20d8bf1129b2468571528。当前构建从用户提供的原版 JAR 读取这三个已编译类，并在本扩展中隔离命名；不打包原版服务和 mapper。构建仍需要原版 1.0.13 JAR，不包含它不代表可省略该前置。

扩展自行编写的解析、显示、缓存、翻译、连接代码在 src/main/java/。编译用 Halo/Spring/Reactor/PF4J 接口签名在 compile-fixtures/，它们不是运行实现，绝不能打入 JAR。

最终包还包含以下来自原版 JAR 的组件与数据：

- JSON-java 20240303：其 POM 声明 Public Domain，保留上游原始 LICENSE，见 licenses/JSON-java-LICENSE.txt；https://github.com/stleary/JSON-java 。
- ip2region 3.3.7 及 IPv4/IPv6 XDB：原版注明 Apache-2.0，参见 licenses/Apache-2.0.txt、licenses/IP2REGION-NOTICE.md；https://github.com/lionsoul2014/ip2region 。

ASM、Guava、Jackson、SLF4J 作为构建/检查依赖使用，不复制入最终 JAR。其固定下载地址和 SHA-256 在 tools/dependencies.lock.json。部署时 Halo 提供相应运行依赖。

图标使用内置 AI 生图工具生成，提示词见 docs/ICON.md，未使用原插件图标、第三方商标或地图轮廓。AI 生成不代表商标唯一性保证。

README 中的地域使用警告是维护者提醒，不构成 GPL-3.0 授权的附加地域限制。
